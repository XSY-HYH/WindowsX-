package com.xutrain.windowxpp.console;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// 客户端 UI 元素，坐标是窗口内像素；值变化时整体替换，避免跨线程改字段
public record UiElement(
        String id,
        String type,
        int x,
        int y,
        int width,
        int height,
        String label,
        Map<String, String> style,
        String value
) {
    public UiElement {
        label = label == null ? "" : label;
        style = style == null ? Map.of() : Map.copyOf(style);
        value = value == null ? "" : value;
    }

    public boolean contains(double pixelX, double pixelY) {
        return pixelX >= x && pixelX < x + width && pixelY >= y && pixelY < y + height;
    }

    public String style(String key, String fallback) {
        return style.getOrDefault(key, fallback);
    }

    public UiElement withValue(String newValue) {
        return new UiElement(id, type, x, y, width, height, label, style, newValue);
    }

    // 服务端下发的样式是 k=v;k=v
    public static Map<String, String> parseStyle(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String part : raw.split(";")) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = part.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String value = part.substring(equals + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                result.put(key, value);
            }
        }
        return result;
    }
}
