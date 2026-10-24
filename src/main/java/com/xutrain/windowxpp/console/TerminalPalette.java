package com.xutrain.windowxpp.console;

import java.util.ArrayList;
import java.util.List;

// 一套 ANSI 配色方案：16 色加默认前景、背景和光标色
public final class TerminalPalette {
    private static final int[] CUBE_STEPS = {0x00, 0x5F, 0x87, 0xAF, 0xD7, 0xFF};

    public static final TerminalPalette CAMPBELL = new TerminalPalette(
            "campbell", 0xCCCCCC, AnsiColors.DEFAULT_BACKGROUND, 0xFFFFFF,
            new int[]{
                    0x0C0C0C, 0xC50F1F, 0x13A10E, 0xC19C00,
                    0x0037DA, 0x881798, 0x3A96DD, 0xCCCCCC,
                    0x767676, 0xE74856, 0x16C60C, 0xF9F1A5,
                    0x3B78FF, 0xB4009E, 0x61D6D6, 0xF2F2F2
            }
    );

    // 传统 conhost 配色，色相最饱和
    public static final TerminalPalette VINTAGE = new TerminalPalette(
            "vintage", 0xC0C0C0, AnsiColors.DEFAULT_BACKGROUND, 0xFFFFFF,
            new int[]{
                    0x000000, 0x800000, 0x008000, 0x808000,
                    0x000080, 0x800080, 0x008080, 0xC0C0C0,
                    0x808080, 0xFF0000, 0x00FF00, 0xFFFF00,
                    0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFFFF
            }
    );

    public static final TerminalPalette ONE_HALF_DARK = new TerminalPalette(
            "onehalfdark", 0xDCDFE4, 0x282C34, 0xDCDFE4,
            new int[]{
                    0x282C34, 0xE06C75, 0x98C379, 0xE5C07B,
                    0x61AFEF, 0xC678DD, 0x56B6C2, 0xDCDFE4,
                    0x5A6374, 0xE06C75, 0x98C379, 0xE5C07B,
                    0x61AFEF, 0xC678DD, 0x56B6C2, 0xDCDFE4
            }
    );

    public static final TerminalPalette SOLARIZED = new TerminalPalette(
            "solarized", 0x839496, 0x002B36, 0x93A1A1,
            new int[]{
                    0x073642, 0xDC322F, 0x859900, 0xB58900,
                    0x268BD2, 0xD33682, 0x2AA198, 0xEEE8D5,
                    0x002B36, 0xCB4B16, 0x586E75, 0x657B83,
                    0x839496, 0x6C71C4, 0x93A1A1, 0xFDF6E3
            }
    );

    public static final TerminalPalette TANGO = new TerminalPalette(
            "tango", 0xD3D7CF, AnsiColors.DEFAULT_BACKGROUND, 0xFFFFFF,
            new int[]{
                    0x000000, 0xCC0000, 0x4E9A06, 0xC4A000,
                    0x3465A4, 0x75507B, 0x06989A, 0xD3D7CF,
                    0x555753, 0xEF2929, 0x8AE234, 0xFCE94F,
                    0x729FCF, 0xAD7FA8, 0x34E2E2, 0xEEEEEC
            }
    );

    private static final List<TerminalPalette> ALL = List.of(
            CAMPBELL, VINTAGE, ONE_HALF_DARK, SOLARIZED, TANGO
    );

    private final String name;
    private final int foreground;
    private final int background;
    private final int cursor;
    private final int[] basic;

    private TerminalPalette(String name, int foreground, int background, int cursor, int[] basic) {
        this.name = name;
        this.foreground = foreground;
        this.background = background;
        this.cursor = cursor;
        this.basic = basic;
    }

    public static TerminalPalette defaultPalette() {
        return CAMPBELL;
    }

    // 名字不区分大小写，未知名字返回 null 交给调用方报错
    public static TerminalPalette byName(String name) {
        if (name == null) {
            return null;
        }
        for (TerminalPalette palette : ALL) {
            if (palette.name.equalsIgnoreCase(name)) {
                return palette;
            }
        }
        return null;
    }

    public static List<String> names() {
        List<String> result = new ArrayList<>(ALL.size());
        for (TerminalPalette palette : ALL) {
            result.add(palette.name);
        }
        return result;
    }

    public String name() {
        return name;
    }

    public int foreground() {
        return foreground;
    }

    public int background() {
        return background;
    }

    public int cursor() {
        return cursor;
    }

    public int basic(int index) {
        if (index < 0 || index >= basic.length) {
            return foreground;
        }
        return basic[index];
    }

    public int color256(int index) {
        if (index < 0 || index > 255) {
            return foreground;
        }
        if (index < 16) {
            return basic[index];
        }
        if (index < 232) {
            int value = index - 16;
            int red = CUBE_STEPS[value / 36];
            int green = CUBE_STEPS[(value % 36) / 6];
            int blue = CUBE_STEPS[value % 6];
            return red << 16 | green << 8 | blue;
        }
        int gray = 8 + (index - 232) * 10;
        return gray << 16 | gray << 8 | gray;
    }

    // 粗体把前 8 色提升到对应的亮色，与 IntenseIsBright 行为一致
    public int intensify(int color) {
        for (int i = 0; i < 8; i++) {
            if (basic[i] == color) {
                return basic[i + 8];
            }
        }
        return color;
    }
}
