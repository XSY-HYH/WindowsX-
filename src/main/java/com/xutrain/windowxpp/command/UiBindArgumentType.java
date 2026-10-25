package com.xutrain.windowxpp.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;

import java.util.List;

// 绑定参数要接受 # $ 前缀和 / 后缀，word 类型不允许这些符号
public final class UiBindArgumentType implements ArgumentType<String> {
    private static final List<String> EXAMPLES = List.of("-", "#menu/press", "$volume/live");

    private UiBindArgumentType() {
    }

    public static UiBindArgumentType bind() {
        return new UiBindArgumentType();
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && isAllowed(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean isAllowed(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '_'
                || value == '-'
                || value == '.'
                || value == '#'
                || value == '$'
                || value == '/';
    }

    @Override
    public List<String> getExamples() {
        return EXAMPLES;
    }
}
