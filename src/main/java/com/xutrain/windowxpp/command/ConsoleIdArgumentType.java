package com.xutrain.windowxpp.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;

import java.util.List;

// 控制台 id 只允许字母数字与 $
public final class ConsoleIdArgumentType implements ArgumentType<String> {
    private static final List<String> EXAMPLES = List.of("term1", "log", "shell$");

    private ConsoleIdArgumentType() {
    }

    public static ConsoleIdArgumentType consoleId() {
        return new ConsoleIdArgumentType();
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
                || value == '$';
    }

    @Override
    public List<String> getExamples() {
        return EXAMPLES;
    }
}
