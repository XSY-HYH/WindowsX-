package com.xutrain.windowxpp.console;

// 终端单元的显示样式，来自 SGR 序列
public final class CellStyle {
    public static final CellStyle DEFAULT = new CellStyle(
            AnsiColors.DEFAULT_FOREGROUND,
            AnsiColors.DEFAULT_BACKGROUND,
            false,
            false,
            false
    );

    private final int foreground;
    private final int background;
    private final boolean bold;
    private final boolean underline;
    private final boolean inverse;

    public CellStyle(int foreground, int background, boolean bold, boolean underline, boolean inverse) {
        this.foreground = foreground;
        this.background = background;
        this.bold = bold;
        this.underline = underline;
        this.inverse = inverse;
    }

    public int foreground() {
        return inverse ? background : foreground;
    }

    public int background() {
        return inverse ? foreground : background;
    }

    public boolean bold() {
        return bold;
    }

    public boolean underline() {
        return underline;
    }

    public CellStyle withForeground(int color) {
        return new CellStyle(color, background, bold, underline, inverse);
    }

    public CellStyle withBackground(int color) {
        return new CellStyle(foreground, color, bold, underline, inverse);
    }

    public CellStyle withBold(boolean value) {
        return new CellStyle(foreground, background, value, underline, inverse);
    }

    public CellStyle withUnderline(boolean value) {
        return new CellStyle(foreground, background, bold, value, inverse);
    }

    public CellStyle withInverse(boolean value) {
        return new CellStyle(foreground, background, bold, underline, value);
    }
}
