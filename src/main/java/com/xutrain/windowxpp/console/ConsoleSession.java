package com.xutrain.windowxpp.console;

import com.xutrain.windowx.client.multiwindow.CustomWindow;
import com.xutrain.windowxpp.network.ConsolePayloads;
import com.xutrain.windowxpp.network.UiPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 一个控制台窗口及其终端状态
public final class ConsoleSession {
    private static final int MIN_FONT_SIZE = 6;
    // 2000 号字已经比 4K 屏还高，只是防住手滑输入的天文数字
    private static final int MAX_FONT_SIZE = 2000;
    private static final int MAX_INPUT_LENGTH = 256;
    private static final int MAX_UI_INPUT_LENGTH = 256;
    // 程序设过尺寸后等这么多帧再允许按实际尺寸重排，避开系统的延迟调整
    private static final int SIZE_SETTLE_FRAMES = 5;

    private final UUID owner;
    private final String id;
    private final CustomWindow window;
    private final long handle;
    private final String flags;
    private TerminalRenderer renderer;
    private TerminalBuffer buffer;
    private final AnsiParser parser = new AnsiParser();
    private TerminalPalette palette = TerminalPalette.defaultPalette();

    private final StringBuilder inputLine = new StringBuilder();
    private boolean inputPending;
    private boolean inputLive;
    private boolean masked;
    private boolean lengthWarned;
    private boolean eventsEnabled;
    private String inputObjective = "";
    private int fontSize;
    private String fontFamily = "";
    // -1 跟随字号自动，0 强制像素渲染，1 强制抗锯齿
    private int smoothMode = -1;
    private float opacity = 1.0F;
    private int background = AnsiColors.DEFAULT_BACKGROUND;
    private int layoutWidth = -1;
    private int layoutHeight = -1;
    private int selectionStartX = -1;
    private int selectionStartY = -1;
    private int selectionEndX = -1;
    private int selectionEndY = -1;
    private boolean selecting;
    private boolean hasSelection;
    // 程序刚设过窗口尺寸，随后几帧只同步期望值，不据此重排字符
    private int pendingSizeFrames;
    private boolean fontLimitWarned;
    private boolean dirty = true;
    private int textureWidth;
    private int textureHeight;
    private int renderedWindowWidth = -1;
    private int renderedWindowHeight = -1;
    private final ByteBuffer[] pixelBuffers = new ByteBuffer[2];
    private int pixelBufferIndex;
    // UI 元素按加入顺序叠放，后加的盖住先加的
    private final Map<String, UiElement> uiElements = new LinkedHashMap<>();
    private String pressedUiId = "";
    private String focusedUiId = "";
    private final StringBuilder uiInput = new StringBuilder();

    public ConsoleSession(UUID owner, String id, CustomWindow window, int fontSize, String flags, int background) {
        this.owner = owner;
        this.id = id;
        this.window = window;
        this.handle = window.getHandle();
        this.flags = flags == null ? "" : flags;
        this.fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
        this.background = background;
        this.renderer = new TerminalRenderer(this.fontSize, "", smoothMode);
        this.buffer = new TerminalBuffer(colsFor(window), rowsFor(window));
        this.layoutWidth = window.getWidth();
        this.layoutHeight = window.getHeight();
        window.setOverlayOpacity(1.0F);
        window.setContentLayer("T");
        parser.setPalette(palette);
        // 窗口底色跟随终端底色，避免纹理边缘透出窗口原色产生反光感
        applyWindowBackground(1.0F);
    }

    private void applyWindowBackground(float alpha) {
        float red = ((background >> 16) & 0xFF) / 255.0F;
        float green = ((background >> 8) & 0xFF) / 255.0F;
        float blue = (background & 0xFF) / 255.0F;
        window.setBackgroundColor(red, green, blue, alpha);
    }

    public UUID owner() {
        return owner;
    }

    public String id() {
        return id;
    }

    public CustomWindow window() {
        return window;
    }

    // 窗口销毁后 handle 会失效，会话固定保存创建时的值
    public long handle() {
        return handle;
    }

    public String flags() {
        return flags;
    }

    public int fontSize() {
        return fontSize;
    }

    public TerminalBuffer buffer() {
        return buffer;
    }

    public TerminalRenderer renderer() {
        return renderer;
    }

    public synchronized void print(String text) {
        parser.feed(text, buffer);
        dirty = true;
    }

    public synchronized void clear() {
        clear(2);
    }

    // mode 0 光标到末尾，1 开头到光标，2 整屏
    public synchronized void clear(int mode) {
        parser.reset();
        buffer.eraseDisplay(mode);
        // 整屏清除等于 cls，要回到左上角，否则提示符会落在清屏前的行上
        if (mode == 2) {
            buffer.setCursor(0, 0);
            buffer.resetScrollRegion();
        }
        dirty = true;
    }

    // 正数向上滚，负数向下滚
    public synchronized void scroll(int lines) {
        if (lines > 0) {
            buffer.scrollRegionUp(lines);
        } else if (lines < 0) {
            buffer.scrollRegionDown(-lines);
        }
        dirty = true;
    }

    // top 和 bottom 传 0 表示恢复整屏滚动
    public synchronized void setRegion(int top, int bottom) {
        if (top <= 0 || bottom <= 0) {
            buffer.resetScrollRegion();
        } else {
            buffer.setScrollRegion(top - 1, bottom - 1);
        }
        dirty = true;
    }

    // 坐标从 1 开始，和终端习惯一致
    public synchronized void printAt(int x, int y, String text) {
        buffer.setCursor(Math.max(1, x) - 1, Math.max(1, y) - 1);
        print(text);
    }

    // 尺寸按请求的像素算行列，允许窗口超出屏幕，和 conhost 拉伸窗口的行为一致
    public synchronized void resizeWindow(int height, int width) {
        int targetWidth = Math.max(1, width);
        int targetHeight = Math.max(1, height);
        resizeWindowUnrestricted(targetWidth, targetHeight);
        int targetCols = Math.max(1, targetWidth / renderer.cellWidth());
        int targetRows = Math.max(1, targetHeight / renderer.cellHeight());
        if (targetCols != buffer.cols() || targetRows != buffer.rows()) {
            buffer.resize(targetCols, targetRows);
        }
        layoutWidth = targetWidth;
        layoutHeight = targetHeight;
        pendingSizeFrames = SIZE_SETTLE_FRAMES;
        dirty = true;
    }

    // nogrow / noshrink 的尺寸上限同样会挡住程序自己改尺寸，改之前先临时放开，改完再按新尺寸重设
    private void resizeWindowUnrestricted(int width, int height) {
        boolean limited = flags.contains("noshrink") || flags.contains("nogrow");
        if (limited) {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
            GLFW.glfwSetWindowSizeLimits(
                    handle,
                    GLFW.GLFW_DONT_CARE,
                    GLFW.GLFW_DONT_CARE,
                    GLFW.GLFW_DONT_CARE,
                    GLFW.GLFW_DONT_CARE
            );
        }
        window.setSize(width, height);
        if (limited) {
            ConsoleManager.getInstance().applySizeFlags(window, flags);
        }
    }

    public synchronized void move(int x, int y) {
        window.setPosition(x, y);
    }

    public synchronized boolean inputPending() {
        return inputPending;
    }

    public synchronized void beginInput(String objective, boolean live) {
        inputPending = true;
        inputLive = live;
        inputObjective = objective == null ? "" : objective;
        inputLine.setLength(0);
        lengthWarned = false;
        dirty = true;
    }

    public synchronized void cancelInput() {
        cancelInput("");
    }

    // 服务端取消时可带一句提示，玩家自己按 ESC 则不打印
    public synchronized void cancelInput(String message) {
        if (!inputPending) {
            return;
        }
        inputPending = false;
        inputLive = false;
        inputObjective = "";
        inputLine.setLength(0);
        lengthWarned = false;
        buffer.carriageReturn();
        buffer.newline();
        dirty = true;
        sendAbort();
        if (message != null && !message.isEmpty()) {
            print("\u001b[33m" + message + "\u001b[0m\n");
        }
    }

    public synchronized void typeCharacter(char character) {
        if (!inputPending) {
            return;
        }
        if (inputLine.length() >= MAX_INPUT_LENGTH) {
            if (!lengthWarned) {
                print("\u001b[33m已达 " + MAX_INPUT_LENGTH + " 字符上限\u001b[0m\n");
                lengthWarned = true;
            }
            return;
        }
        inputLine.append(character);
        // 遮蔽模式下只回显星号，真实内容留在缓冲里
        buffer.write(masked ? '*' : character, CellStyle.DEFAULT);
        dirty = true;
        publishLive();
    }

    public synchronized void eraseCharacter() {
        if (!inputPending || inputLine.length() == 0) {
            return;
        }
        inputLine.deleteCharAt(inputLine.length() - 1);
        buffer.backspace();
        dirty = true;
        publishLive();
    }

    // 键盘和鼠标回调跑在窗口自己的渲染线程上，发包必须切回游戏主线程
    private void sendToServer(CustomPacketPayload payload) {
        Minecraft.getInstance().execute(() -> {
            try {
                PacketDistributor.sendToServer(payload);
            } catch (RuntimeException e) {
                com.xutrain.windowxpp.WindowXPP.LOGGER.warn(
                        "[WindowsX++] 发送失败: {}", e.getMessage()
                );
            }
        });
    }

    // 动态模式下每次缓冲变化就同步，解析不出整数时保持计分板原值
    private void publishLive() {
        if (!inputLive || inputObjective.isEmpty()) {
            return;
        }
        try {
            int value = Integer.parseInt(inputLine.toString().trim());
            sendToServer(new ConsolePayloads.InputResult(id, String.valueOf(value), inputObjective));
        } catch (NumberFormatException ignored) {
        }
    }

    // 回车提交，空输入视为取消，非整数仍然提交但只落到 storage
    public synchronized boolean commitInput() {
        if (!inputPending) {
            return false;
        }
        String value = inputLine.toString().trim();
        String objective = inputObjective;
        inputPending = false;
        inputLive = false;
        inputObjective = "";
        inputLine.setLength(0);
        lengthWarned = false;
        // 回车换行需要回到行首，否则下一行会阶梯式右移
        buffer.carriageReturn();
        buffer.newline();
        dirty = true;
        if (value.isEmpty()) {
            print("\u001b[33m输入为空，已取消\u001b[0m\n");
            sendAbort();
            return false;
        }
        // 纯文本输入（objective 为空）不做整数校验，避免干扰命令行界面
        if (!objective.isEmpty() && !isInteger(value)) {
            print("\u001b[33m非整数，只写入 storage\u001b[0m\n");
        }
        sendResult(value, objective);
        return true;
    }

    private void sendResult(String text, String objective) {
        sendToServer(new ConsolePayloads.InputResult(id, text, objective));
    }

    // 取消也要回传，否则服务端无法区分「没回答」和「还在等」
    private void sendAbort() {
        sendToServer(new ConsolePayloads.InputAborted(id));
    }

    // 事件默认关闭，数据包显式开启后才回传，避免随便按键都发包
    public synchronized void setEventsEnabled(boolean value) {
        eventsEnabled = value;
    }

    public synchronized boolean eventsEnabled() {
        return eventsEnabled;
    }

    public synchronized void publishKey(int key, int mods) {
        if (!eventsEnabled) {
            return;
        }
        sendToServer(new ConsolePayloads.KeyEvent(id, key, mods));
    }

    // 内部坐标是 0 基，回传时换成终端习惯的 1 基
    public synchronized void publishClick(int column, int row, int button) {
        if (!eventsEnabled) {
            return;
        }
        sendToServer(new ConsolePayloads.ClickEvent(id, column + 1, row + 1, button));
    }

    private static boolean isInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 服务端填充输入缓冲，submit 为 true 时等价于玩家敲完回车
    public synchronized void inject(String text, boolean submit) {
        if (!inputPending || text == null) {
            return;
        }
        for (int i = 0; i < text.length() && inputLine.length() < MAX_INPUT_LENGTH; i++) {
            char character = text.charAt(i);
            if (character < 0x20 || character == 0x7F) {
                continue;
            }
            typeCharacter(character);
        }
        if (submit) {
            commitInput();
        }
    }

    public synchronized void setMasked(boolean value) {
        masked = value;
    }

    public synchronized void setTitle(String title) {
        window.setTitle(title);
    }

    public synchronized int background() {
        return background;
    }

    public synchronized void setBackground(int color) {
        this.background = color;
        dirty = true;
    }

    public synchronized TerminalPalette palette() {
        return palette;
    }

    // 切配色方案时背景一并跟随，否则新前景色会和旧底色撞在一起
    public synchronized void applyPalette(TerminalPalette target) {
        palette = target;
        parser.setPalette(target);
        background = target.background();
        applyWindowBackground(opacity);
        dirty = true;
    }

    public synchronized void setLayer(String layer) {
        window.setLayer(layer);
    }

    // 关闭被拦下时给个回执，否则玩家会以为点击没生效
    public synchronized void notifyCloseBlocked() {
        print("\u001b[33m该控制台标记了 noclose，无法关闭\u001b[0m\n");
    }

    // list 用的单行摘要
    public synchronized String summary() {
        return id
                + " | " + buffer.cols() + "x" + buffer.rows()
                + " | font " + fontSize
                + " | " + palette.name()
                + " | bg #" + String.format("%06X", background)
                + " | " + (flags.isEmpty() ? "-" : flags)
                + " | " + window.getTitle();
    }

    // get 用的完整属性
    public synchronized String describe() {
        StringBuilder text = new StringBuilder();
        text.append("id: ").append(id).append('\n');
        text.append("title: ").append(window.getTitle()).append('\n');
        text.append("grid: ").append(buffer.cols()).append('x').append(buffer.rows()).append('\n');
        text.append("pixels: ").append(window.getWidth()).append('x').append(window.getHeight()).append('\n');
        text.append("font: ").append(fontSize);
        if (!fontFamily.isEmpty()) {
            text.append(' ').append(fontFamily);
        }
        text.append('\n');
        text.append("palette: ").append(palette.name()).append('\n');
        text.append("background: #").append(String.format("%06X", background)).append('\n');
        text.append("opacity: ").append(String.format("%.2f", opacity)).append('\n');
        text.append("cursor: ").append(buffer.cursorVisible()).append('\n');
        text.append("flags: ").append(flags.isEmpty() ? "-" : flags).append('\n');
        text.append("input: ").append(inputPending ? (masked ? "masked" : "waiting") : "-").append('\n');
        text.append("selection: ").append(hasSelection).append('\n');
        text.append("scroll region: ").append(buffer.scrollRegion());
        return text.toString();
    }

    // dump 用的整屏文本，行尾空白去掉
    public synchronized String dump() {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < buffer.rows(); y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < buffer.cols(); x++) {
                char character = buffer.cellAt(x, y).character();
                if (TerminalBuffer.isContinuation(character)) {
                    continue;
                }
                line.append(character);
            }
            text.append(line.toString().stripTrailing());
            if (y < buffer.rows() - 1) {
                text.append('\n');
            }
        }
        return text.toString().stripTrailing();
    }

    // 定义或更新一个 UI 元素，值变化时整体替换对象
    public synchronized void defineUi(UiPayloads.Define payload) {
        UiElement element = new UiElement(
                payload.elementId(),
                payload.elementType(),
                payload.x(),
                payload.y(),
                payload.width(),
                payload.height(),
                payload.label(),
                UiElement.parseStyle(payload.style()),
                payload.value()
        );
        uiElements.put(element.id(), element);
        if (element.id().equals(focusedUiId)) {
            uiInput.setLength(0);
            uiInput.append(element.value());
        }
        dirty = true;
    }

    public synchronized void removeUi(String elementId) {
        uiElements.remove(elementId);
        if (elementId.equals(focusedUiId)) {
            focusedUiId = "";
            uiInput.setLength(0);
        }
        if (elementId.equals(pressedUiId)) {
            pressedUiId = "";
        }
        dirty = true;
    }

    public synchronized void clearUi() {
        uiElements.clear();
        focusedUiId = "";
        pressedUiId = "";
        uiInput.setLength(0);
        dirty = true;
    }

    // 编辑中显示的是本地缓冲，服务端回值只更新底层值，不影响正在敲的字
    public synchronized void setUiValue(String elementId, String value) {
        UiElement element = uiElements.get(elementId);
        if (element == null) {
            return;
        }
        uiElements.put(elementId, element.withValue(value));
        dirty = true;
    }

    public synchronized UiScene uiScene() {
        return new UiScene(List.copyOf(uiElements.values()), focusedUiId, pressedUiId, uiInput.toString());
    }

    // 后加的元素在上层，命中测试从后往前找
    public synchronized UiElement uiAt(double pixelX, double pixelY) {
        List<UiElement> elements = new ArrayList<>(uiElements.values());
        for (int index = elements.size() - 1; index >= 0; index--) {
            UiElement element = elements.get(index);
            if (element.contains(pixelX, pixelY)) {
                return element;
            }
        }
        return null;
    }

    public synchronized void pressUi(String elementId) {
        UiElement element = uiElements.get(elementId);
        if (element == null || !"button".equals(element.type())) {
            return;
        }
        pressedUiId = elementId;
        dirty = true;
        sendToServer(new UiPayloads.Event(id, elementId, UiPayloads.ACTION_PRESS, ""));
    }

    // 鼠标松开时无论指针在哪都回传，按下态不会卡住
    public synchronized void releaseUi() {
        if (pressedUiId.isEmpty()) {
            return;
        }
        String elementId = pressedUiId;
        pressedUiId = "";
        dirty = true;
        sendToServer(new UiPayloads.Event(id, elementId, UiPayloads.ACTION_RELEASE, ""));
    }

    public synchronized boolean uiInputFocused() {
        return !focusedUiId.isEmpty();
    }

    public synchronized void focusUiInput(String elementId) {
        UiElement element = uiElements.get(elementId);
        if (element == null || !"input".equals(element.type())) {
            return;
        }
        focusedUiId = elementId;
        uiInput.setLength(0);
        uiInput.append(element.value());
        dirty = true;
    }

    public synchronized void typeUiCharacter(char character) {
        if (focusedUiId.isEmpty() || uiInput.length() >= MAX_UI_INPUT_LENGTH) {
            return;
        }
        uiInput.append(character);
        dirty = true;
        publishUiLive();
    }

    public synchronized void eraseUiCharacter() {
        if (focusedUiId.isEmpty() || uiInput.length() == 0) {
            return;
        }
        uiInput.deleteCharAt(uiInput.length() - 1);
        dirty = true;
        publishUiLive();
    }

    public synchronized void pasteUi(String text) {
        if (focusedUiId.isEmpty() || text == null) {
            return;
        }
        int limit = Math.min(text.length(), MAX_UI_INPUT_LENGTH - uiInput.length());
        boolean changed = false;
        for (int index = 0; index < limit; index++) {
            char character = text.charAt(index);
            if (character < 0x20 || character == 0x7F) {
                continue;
            }
            uiInput.append(character);
            changed = true;
        }
        if (changed) {
            dirty = true;
            publishUiLive();
        }
    }

    private void publishUiLive() {
        sendToServer(new UiPayloads.Event(id, focusedUiId, UiPayloads.ACTION_LIVE, uiInput.toString()));
    }

    public synchronized void commitUiInput() {
        if (focusedUiId.isEmpty()) {
            return;
        }
        String elementId = focusedUiId;
        String text = uiInput.toString();
        focusedUiId = "";
        dirty = true;
        sendToServer(new UiPayloads.Event(id, elementId, UiPayloads.ACTION_SUBMIT, text));
    }

    public synchronized void blurUiInput() {
        if (focusedUiId.isEmpty()) {
            return;
        }
        focusedUiId = "";
        dirty = true;
    }

    // 鼠标像素坐标换算成字符格
    public synchronized int cellColumn(double cursorX) {
        int column = (int) (cursorX / renderer.cellWidth());
        return Math.max(0, Math.min(buffer.cols() - 1, column));
    }

    public synchronized int cellRow(double cursorY) {
        int row = (int) (cursorY / renderer.cellHeight());
        return Math.max(0, Math.min(buffer.rows() - 1, row));
    }

    public synchronized void beginSelection(int column, int row) {
        selectionStartX = column;
        selectionStartY = row;
        selectionEndX = column;
        selectionEndY = row;
        selecting = true;
        hasSelection = true;
        dirty = true;
    }

    public synchronized void updateSelection(int column, int row) {
        if (!selecting) {
            return;
        }
        if (selectionEndX == column && selectionEndY == row) {
            return;
        }
        selectionEndX = column;
        selectionEndY = row;
        dirty = true;
    }

    public synchronized void endSelection() {
        if (!selecting) {
            return;
        }
        selecting = false;
        // 单击也保留一个字符的选区，和 conhost 一致
        dirty = true;
    }

    public synchronized boolean selecting() {
        return selecting;
    }

    // 有选区时右键复制，没有时右键粘贴
    public synchronized boolean hasSelection() {
        return hasSelection;
    }

    // 粘贴只写进输入行，控制字符直接丢弃
    public synchronized void paste(String text) {
        if (!inputPending || text == null) {
            return;
        }
        int limit = Math.min(text.length(), MAX_INPUT_LENGTH - inputLine.length());
        for (int i = 0; i < limit; i++) {
            char character = text.charAt(i);
            if (character < 0x20 || character == 0x7F) {
                continue;
            }
            typeCharacter(character);
        }
    }

    public synchronized void clearSelection() {
        selecting = false;
        hasSelection = false;
        dirty = true;
    }

    public synchronized Selection selection() {
        if (!hasSelection) {
            return null;
        }
        return new Selection(selectionStartX, selectionStartY, selectionEndX, selectionEndY);
    }

    // 按行提取选中文本，行尾空白去掉
    public synchronized String selectionText() {
        if (!hasSelection) {
            return "";
        }
        int fromX = selectionStartX;
        int fromY = selectionStartY;
        int toX = selectionEndX;
        int toY = selectionEndY;
        if (fromY > toY || (fromY == toY && fromX > toX)) {
            int swapX = fromX;
            int swapY = fromY;
            fromX = toX;
            fromY = toY;
            toX = swapX;
            toY = swapY;
        }
        StringBuilder result = new StringBuilder();
        for (int y = fromY; y <= toY; y++) {
            int startX = y == fromY ? fromX : 0;
            int endX = y == toY ? toX : buffer.cols() - 1;
            StringBuilder line = new StringBuilder();
            for (int x = startX; x <= endX; x++) {
                char character = buffer.cellAt(x, y).character();
                if (TerminalBuffer.isContinuation(character)) {
                    continue;
                }
                line.append(character);
            }
            result.append(line.toString().stripTrailing());
            if (y < toY) {
                result.append('\n');
            }
        }
        return result.toString();
    }

    public record Selection(int startX, int startY, int endX, int endY) {
        public boolean contains(int x, int y, int cols) {
            int fromX = startX;
            int fromY = startY;
            int toX = endX;
            int toY = endY;
            if (fromY > toY || (fromY == toY && fromX > toX)) {
                int swapX = fromX;
                int swapY = fromY;
                fromX = toX;
                fromY = toY;
                toX = swapX;
                toY = swapY;
            }
            int index = y * cols + x;
            return index >= fromY * cols + fromX && index <= toY * cols + toX;
        }
    }

    // 字号变化时同步调整窗口大小以保持行列不变，避免内容被重排挤掉
    public synchronized void adjustFontSize(int delta) {
        setFontSize(fontSize + delta);
    }

    public synchronized void setFontSize(int size) {
        int clamped = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
        if (clamped == fontSize) {
            if (size > MAX_FONT_SIZE && !fontLimitWarned) {
                print("\u001b[33m已到最大字号 " + MAX_FONT_SIZE + "\u001b[0m\n");
                fontLimitWarned = true;
            }
            return;
        }
        fontLimitWarned = false;
        applyFontChange(clamped, fontFamily);
    }

    public synchronized void setFontFamily(String family) {
        String target = family == null ? "" : family;
        if (target.equals(fontFamily)) {
            return;
        }
        applyFontChange(fontSize, target);
    }

    // 窗口先跟着字号长，顶到屏幕工作区就停住，之后只有渲染继续放大
    private void applyFontChange(int size, String family) {
        int cols = buffer.cols();
        int rows = buffer.rows();
        fontSize = size;
        fontFamily = family;
        renderer = new TerminalRenderer(fontSize, family, smoothMode);
        int contentWidth = cols * renderer.cellWidth();
        int contentHeight = rows * renderer.cellHeight();
        int[] limit = displayLimit();
        int width = Math.min(contentWidth, limit[0]);
        int height = Math.min(contentHeight, limit[1]);
        if (width != window.getWidth() || height != window.getHeight()) {
            resizeWindowUnrestricted(width, height);
        }
        // 行列不动，布局基准直接认新窗口尺寸，免得被当成用户拖拽而重排
        layoutWidth = width;
        layoutHeight = height;
        pendingSizeFrames = SIZE_SETTLE_FRAMES;
        dirty = true;
    }

    // 锐利度开关，-1 跟随字号，1 强制抗锯齿，0 强制像素渲染
    public synchronized void setSmoothMode(int mode) {
        int target = Math.max(-1, Math.min(1, mode));
        if (target == smoothMode) {
            return;
        }
        smoothMode = target;
        renderer = new TerminalRenderer(fontSize, fontFamily, smoothMode);
        dirty = true;
    }

    // 主显示器工作区，留出标题栏和窗口边框
    private int[] displayLimit() {
        long monitor = GLFW.glfwGetWindowMonitor(handle);
        if (monitor == 0L) {
            monitor = GLFW.glfwGetPrimaryMonitor();
        }
        if (monitor == 0L) {
            return new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE};
        }
        int[] x = new int[1];
        int[] y = new int[1];
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetMonitorWorkarea(monitor, x, y, width, height);
        return new int[]{Math.max(320, width[0] - 80), Math.max(240, height[0] - 120)};
    }

    public synchronized void setOpacity(float value) {
        opacity = Math.max(0.1F, Math.min(1.0F, value));
        window.setOverlayOpacity(opacity);
        applyWindowBackground(opacity);
        dirty = true;
    }

    public synchronized void setCursorVisible(boolean visible) {
        buffer.setCursorVisible(visible);
        dirty = true;
    }

    // Ctrl+Shift+滚轮同时降低内容层和窗口背景的透明度
    public synchronized void adjustOpacity(float delta) {
        opacity = Math.max(0.1F, Math.min(1.0F, opacity + delta));
        window.setOverlayOpacity(opacity);
        applyWindowBackground(opacity);
        dirty = true;
    }

    // 只有窗口尺寸真的变了才重排，字号变化不动窗口也不动行列
    public synchronized void resizeIfNeeded() {
        int width = window.getWidth();
        int height = window.getHeight();
        // 程序刚设过尺寸，这几帧只认下实际值
        if (pendingSizeFrames > 0) {
            pendingSizeFrames--;
            layoutWidth = width;
            layoutHeight = height;
            return;
        }
        if (width == layoutWidth && height == layoutHeight) {
            return;
        }
        int targetCols = Math.max(1, width / renderer.cellWidth());
        int targetRows = Math.max(1, height / renderer.cellHeight());
        if (targetCols != buffer.cols() || targetRows != buffer.rows()) {
            buffer.resize(targetCols, targetRows);
            dirty = true;
        }
        layoutWidth = width;
        layoutHeight = height;
    }

    // 窗口尺寸变了也必须重画，否则旧纹理会被拉伸成糊的
    public synchronized boolean needsRender(int windowWidth, int windowHeight) {
        return dirty || windowWidth != renderedWindowWidth || windowHeight != renderedWindowHeight;
    }

    public synchronized void markRendered(int windowWidth, int windowHeight) {
        renderedWindowWidth = windowWidth;
        renderedWindowHeight = windowHeight;
        dirty = false;
    }

    public synchronized int textureWidth() {
        return textureWidth;
    }

    public synchronized int textureHeight() {
        return textureHeight;
    }

    public synchronized void setTextureSize(int width, int height) {
        this.textureWidth = width;
        this.textureHeight = height;
    }

    // 双缓冲轮换，避免覆盖窗口线程还没消费的像素
    public synchronized ByteBuffer nextPixelBuffer(int width, int height) {
        int required = width * height * 4;
        int index = pixelBufferIndex;
        pixelBufferIndex = (pixelBufferIndex + 1) % pixelBuffers.length;
        ByteBuffer buffer = pixelBuffers[index];
        if (buffer == null || buffer.capacity() < required) {
            buffer = ByteBuffer.allocateDirect(required);
            pixelBuffers[index] = buffer;
        }
        buffer.clear();
        return buffer;
    }

    private int colsFor(CustomWindow window) {
        return Math.max(1, window.getWidth() / renderer.cellWidth());
    }

    private int rowsFor(CustomWindow window) {
        return Math.max(1, window.getHeight() / renderer.cellHeight());
    }
}
