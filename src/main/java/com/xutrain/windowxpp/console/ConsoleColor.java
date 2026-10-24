package com.xutrain.windowxpp.console;

// 终端背景色解析，支持颜色名与 #RRGGBB
public final class ConsoleColor {
    private ConsoleColor() {
    }

    public static int parse(String value, int fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        String text = value.trim().toLowerCase();
        if (text.startsWith("#")) {
            text = text.substring(1);
        } else if (text.startsWith("0x")) {
            text = text.substring(2);
        }
        if (text.length() == 6) {
            try {
                return Integer.parseInt(text, 16);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return switch (text) {
            case "black" -> 0x0C0C0C;
            case "white" -> 0xF2F2F2;
            case "red" -> 0xC50F1F;
            case "green" -> 0x13A10E;
            case "yellow" -> 0xC19C00;
            case "blue" -> 0x0037DA;
            case "cyan" -> 0x3A96DD;
            case "magenta", "purple" -> 0x881798;
            case "gray", "grey" -> 0x767676;
            case "darkgray", "darkgrey" -> 0x404040;
            case "orange" -> 0xCC7700;
            case "pink" -> 0xE74856;
            case "navy" -> 0x101830;
            case "transparent" -> 0x000000;
            default -> fallback;
        };
    }
}
