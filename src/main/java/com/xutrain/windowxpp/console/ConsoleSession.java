package com.xutrain.windowxpp.console;

import com.xutrain.windowx.client.multiwindow.CustomWindow;

import java.nio.ByteBuffer;
import java.util.UUID;

// 一个控制台窗口及其终端状态
public final class ConsoleSession {
    private static final int MIN_FONT_SIZE = 6;
    private static final int MAX_FONT_SIZE = 48;

    private final UUID owner;
    private final String id;
    private final CustomWindow window;
    private final long handle;
    private final String flags;
    private TerminalRenderer renderer;
    private TerminalBuffer buffer;
    private final AnsiParser parser = new AnsiParser();

    private final StringBuilder inputLine = new StringBuilder();
    private boolean inputPending;
    private String inputObjective = "";
    private int fontSize;
    private float opacity = 1.0F;
    private int background = AnsiColors.DEFAULT_BACKGROUND;
    private boolean dirty = true;
    private int textureWidth;
    private int textureHeight;
    private final ByteBuffer[] pixelBuffers = new ByteBuffer[2];
    private int pixelBufferIndex;

    public ConsoleSession(UUID owner, String id, CustomWindow window, int fontSize, String flags, int background) {
        this.owner = owner;
        this.id = id;
        this.window = window;
        this.handle = window.getHandle();
        this.flags = flags == null ? "" : flags;
        this.fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
        this.background = background;
        this.renderer = new TerminalRenderer(this.fontSize);
        this.buffer = new TerminalBuffer(colsFor(window), rowsFor(window));
        window.setOverlayOpacity(1.0F);
        window.setContentLayer("T");
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
        parser.reset();
        buffer.clearScreen();
        dirty = true;
    }

    public synchronized boolean inputPending() {
        return inputPending;
    }

    public synchronized void beginInput(String objective) {
        inputPending = true;
        inputObjective = objective == null ? "" : objective;
        inputLine.setLength(0);
        dirty = true;
    }

    public synchronized String inputObjective() {
        return inputObjective;
    }

    public synchronized void cancelInput() {
        inputPending = false;
        inputObjective = "";
        inputLine.setLength(0);
        dirty = true;
    }

    public synchronized void typeCharacter(char character) {
        if (!inputPending) {
            return;
        }
        inputLine.append(character);
        buffer.write(character, CellStyle.DEFAULT);
        dirty = true;
    }

    public synchronized void eraseCharacter() {
        if (!inputPending || inputLine.length() == 0) {
            return;
        }
        inputLine.deleteCharAt(inputLine.length() - 1);
        buffer.backspace();
        dirty = true;
    }

    public synchronized String submitInput() {
        if (!inputPending) {
            return null;
        }
        String value = inputLine.toString();
        inputPending = false;
        inputObjective = "";
        inputLine.setLength(0);
        // 回车换行需要回到行首，否则下一行会阶梯式右移
        buffer.carriageReturn();
        buffer.newline();
        dirty = true;
        return value;
    }

    public synchronized String inputText() {
        return inputLine.toString();
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

    // 字号变化时同步调整窗口大小以保持行列不变，避免内容被重排挤掉
    public synchronized void adjustFontSize(int delta) {
        int target = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize + delta));
        if (target == fontSize) {
            return;
        }
        int cols = buffer.cols();
        int rows = buffer.rows();
        fontSize = target;
        renderer = new TerminalRenderer(fontSize);
        window.setSize(cols * renderer.cellWidth(), rows * renderer.cellHeight());
        dirty = true;
    }

    // Ctrl+Shift+滚轮同时降低内容层和窗口背景的透明度
    public synchronized void adjustOpacity(float delta) {
        opacity = Math.max(0.1F, Math.min(1.0F, opacity + delta));
        window.setOverlayOpacity(opacity);
        float red = ((background >> 16) & 0xFF) / 255.0F;
        float green = ((background >> 8) & 0xFF) / 255.0F;
        float blue = (background & 0xFF) / 255.0F;
        window.setBackgroundColor(red, green, blue, opacity);
        dirty = true;
    }

    // 窗口尺寸变化时按新的字符数重排内容
    public synchronized void resizeIfNeeded() {
        int targetCols = colsFor(window);
        int targetRows = rowsFor(window);
        if (targetCols == buffer.cols() && targetRows == buffer.rows()) {
            return;
        }
        buffer.resize(targetCols, targetRows);
        dirty = true;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void markDirty() {
        dirty = true;
    }

    public synchronized void markClean() {
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
