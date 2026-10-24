package com.xutrain.windowxpp.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

import java.util.List;

// 选项参数必须带 @ 前缀，避免和标题混淆
public final class ConsoleOptionsArgumentType implements ArgumentType<String> {
    private static final SimpleCommandExceptionType ERROR_MISSING_PREFIX =
            new SimpleCommandExceptionType(Component.literal("选项需要以 @ 开头"));
    private static final List<String> EXAMPLES = List.of("@noclose", "@bg=#101010,noshrink");

    private ConsoleOptionsArgumentType() {
    }

    public static ConsoleOptionsArgumentType options() {
        return new ConsoleOptionsArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead() || reader.peek() != '@') {
            throw ERROR_MISSING_PREFIX.createWithContext(reader);
        }
        reader.skip();
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        if (reader.getCursor() == start) {
            throw ERROR_MISSING_PREFIX.createWithContext(reader);
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public List<String> getExamples() {
        return EXAMPLES;
    }
}
