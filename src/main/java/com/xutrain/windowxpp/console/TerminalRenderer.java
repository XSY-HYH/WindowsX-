package com.xutrain.windowxpp.console;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// 把字符网格画成 ARGB 像素，布局参考 Windows Terminal
public final class TerminalRenderer {
    // conhost 的选中是白底黑字，不是蓝色高亮
    private static final int SELECTION_BACKGROUND = 0xF2F2F2;
    private static final int SELECTION_FOREGROUND = 0x000000;
    // 超过这个字号才开抗锯齿，小字开着会发虚
    private static final int SMOOTH_FONT_FROM = 20;

    // 优先选自带中文的等宽字体，其次才是西文等宽
    private static final String[] FONT_CANDIDATES = {
            "Sarasa Mono SC", "Sarasa Term SC", "Sarasa Fixed SC",
            "Noto Sans Mono CJK SC", "Noto Sans Mono CJK",
            "Cascadia Mono", "Cascadia Code", "Consolas", "Menlo",
            "DejaVu Sans Mono", "Lucida Console", "Courier New", Font.MONOSPACED
    };

    // 主字体缺字时用它兜底，保证中文不出现方块
    private static final String[] FALLBACK_CANDIDATES = {
            "Sarasa Mono SC", "Noto Sans Mono CJK SC",
            "Microsoft YaHei UI", "Microsoft YaHei", "SimSun", "NSimSun",
            "Noto Sans CJK SC", "Source Han Sans SC", Font.SANS_SERIF
    };

    private final Font normalFont;
    private final Font boldFont;
    private final Font fallbackFont;
    private final Font fallbackBoldFont;
    private final int cellWidth;
    private final int cellHeight;
    private final int baseline;
    private final int fontSize;
    private final float baselineRatio;
    private BufferedImage cachedImage;

    public TerminalRenderer(int fontSize) {
        this(fontSize, "");
    }

    // family 为空时按候选链自动挑选，指定后优先用它
    public TerminalRenderer(int fontSize, String family) {
        int size = Math.max(6, fontSize);
        Set<String> available = availableFamilies();
        String normalFamily = family == null || family.isEmpty()
                ? pick(FONT_CANDIDATES, available)
                : family;
        // 中文字形无论主字体选什么都用同一套兜底链
        String fallbackFamily = pick(FALLBACK_CANDIDATES, available);
        this.normalFont = new Font(normalFamily, Font.PLAIN, size);
        this.boldFont = normalFont.deriveFont(Font.BOLD);
        this.fallbackFont = new Font(fallbackFamily, Font.PLAIN, size);
        this.fallbackBoldFont = fallbackFont.deriveFont(Font.BOLD);

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = probe.createGraphics();
        graphics.setFont(normalFont);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        FontMetrics metrics = graphics.getFontMetrics();
        // 用大写 M 的步进作为单元宽度，避免等宽字体下字符抖动
        this.cellWidth = Math.max(1, Math.round(metrics.charWidth('M')));
        this.cellHeight = Math.max(1, metrics.getHeight());
        this.baseline = (cellHeight + metrics.getAscent() - metrics.getDescent()) / 2;
        this.baselineRatio = (float) baseline / cellHeight;
        this.fontSize = size;
        graphics.dispose();
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    public int pixelWidth(int cols) {
        return cols * cellWidth;
    }

    public int pixelHeight(int rows) {
        return rows * cellHeight;
    }

    public int[] render(
            TerminalBuffer buffer,
            boolean showCursor,
            int background,
            TerminalPalette palette,
            ConsoleSession.Selection selection
    ) {
        return render(
                buffer,
                showCursor,
                background,
                palette,
                selection,
                pixelWidth(buffer.cols()),
                pixelHeight(buffer.rows())
        );
    }

    // 目标像素尺寸由窗口决定，格宽按它均分，纹理和窗口严格同尺寸，拉伸时就是 1:1
    public int[] render(
            TerminalBuffer buffer,
            boolean showCursor,
            int background,
            TerminalPalette palette,
            ConsoleSession.Selection selection,
            int pixelWidth,
            int pixelHeight
    ) {
        int cols = buffer.cols();
        int rows = buffer.rows();
        int width = Math.max(1, pixelWidth);
        int height = Math.max(1, pixelHeight);

        // 复用离屏图像，避免每次渲染都分配显存外的整块位图
        if (cachedImage == null
                || cachedImage.getWidth() != width
                || cachedImage.getHeight() != height) {
            cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage image = cachedImage;
        Graphics2D graphics = image.createGraphics();
        // 小字号要像素对齐才锐利，大字号开抗锯齿边缘才不毛糙
        boolean smooth = fontSize >= SMOOTH_FONT_FROM;
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                smooth ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                smooth ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF
        );
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        // 用 Src 直接覆盖而非混合，保证底色是纯色而不是叠出来的
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(background));
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);

        // 逐格取整而不是固定格宽，累计误差不会留到最后一行一列
        int[] edgeX = new int[cols + 1];
        for (int x = 0; x <= cols; x++) {
            edgeX[x] = Math.round((float) width * x / cols);
        }
        int[] edgeY = new int[rows + 1];
        for (int y = 0; y <= rows; y++) {
            edgeY[y] = Math.round((float) height * y / rows);
        }

        for (int y = 0; y < rows; y++) {
            int top = edgeY[y];
            int cellHeight = edgeY[y + 1] - top;
            if (cellHeight <= 0) {
                continue;
            }
            for (int x = 0; x < cols; x++) {
                int left = edgeX[x];
                int cellWidth = edgeX[x + 1] - left;
                if (cellWidth <= 0) {
                    continue;
                }
                TerminalBuffer.Cell cell = buffer.cellAt(x, y);
                char character = cell.character();
                CellStyle style = cell.style();

                // 底色先铺满整格，宽字符的占位格也要一起上色，否则选区会有缺口
                boolean selected = selection != null && selection.contains(x, y, cols);
                if (selected) {
                    graphics.setColor(new Color(SELECTION_BACKGROUND));
                    graphics.fillRect(left, top, cellWidth, cellHeight);
                } else if (style.background() != CellStyle.UNSET && style.background() != background) {
                    graphics.setColor(new Color(style.background()));
                    graphics.fillRect(left, top, cellWidth, cellHeight);
                }
                if (TerminalBuffer.isContinuation(character)) {
                    continue;
                }
                int drawWidth = TerminalBuffer.isWide(character) && x + 1 < cols
                        ? edgeX[x + 2] - left
                        : cellWidth;
                drawWidth = Math.min(drawWidth, width - left);
                int color = selected ? SELECTION_FOREGROUND : resolveForeground(style, palette);
                if (character != ' ') {
                    Font font = pickFont(character, style.bold());
                    graphics.setFont(font);
                    graphics.setColor(new Color(color));
                    int baselineY = top + Math.round(cellHeight * baselineRatio);
                    graphics.drawString(String.valueOf(character), left, baselineY);
                }
                if (style.underline()) {
                    graphics.setColor(new Color(color));
                    graphics.fillRect(left, top + cellHeight - 1, drawWidth, 1);
                }
                if (style.strike()) {
                    graphics.setColor(new Color(color));
                    graphics.fillRect(left, top + cellHeight / 2, drawWidth, 1);
                }
            }
        }

        if (showCursor) {
            // conhost 默认光标是底部一段实心条
            int cursorColumn = Math.min(cols - 1, buffer.cursorX());
            int cursorRow = Math.min(rows - 1, buffer.cursorY());
            int cursorLeft = edgeX[cursorColumn];
            int cursorTop = edgeY[cursorRow];
            int cursorWidth = edgeX[cursorColumn + 1] - cursorLeft;
            int cursorHeight = edgeY[cursorRow + 1] - cursorTop;
            int barHeight = Math.max(2, cursorHeight / 4);
            graphics.setColor(new Color(palette.cursor()));
            graphics.fillRect(
                    cursorLeft,
                    cursorTop + cursorHeight - barHeight,
                    cursorWidth,
                    barHeight
            );
        }

        graphics.dispose();
        return ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    // 主字体缺字时切到中文字体，避免渲染成方块
    private Font pickFont(char character, boolean bold) {
        if (normalFont.canDisplay(character)) {
            return bold ? boldFont : normalFont;
        }
        return bold ? fallbackBoldFont : fallbackFont;
    }

    // 未指定颜色时跟随配色方案，粗体提亮与 faint 减半都在这里落地
    private static int resolveForeground(CellStyle style, TerminalPalette palette) {
        int color = style.foreground() == CellStyle.UNSET ? palette.foreground() : style.foreground();
        if (style.bold()) {
            color = palette.intensify(color);
        }
        if (style.faint()) {
            color = dim(color);
        }
        return color;
    }

    // faint 按每通道减半处理，和 conhost 的暗色表现一致
    private static int dim(int color) {
        int red = ((color >> 16) & 0xFF) / 2;
        int green = ((color >> 8) & 0xFF) / 2;
        int blue = (color & 0xFF) / 2;
        return red << 16 | green << 8 | blue;
    }

    // 枚举系统字体很慢，字号频繁试探时不能每次都扫一遍
    private static Set<String> familyCache;

    private static synchronized Set<String> availableFamilies() {
        if (familyCache == null) {
            familyCache = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
            ));
        }
        return familyCache;
    }

    private static String pick(String[] candidates, Set<String> available) {
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.MONOSPACED;
    }
}
