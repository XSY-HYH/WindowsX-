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
import java.util.Locale;
import java.util.Set;

// 把字符网格画成 ARGB 像素，布局参考 Windows Terminal
public final class TerminalRenderer {
    // conhost 的选中是白底黑字，不是蓝色高亮
    private static final int SELECTION_BACKGROUND = 0xF2F2F2;
    private static final int SELECTION_FOREGROUND = 0x000000;
    // conhost 走 ClearType，混色是带伽马校正的；Java2D 的 LCD 模式是对应实现
    private static final int LCD_CONTRAST = 180;
    // conhost 靠内嵌点阵做到小字号锐利，Java2D 拿不到点阵，只能在小字号上退回像素渲染
    private static final int PIXEL_RENDER_MAX_HEIGHT = 20;

    // conhost 按代码页从注册表挑默认字体，中日韩各有自己的等宽字体，其余回退 Consolas
    private static final String[] FONT_CANDIDATES = buildCandidates();

    // 第一顺位对齐 conhost 的系统默认字体，后面的只是没装时依次回退
    private static String[] buildCandidates() {
        String language = Locale.getDefault().getLanguage();
        String preferred = switch (language) {
            case "zh" -> "NSimSun";
            case "ja" -> "MS Gothic";
            case "ko" -> "GulimChe";
            default -> "Consolas";
        };
        return new String[]{
                preferred,
                "Consolas", "Cascadia Mono", "Cascadia Code",
                "Sarasa Mono SC", "Noto Sans Mono CJK SC",
                "Lucida Console", "Courier New", Font.MONOSPACED
        };
    }

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
    private final int smoothMode;
    private BufferedImage cachedImage;

    // smoothMode 传 -1 跟随字号自动判断，0 强制像素渲染，1 强制抗锯齿
    public TerminalRenderer(int fontSize, String family, int smoothMode) {
        int size = Math.max(6, fontSize);
        this.smoothMode = smoothMode;
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
        // 小字号优先像素渲染，锐利度接近 conhost 的内嵌点阵；大字号才用带校正的子像素抗锯齿
        boolean smooth = smoothMode > 0 || (smoothMode < 0 && cellHeight > PIXEL_RENDER_MAX_HEIGHT);
        if (smooth) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB
            );
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, LCD_CONTRAST);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
            );
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        // 用 Src 直接覆盖而非混合，保证底色是纯色而不是叠出来的
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(background));
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);

        // 格子按字体真实尺寸排布，超出纹理的部分直接裁掉，和 conhost 把字形画出视口外一致
        for (int y = 0; y < rows; y++) {
            int top = y * cellHeight;
            if (top >= height) {
                break;
            }
            for (int x = 0; x < cols; x++) {
                int left = x * cellWidth;
                if (left >= width) {
                    break;
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
                int drawWidth = Math.min(
                        TerminalBuffer.isWide(character) ? cellWidth * 2 : cellWidth,
                        width - left
                );
                int color = selected ? SELECTION_FOREGROUND : resolveForeground(style, palette);
                if (character != ' ') {
                    Font font = pickFont(character, style.bold());
                    graphics.setFont(font);
                    graphics.setColor(new Color(color));
                    graphics.drawString(String.valueOf(character), left, top + baseline);
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
            int cursorLeft = buffer.cursorX() * cellWidth;
            int cursorTop = buffer.cursorY() * cellHeight;
            if (cursorLeft < width && cursorTop < height) {
                int barHeight = Math.max(2, cellHeight / 4);
                graphics.setColor(new Color(palette.cursor()));
                graphics.fillRect(
                        cursorLeft,
                        cursorTop + cellHeight - barHeight,
                        Math.min(cellWidth, width - cursorLeft),
                        barHeight
                );
            }
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
