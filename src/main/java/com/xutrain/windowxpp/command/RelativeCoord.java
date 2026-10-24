package com.xutrain.windowxpp.command;

// 坐标解析：r100 相对偏移，m100 鼠标偏移，p50 百分比原点
public final class RelativeCoord {
    public static final int SIZE_PERCENT_BASE = 50000;
    public static final int POSITION_PERCENT_BASE = 60000;

    private RelativeCoord() {
    }

    public static double parse(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        String text = input.trim().toLowerCase();
        if (text.equals("x")) {
            return Double.NaN;
        }
        if (text.startsWith("p")) {
            try {
                double percent = Double.parseDouble(text.substring(1));
                return (percent - 50) / 50.0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (text.startsWith("m") || text.startsWith("r")) {
            String offset = text.substring(1);
            if (offset.isEmpty()) {
                return Double.NaN;
            }
            try {
                return Double.parseDouble(offset);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isPercent(String input) {
        return input != null && input.trim().toLowerCase().startsWith("p");
    }

    public static boolean isRelative(String input) {
        return input != null && input.trim().toLowerCase().startsWith("r");
    }

    public static boolean isMouseRelative(String input) {
        return input != null && input.trim().toLowerCase().startsWith("m");
    }
}
