package com.xutrain.windowxpp.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xutrain.windowxpp.console.AnsiColors;
import com.xutrain.windowxpp.console.ConsoleColor;
import com.xutrain.windowxpp.network.ConsolePayloads;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.function.Function;

// 控制台窗口命令，语法与 windowxm 保持一致
public final class WindowXCCommand {
    private static final DynamicCommandExceptionType ERROR_MISSING_OBJECTIVE =
            new DynamicCommandExceptionType(name -> Component.literal("计分板不存在: " + name));
    private static final DynamicCommandExceptionType ERROR_OBJECTIVE_TYPE =
            new DynamicCommandExceptionType(name -> Component.literal("计分板不是整数类型: " + name));

    // 只补全整数型计分板，写别的类型没有意义
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_OBJECTIVES = (context, builder) -> {
        for (Objective objective : context.getSource().getServer().getScoreboard().getObjectives()) {
            if (objective.getCriteria().getDefaultRenderType() == ObjectiveCriteria.RenderType.INTEGER) {
                builder.suggest(objective.getName());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_OPTIONS = (context, builder) -> {
        builder.suggest("@noshrink");
        builder.suggest("@nogrow");
        builder.suggest("@noclose");
        builder.suggest("@noshrink,nogrow,noclose");
        builder.suggest("@bg=#101010");
        builder.suggest("@bg=#101010,noclose");
        return builder.buildFuture();
    };

    private WindowXCCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("windowsxc")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(add())
                                .then(remove())
                                .then(title())
                                .then(top())
                                .then(bg())
                                .then(print())
                                .then(clear())
                                .then(read()))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> add() {
        return Commands.literal("add").then(addIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(addXNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addXNode() {
        return Commands.argument("x", IntegerArgumentType.integer()).then(addYNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addYNode() {
        return Commands.argument("y", IntegerArgumentType.integer()).then(addHeightNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addHeightNode() {
        return Commands.argument("height", IntegerArgumentType.integer(1)).then(addWidthNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addWidthNode() {
        return Commands.argument("width", IntegerArgumentType.integer(1)).then(addColorNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addColorNode() {
        return Commands.argument("color", StringArgumentType.word())
                .then(addFlagsNode())
                .then(addTitleNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addFlagsNode() {
        return Commands.argument("options", ConsoleOptionsArgumentType.options())
                .suggests(SUGGEST_OPTIONS)
                .executes(context -> add(context, context.getArgument("options", String.class), ""))
                .then(addTitleAfterFlagsNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addTitleAfterFlagsNode() {
        return Commands.argument("title", StringArgumentType.greedyString())
                .executes(context -> add(
                        context,
                        context.getArgument("options", String.class),
                        StringArgumentType.getString(context, "title")
                ));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addTitleNode() {
        return Commands.argument("title", StringArgumentType.greedyString())
                .executes(context -> add(context, "", StringArgumentType.getString(context, "title")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> remove() {
        return Commands.literal("remove")
                .then(Commands.argument("id", ConsoleIdArgumentType.consoleId())
                        .executes(WindowXCCommand::remove));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> title() {
        return Commands.literal("title").then(titleIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> titleIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(titleTextNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> titleTextNode() {
        return Commands.argument("title", StringArgumentType.greedyString())
                .executes(WindowXCCommand::setTitle);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> top() {
        return Commands.literal("top").then(topIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> topIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(topStateNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> topStateNode() {
        return Commands.argument("state", BoolArgumentType.bool())
                .executes(context -> top(context, BoolArgumentType.getBool(context, "state")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> bg() {
        return Commands.literal("bg").then(bgIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> bgIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(bgColorNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> bgColorNode() {
        return Commands.argument("color", StringArgumentType.word())
                .executes(WindowXCCommand::setBackground);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> print() {
        return Commands.literal("print").then(printIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> printIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(printTextNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> printTextNode() {
        return Commands.argument("text", StringArgumentType.greedyString())
                .executes(WindowXCCommand::print);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clear() {
        return Commands.literal("clear")
                .then(Commands.argument("id", ConsoleIdArgumentType.consoleId())
                        .executes(WindowXCCommand::clear));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> read() {
        return Commands.literal("read").then(readIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> readIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(readObjectiveNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> readObjectiveNode() {
        return Commands.argument("objective", StringArgumentType.word())
                .suggests(SUGGEST_OBJECTIVES)
                .executes(context -> read(context, false, ""))
                .then(readFocusNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> readFocusNode() {
        return Commands.argument("focus", BoolArgumentType.bool())
                .executes(context -> read(context, BoolArgumentType.getBool(context, "focus"), ""))
                .then(readPromptNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> readPromptNode() {
        return Commands.argument("prompt", StringArgumentType.greedyString())
                .executes(context -> read(
                        context,
                        BoolArgumentType.getBool(context, "focus"),
                        StringArgumentType.getString(context, "prompt")
                ));
    }

    private static int add(
            CommandContext<CommandSourceStack> context,
            String options,
            String title
    ) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int height = IntegerArgumentType.getInteger(context, "height");
        int width = IntegerArgumentType.getInteger(context, "width");
        String color = StringArgumentType.getString(context, "color");
        int background = ConsoleColor.parse(optionValue(options, "bg"), AnsiColors.DEFAULT_BACKGROUND);
        String flags = flagsOf(options);
        return broadcast(context, player -> new ConsolePayloads.Create(
                id, x, y, width, height, color, title, flags, background
        ));
    }

    // options 形如 bg=#101010,noclose，取指定键的值
    private static String optionValue(String options, String key) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        for (String part : options.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(key + "=")) {
                return trimmed.substring(key.length() + 1);
            }
        }
        return "";
    }

    // 去掉键值对后剩下的就是窗口标志
    private static String flagsOf(String options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String part : options.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty() || trimmed.contains("=")) {
                continue;
            }
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(trimmed);
        }
        return result.toString();
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.Remove(id));
    }

    private static int setTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String title = StringArgumentType.getString(context, "title");
        return broadcast(context, player -> new ConsolePayloads.SetTitle(id, title));
    }

    private static int top(CommandContext<CommandSourceStack> context, boolean state) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.Top(id, state));
    }

    private static int setBackground(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int color = ConsoleColor.parse(
                StringArgumentType.getString(context, "color"),
                AnsiColors.DEFAULT_BACKGROUND
        );
        return broadcast(context, player -> new ConsolePayloads.SetBackground(id, color));
    }

    private static int print(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String text = StringArgumentType.getString(context, "text");
        return broadcast(context, player -> new ConsolePayloads.Print(id, text));
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.Clear(id));
    }

    private static int read(
            CommandContext<CommandSourceStack> context,
            boolean focus,
            String prompt
    ) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String objectiveName = StringArgumentType.getString(context, "objective");
        Objective objective = context.getSource().getServer().getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            throw ERROR_MISSING_OBJECTIVE.create(objectiveName);
        }
        // 只接受整数型计分板，避免把数字写进血量这类只读准则
        if (objective.getCriteria().getDefaultRenderType() != ObjectiveCriteria.RenderType.INTEGER) {
            throw ERROR_OBJECTIVE_TYPE.create(objectiveName);
        }
        return broadcast(context, player -> new ConsolePayloads.InputRequest(id, prompt, objectiveName, focus));
    }

    private static int broadcast(
            CommandContext<CommandSourceStack> context,
            Function<ServerPlayer, CustomPacketPayload> factory
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, factory.apply(player));
        }
        return players.size();
    }
}
