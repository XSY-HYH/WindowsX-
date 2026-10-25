package com.xutrain.windowxpp.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 服务端保存 UI 元素定义，点击回传后按这里的绑定写值
public final class UiRegistry {
    private static final Map<UUID, Map<String, LinkedHashMap<String, Element>>> REGISTRY = new HashMap<>();

    private UiRegistry() {
    }

    public record Element(
            String id,
            String type,
            int x,
            int y,
            int width,
            int height,
            String label,
            String style,
            UiBind bind
    ) {
    }

    public static void define(UUID owner, String consoleId, Element element) {
        REGISTRY.computeIfAbsent(owner, key -> new HashMap<>())
                .computeIfAbsent(consoleId, key -> new LinkedHashMap<>())
                .put(element.id(), element);
    }

    public static Element get(UUID owner, String consoleId, String elementId) {
        Map<String, LinkedHashMap<String, Element>> consoles = REGISTRY.get(owner);
        if (consoles == null) {
            return null;
        }
        LinkedHashMap<String, Element> elements = consoles.get(consoleId);
        return elements == null ? null : elements.get(elementId);
    }

    public static void remove(UUID owner, String consoleId, String elementId) {
        Map<String, LinkedHashMap<String, Element>> consoles = REGISTRY.get(owner);
        if (consoles == null) {
            return;
        }
        LinkedHashMap<String, Element> elements = consoles.get(consoleId);
        if (elements != null) {
            elements.remove(elementId);
        }
    }

    public static void clear(UUID owner, String consoleId) {
        Map<String, LinkedHashMap<String, Element>> consoles = REGISTRY.get(owner);
        if (consoles != null) {
            consoles.remove(consoleId);
        }
    }

    public static Collection<Element> all(UUID owner, String consoleId) {
        Map<String, LinkedHashMap<String, Element>> consoles = REGISTRY.get(owner);
        if (consoles == null) {
            return List.of();
        }
        LinkedHashMap<String, Element> elements = consoles.get(consoleId);
        return elements == null ? List.of() : List.copyOf(elements.values());
    }

    // 换样式时只改样式字段，其余保持原样
    public static Element withStyle(Element element, String style) {
        return new Element(
                element.id(),
                element.type(),
                element.x(),
                element.y(),
                element.width(),
                element.height(),
                element.label(),
                style,
                element.bind()
        );
    }

    public static void removeOwner(UUID owner) {
        REGISTRY.remove(owner);
    }
}
