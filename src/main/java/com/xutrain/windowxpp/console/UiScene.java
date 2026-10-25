package com.xutrain.windowxpp.console;

import java.util.List;

// 渲染一帧 UI 需要的全部状态
public record UiScene(
        List<UiElement> elements,
        String focusedId,
        String pressedId,
        String inputText
) {
    public static final UiScene EMPTY = new UiScene(List.of(), "", "", "");
}
