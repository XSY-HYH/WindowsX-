package com.xutrain.windowxpp.console;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

// 把 UI 元素画在终端像素上，和终端共用同一张纹理
public final class UiRenderer {
    private UiRenderer() {
    }

    public static void paint(
            Graphics2D graphics,
            UiScene scene,
            int width,
            int height,
            Font normalFont,
            Font boldFont
    ) {
        if (scene == null || scene.elements().isEmpty()) {
            return;
        }
        for (UiElement element : scene.elements()) {
            switch (element.type()) {
                case "button" -> paintButton(graphics, element, scene, normalFont, boldFont);
                case "input" -> paintInput(graphics, element, scene, normalFont, boldFont);
                default -> paintLabel(graphics, element, normalFont, boldFont);
            }
        }
    }

    private static void paintButton(
            Graphics2D graphics,
            UiElement element,
            UiScene scene,
            Font normalFont,
            Font boldFont
    ) {
        boolean active = element.id().equals(scene.pressedId()) || "1".equals(element.value());
        int background = color(element.style("bg", "#2D2D30"), 0xFF2D2D30);
        int activeBackground = color(element.style("active-bg", "#007ACC"), 0xFF007ACC);
        int border = color(element.style("border", "#3F3F46"), 0xFF3F3F46);
        int foreground = color(element.style("fg", "#FFFFFF"), 0xFFFFFFFF);
        int activeForeground = color(element.style("active-fg", element.style("fg", "#FFFFFF")), 0xFFFFFFFF);
        paintBox(
                graphics,
                element,
                active ? activeBackground : background,
                border,
                element.style("border-width", "1")
        );
        Font font = font(element, normalFont, boldFont);
        drawText(
                graphics,
                element,
                element.label(),
                font,
                active ? activeForeground : foreground,
                element.style("align", "center")
        );
    }

    private static void paintInput(
            Graphics2D graphics,
            UiElement element,
            UiScene scene,
            Font normalFont,
            Font boldFont
    ) {
        boolean focused = element.id().equals(scene.focusedId());
        int background = color(element.style("bg", "#1E1E1E"), 0xFF1E1E1E);
        int border = color(
                element.style("border", focused ? "#007ACC" : "#3F3F46"),
                focused ? 0xFF007ACC : 0xFF3F3F46
        );
        int foreground = color(element.style("fg", "#FFFFFF"), 0xFFFFFFFF);
        int placeholder = color(element.style("placeholder", "#808080"), 0xFF808080);
        paintBox(graphics, element, background, border, element.style("border-width", "1"));

        String text = focused ? scene.inputText() : element.value();
        boolean empty = text.isEmpty();
        String shown = empty ? element.label() : text;
        Font font = font(element, normalFont, boldFont);
        int textColor = empty && !focused ? placeholder : foreground;
        int textX = drawText(
                graphics,
                element,
                shown,
                font,
                textColor,
                element.style("align", "left")
        );
        if (focused) {
            FontMetrics metrics = graphics.getFontMetrics(font);
            int padding = intStyle(element, "padding", 6);
            int cursorX = empty ? textX : textX + metrics.stringWidth(text);
            int baseline = baseline(element, metrics);
            int cursorTop = baseline - metrics.getAscent();
            int cursorHeight = metrics.getAscent() + metrics.getDescent();
            int limit = element.x() + element.width() - padding;
            if (cursorX <= limit) {
                graphics.setColor(new Color(foreground, true));
                graphics.fillRect(cursorX + 1, cursorTop, 2, cursorHeight);
            }
        }
    }

    private static void paintLabel(Graphics2D graphics, UiElement element, Font normalFont, Font boldFont) {
        int foreground = color(element.style("fg", "#DDDDDD"), 0xFFDDDDDD);
        Font font = font(element, normalFont, boldFont);
        drawText(
                graphics,
                element,
                element.label(),
                font,
                foreground,
                element.style("align", "left")
        );
    }

    // 圆角矩形加边框，border-width 为 0 时只填底色
    private static void paintBox(
            Graphics2D graphics,
            UiElement element,
            int background,
            int border,
            String borderWidthText
    ) {
        int radius = intStyle(element, "radius", 4);
        int borderWidth = parseInt(borderWidthText, 1);
        Shape shape = radius > 0
                ? new RoundRectangle2D.Float(
                        element.x(), element.y(), element.width(), element.height(), radius * 2F, radius * 2F
                )
                : new Rectangle(element.x(), element.y(), element.width(), element.height());
        graphics.setColor(new Color(background, true));
        graphics.fill(shape);
        if (borderWidth > 0 && (border >>> 24) != 0) {
            graphics.setColor(new Color(border, true));
            graphics.setStroke(new java.awt.BasicStroke(borderWidth));
            graphics.draw(shape);
        }
    }

    // 返回文字起始 x，输入框的光标要接着它算
    private static int drawText(
            Graphics2D graphics,
            UiElement element,
            String text,
            Font font,
            int foreground,
            String align
    ) {
        if (text.isEmpty()) {
            return element.x() + intStyle(element, "padding", 6);
        }
        FontMetrics metrics = graphics.getFontMetrics(font);
        int padding = intStyle(element, "padding", 6);
        int textWidth = metrics.stringWidth(text);
        int textX = switch (align.toLowerCase(Locale.ROOT)) {
            case "center" -> element.x() + (element.width() - textWidth) / 2;
            case "right" -> element.x() + element.width() - padding - textWidth;
            default -> element.x() + padding;
        };
        int baseline = baseline(element, metrics);
        Shape clip = graphics.getClip();
        graphics.clipRect(element.x(), element.y(), element.width(), element.height());
        graphics.setFont(font);
        graphics.setColor(new Color(foreground, true));
        graphics.drawString(text, textX, baseline);
        graphics.setClip(clip);
        return textX;
    }

    private static int baseline(UiElement element, FontMetrics metrics) {
        return element.y() + (element.height() - metrics.getHeight()) / 2 + metrics.getAscent();
    }

    private static Font font(UiElement element, Font normalFont, Font boldFont) {
        boolean bold = "true".equalsIgnoreCase(element.style("bold", "false"));
        Font base = bold ? boldFont : normalFont;
        int size = intStyle(element, "font", 0);
        return size > 0 ? base.deriveFont((float) size) : base;
    }

    private static int intStyle(UiElement element, String key, int fallback) {
        return parseInt(element.style(key, ""), fallback);
    }

    private static int parseInt(String text, int fallback) {
        if (text == null || text.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // 支持 #RRGGBB、#AARRGGBB 与几个常用名字
    private static int color(String text, int fallback) {
        if (text == null || text.isEmpty()) {
            return fallback;
        }
        String value = text.trim();
        if (value.startsWith("#")) {
            try {
                if (value.length() == 7) {
                    return 0xFF000000 | Integer.parseInt(value.substring(1), 16);
                }
                if (value.length() == 9) {
                    return (int) Long.parseLong(value.substring(1), 16);
                }
            } catch (NumberFormatException e) {
                return fallback;
            }
            return fallback;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "white" -> 0xFFFFFFFF;
            case "black" -> 0xFF000000;
            case "gray", "grey" -> 0xFF808080;
            case "red" -> 0xFFFF5555;
            case "green" -> 0xFF55FF55;
            case "blue" -> 0xFF5555FF;
            case "yellow" -> 0xFFFFFF55;
            case "transparent" -> 0;
            default -> fallback;
        };
    }
}
