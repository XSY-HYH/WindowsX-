package com.xutrain.windowxpp.console;

// Campbell 配色与 256 色表，色值取自 Windows Terminal
public final class AnsiColors {
    public static final int DEFAULT_FOREGROUND = 0xCCCCCC;
    public static final int DEFAULT_BACKGROUND = 0x0C0C0C;
    public static final int DEFAULT_CURSOR = 0xFFFFFF;

    private static final int[] BASIC = {
            0x0C0C0C, 0xC50F1F, 0x13A10E, 0xC19C00,
            0x0037DA, 0x881798, 0x3A96DD, 0xCCCCCC,
            0x767676, 0xE74856, 0x16C60C, 0xF9F1A5,
            0x3B78FF, 0xB4009E, 0x61D6D6, 0xF2F2F2
    };

    private static final int[] CUBE_STEPS = {0x00, 0x5F, 0x87, 0xAF, 0xD7, 0xFF};

    private AnsiColors() {
    }

    public static int basic(int index) {
        if (index < 0 || index >= BASIC.length) {
            return DEFAULT_FOREGROUND;
        }
        return BASIC[index];
    }

    public static int color256(int index) {
        if (index < 0 || index > 255) {
            return DEFAULT_FOREGROUND;
        }
        if (index < 16) {
            return BASIC[index];
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

    // 粗体把前 8 色提升到对应亮色，与 IntenseIsBright 行为一致
    public static int intensify(int color) {
        for (int i = 0; i < 8; i++) {
            if (BASIC[i] == color) {
                return BASIC[i + 8];
            }
        }
        return color;
    }
}
