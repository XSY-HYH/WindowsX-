package com.xutrain.windowxpp.server;

import java.util.Locale;

// 元素绑定目标：原版计分板用 # 前缀，伪变量用 $ 前缀
public record UiBind(Source source, String name, Mode mode) {
    public static final UiBind NONE = new UiBind(Source.NONE, "", Mode.PRESS);

    public enum Source {
        NONE, OBJECTIVE, VARIABLE
    }

    public enum Mode {
        PRESS, TOGGLE, LIVE
    }

    // fallback 是没写行为后缀时的默认模式，按钮默认按下、输入框默认实时
    public static UiBind parse(String raw, Mode fallback) {
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
            return NONE;
        }
        String text = raw.trim();
        Mode mode = fallback;
        int slash = text.lastIndexOf('/');
        if (slash > 0) {
            Mode parsed = modeOf(text.substring(slash + 1));
            if (parsed != null) {
                mode = parsed;
                text = text.substring(0, slash);
            }
        }
        if (text.startsWith("#") && text.length() > 1) {
            return new UiBind(Source.OBJECTIVE, text.substring(1), mode);
        }
        if (text.startsWith("$") && text.length() > 1) {
            return new UiBind(Source.VARIABLE, text.substring(1), mode);
        }
        // 没写前缀时按伪变量处理，省掉玩家记符号的负担
        return new UiBind(Source.VARIABLE, text, mode);
    }

    private static Mode modeOf(String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "press" -> Mode.PRESS;
            case "toggle" -> Mode.TOGGLE;
            case "live" -> Mode.LIVE;
            default -> null;
        };
    }
}
