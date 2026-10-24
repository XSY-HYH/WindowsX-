package com.xutrain.windowxpp.console;

// 终端单元的显示样式，来自 SGR 序列
public final class CellStyle {
    // 跟随当前配色方案的默认色，切换 palette 时不用重写整屏
    public static final int UNSET = -1;

    public static final CellStyle DEFAULT = new CellStyle(
            UNSET,
            UNSET,
            false,
            false,
            false,
            false,
            false,
            false
    );

    private final int foreground;
    private final int background;
    private final boolean bold;
    private final boolean underline;
    private final boolean inverse;
    private final boolean faint;
    private final boolean blink;
    private final boolean strike;

    public CellStyle(
            int foreground,
            int background,
            boolean bold,
            boolean underline,
            boolean inverse,
            boolean faint,
            boolean blink,
            boolean strike
    ) {
        this.foreground = foreground;
        this.background = background;
        this.bold = bold;
        this.underline = underline;
        this.inverse = inverse;
        this.faint = faint;
        this.blink = blink;
        this.strike = strike;
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

    public boolean faint() {
        return faint;
    }

    public boolean blink() {
        return blink;
    }

    public boolean strike() {
        return strike;
    }

    private CellStyle copy(int fg, int bg, boolean boldValue, boolean underlineValue,
                           boolean inverseValue, boolean faintValue, boolean blinkValue, boolean strikeValue) {
        return new CellStyle(fg, bg, boldValue, underlineValue, inverseValue, faintValue, blinkValue, strikeValue);
    }

    public CellStyle withForeground(int color) {
        return copy(color, background, bold, underline, inverse, faint, blink, strike);
    }

    public CellStyle withBackground(int color) {
        return copy(foreground, color, bold, underline, inverse, faint, blink, strike);
    }

    public CellStyle withBold(boolean value) {
        return copy(foreground, background, value, underline, inverse, faint, blink, strike);
    }

    public CellStyle withUnderline(boolean value) {
        return copy(foreground, background, bold, value, inverse, faint, blink, strike);
    }

    public CellStyle withInverse(boolean value) {
        return copy(foreground, background, bold, underline, value, faint, blink, strike);
    }

    public CellStyle withFaint(boolean value) {
        return copy(foreground, background, bold, underline, inverse, value, blink, strike);
    }

    public CellStyle withBlink(boolean value) {
        return copy(foreground, background, bold, underline, inverse, faint, value, strike);
    }

    public CellStyle withStrike(boolean value) {
        return copy(foreground, background, bold, underline, inverse, faint, blink, value);
    }
}
