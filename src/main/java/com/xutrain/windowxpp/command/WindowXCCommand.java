package com.xutrain.windowxpp.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import com.xutrain.windowxpp.console.TerminalPalette;
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
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

// 控制台窗口命令，语法与 windowxm 保持一致
public final class WindowXCCommand {
    // 与 WindowX 的 WindowLayer 序列化名一致
    private static final Set<String> LAYERS = Set.of("normal", "top", "passthrough", "solid");

    private static final DynamicCommandExceptionType ERROR_MISSING_OBJECTIVE =
            new DynamicCommandExceptionType(name -> Component.literal("计分板不存在: " + name));
    private static final DynamicCommandExceptionType ERROR_OBJECTIVE_TYPE =
            new DynamicCommandExceptionType(name -> Component.literal("计分板不是整数类型: " + name));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_LAYER =
            new DynamicCommandExceptionType(name -> Component.literal("未知层级: " + name));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PALETTE =
            new DynamicCommandExceptionType(name -> Component.literal("未知配色: " + name));

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

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_LAYERS = (context, builder) -> {
        for (String layer : LAYERS) {
            builder.suggest(layer);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PALETTES = (context, builder) -> {
        for (String name : TerminalPalette.names()) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FONTS = (context, builder) -> {
        builder.suggest("Cascadia Mono");
        builder.suggest("Cascadia Code");
        builder.suggest("Consolas");
        builder.suggest("Microsoft YaHei");
        builder.suggest("SimSun");
        builder.suggest("Sarasa Mono SC");
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
                                .then(font())
                                .then(opacity())
                                .then(cursor())
                                .then(print())
                                .then(clear())
                                .then(scroll())
                                .then(region())
                                .then(size())
                                .then(move())
                                .then(input())
                                .then(layer())
                                .then(palette())
                                .then(events())
                                .then(list())
                                .then(getConsole())
                                .then(dump())
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

    private static LiteralArgumentBuilder<CommandSourceStack> font() {
        return Commands.literal("font").then(fontIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> fontIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId())
                .then(fontSizeNode())
                .then(fontFamilyNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> fontSizeNode() {
        return Commands.argument("size", IntegerArgumentType.integer(6, 200))
                .executes(WindowXCCommand::setFontSize);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> fontFamilyNode() {
        return Commands.literal("family").then(fontFamilyNameNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> fontFamilyNameNode() {
        return Commands.argument("name", StringArgumentType.greedyString())
                .suggests(SUGGEST_FONTS)
                .executes(WindowXCCommand::setFontFamily);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opacity() {
        return Commands.literal("opacity").then(opacityIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> opacityIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(opacityValueNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> opacityValueNode() {
        return Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 1.0))
                .executes(WindowXCCommand::setOpacity);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> cursor() {
        return Commands.literal("cursor").then(cursorIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> cursorIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(cursorStateNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> cursorStateNode() {
        return Commands.argument("visible", BoolArgumentType.bool())
                .executes(WindowXCCommand::setCursor);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> print() {
        return Commands.literal("print").then(printIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> printIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId())
                .then(printAtNode())
                .then(printTextNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> printAtNode() {
        return Commands.literal("at")
                .then(Commands.argument("x", IntegerArgumentType.integer(1))
                        .then(Commands.argument("y", IntegerArgumentType.integer(1))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(WindowXCCommand::printAt))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> printTextNode() {
        return Commands.argument("text", StringArgumentType.greedyString())
                .executes(WindowXCCommand::print);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clear() {
        return Commands.literal("clear").then(clearIdNode());
    }

    // before 擦光标之前，after 擦光标之后，all 整屏
    private static ArgumentBuilder<CommandSourceStack, ?> clearIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId())
                .executes(context -> clear(context, 2))
                .then(Commands.literal("before").executes(context -> clear(context, 1)))
                .then(Commands.literal("after").executes(context -> clear(context, 0)))
                .then(Commands.literal("all").executes(context -> clear(context, 2)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> scroll() {
        return Commands.literal("scroll").then(scrollIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> scrollIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(scrollLinesNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> scrollLinesNode() {
        return Commands.argument("lines", IntegerArgumentType.integer())
                .executes(WindowXCCommand::scroll);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> region() {
        return Commands.literal("region").then(regionIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> regionIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(regionTopNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> regionTopNode() {
        return Commands.argument("top", IntegerArgumentType.integer())
                .then(Commands.argument("bottom", IntegerArgumentType.integer())
                        .executes(WindowXCCommand::setRegion));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> size() {
        return Commands.literal("size").then(sizeIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> sizeIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(sizeHeightNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> sizeHeightNode() {
        return Commands.argument("height", IntegerArgumentType.integer(1))
                .then(Commands.argument("width", IntegerArgumentType.integer(1))
                        .executes(WindowXCCommand::setSize));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> move() {
        return Commands.literal("move").then(moveIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> moveIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(moveXNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> moveXNode() {
        return Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                        .executes(WindowXCCommand::move));
    }

    // 输入相关的操作，read 负责发起，其余用于中途干预
    private static LiteralArgumentBuilder<CommandSourceStack> input() {
        return Commands.literal("input")
                .then(inputCancel())
                .then(inputFill())
                .then(inputSend())
                .then(inputMask());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inputCancel() {
        return Commands.literal("cancel").then(inputCancelIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> inputCancelIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId())
                .executes(context -> cancelInput(context, ""))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> cancelInput(
                                context, StringArgumentType.getString(context, "message")
                        )));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inputFill() {
        return Commands.literal("fill").then(inputFillIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> inputFillIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> injectInput(context, false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inputSend() {
        return Commands.literal("send").then(inputSendIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> inputSendIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> injectInput(context, true)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inputMask() {
        return Commands.literal("mask").then(inputMaskIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> inputMaskIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(inputMaskStateNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> inputMaskStateNode() {
        return Commands.argument("masked", BoolArgumentType.bool())
                .executes(WindowXCCommand::setMask);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> layer() {
        return Commands.literal("layer").then(layerIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> layerIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(layerNameNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> layerNameNode() {
        return Commands.argument("layer", StringArgumentType.word())
                .suggests(SUGGEST_LAYERS)
                .executes(WindowXCCommand::setLayer);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> palette() {
        return Commands.literal("palette").then(paletteIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> paletteIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(paletteNameNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> paletteNameNode() {
        return Commands.argument("name", StringArgumentType.word())
                .suggests(SUGGEST_PALETTES)
                .executes(WindowXCCommand::setPalette);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> list() {
        return Commands.literal("list").executes(WindowXCCommand::list);
    }

    // 开启后按键与点击会回传到 storage，供数据包做菜单
    private static LiteralArgumentBuilder<CommandSourceStack> events() {
        return Commands.literal("events").then(eventsIdNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> eventsIdNode() {
        return Commands.argument("id", ConsoleIdArgumentType.consoleId()).then(eventsStateNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> eventsStateNode() {
        return Commands.argument("enabled", BoolArgumentType.bool())
                .executes(WindowXCCommand::setEvents);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getConsole() {
        return Commands.literal("get")
                .then(Commands.argument("id", ConsoleIdArgumentType.consoleId())
                        .executes(WindowXCCommand::getConsole));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dump() {
        return Commands.literal("dump")
                .then(Commands.argument("id", ConsoleIdArgumentType.consoleId())
                        .executes(WindowXCCommand::dump));
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
                .executes(context -> read(context, false, false, ""))
                .then(readFocusNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> readFocusNode() {
        return Commands.argument("focus", BoolArgumentType.bool())
                .executes(context -> read(context, BoolArgumentType.getBool(context, "focus"), false, ""))
                .then(readLiveNode());
    }

    // live 为 true 时输入缓冲每次变化都写入计分板，不必等回车
    private static ArgumentBuilder<CommandSourceStack, ?> readLiveNode() {
        return Commands.argument("live", BoolArgumentType.bool())
                .executes(context -> read(
                        context,
                        BoolArgumentType.getBool(context, "focus"),
                        BoolArgumentType.getBool(context, "live"),
                        ""
                ))
                .then(readPromptNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> readPromptNode() {
        return Commands.argument("prompt", StringArgumentType.greedyString())
                .executes(context -> read(
                        context,
                        BoolArgumentType.getBool(context, "focus"),
                        BoolArgumentType.getBool(context, "live"),
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

    private static int setFontSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int size = IntegerArgumentType.getInteger(context, "size");
        return broadcast(context, player -> new ConsolePayloads.SetFont(id, size, ""));
    }

    private static int setFontFamily(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String name = StringArgumentType.getString(context, "name");
        // size 传 0 表示只换字体族
        return broadcast(context, player -> new ConsolePayloads.SetFont(id, 0, name));
    }

    private static int setOpacity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        float value = (float) DoubleArgumentType.getDouble(context, "value");
        return broadcast(context, player -> new ConsolePayloads.SetOpacity(id, value));
    }

    private static int setCursor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        boolean visible = BoolArgumentType.getBool(context, "visible");
        return broadcast(context, player -> new ConsolePayloads.SetCursor(id, visible));
    }

    private static int print(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String text = StringArgumentType.getString(context, "text");
        return broadcast(context, player -> new ConsolePayloads.Print(id, text));
    }

    private static int clear(CommandContext<CommandSourceStack> context, int mode) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.Clear(id, mode));
    }

    private static int printAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        String text = StringArgumentType.getString(context, "text");
        return broadcast(context, player -> new ConsolePayloads.PrintAt(id, x, y, text));
    }

    private static int scroll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int lines = IntegerArgumentType.getInteger(context, "lines");
        return broadcast(context, player -> new ConsolePayloads.Scroll(id, lines));
    }

    private static int setRegion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int top = IntegerArgumentType.getInteger(context, "top");
        int bottom = IntegerArgumentType.getInteger(context, "bottom");
        return broadcast(context, player -> new ConsolePayloads.SetRegion(id, top, bottom));
    }

    private static int setSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int height = IntegerArgumentType.getInteger(context, "height");
        int width = IntegerArgumentType.getInteger(context, "width");
        return broadcast(context, player -> new ConsolePayloads.SetSize(id, height, width));
    }

    private static int move(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        return broadcast(context, player -> new ConsolePayloads.Move(id, x, y));
    }

    private static int setLayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String layer = StringArgumentType.getString(context, "layer").toLowerCase(Locale.ROOT);
        if (!LAYERS.contains(layer)) {
            throw ERROR_UNKNOWN_LAYER.create(layer);
        }
        return broadcast(context, player -> new ConsolePayloads.SetLayer(id, layer));
    }

    private static int setPalette(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String name = StringArgumentType.getString(context, "name");
        TerminalPalette palette = TerminalPalette.byName(name);
        if (palette == null) {
            throw ERROR_UNKNOWN_PALETTE.create(name);
        }
        return broadcast(context, player -> new ConsolePayloads.SetPalette(id, palette.name()));
    }

    private static int setEvents(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        return broadcast(context, player -> new ConsolePayloads.SetEvents(id, enabled));
    }

    // 查询由客户端收集后回包，服务端只负责发请求
    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return broadcast(context, player -> new ConsolePayloads.Query("", 0));
    }

    private static int getConsole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.Query(id, 1));
    }

    private static int dump(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.Query(id, 2));
    }

    private static int cancelInput(
            CommandContext<CommandSourceStack> context,
            String message
    ) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        return broadcast(context, player -> new ConsolePayloads.InputCancel(id, message));
    }

    // submit 为 true 时填完直接提交，等价于玩家敲了回车
    private static int injectInput(
            CommandContext<CommandSourceStack> context,
            boolean submit
    ) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        String text = StringArgumentType.getString(context, "text");
        return broadcast(context, player -> new ConsolePayloads.InputInject(id, text, submit));
    }

    private static int setMask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = context.getArgument("id", String.class);
        boolean masked = BoolArgumentType.getBool(context, "masked");
        return broadcast(context, player -> new ConsolePayloads.SetMask(id, masked));
    }

    private static int read(
            CommandContext<CommandSourceStack> context,
            boolean focus,
            boolean live,
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
        return broadcast(context, player -> new ConsolePayloads.InputRequest(
                id, prompt, objectiveName, focus, live
        ));
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
