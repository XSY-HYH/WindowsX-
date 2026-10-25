package com.xutrain.windowxpp.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.xutrain.windowxpp.network.UiPayloads;
import com.xutrain.windowxpp.server.UiBind;
import com.xutrain.windowxpp.server.UiEventHandler;
import com.xutrain.windowxpp.server.UiRegistry;
import com.xutrain.windowxpp.server.UiVariables;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 实验性 UI 命令，windowxpp / wxppui / wxui 是同一棵树的三个入口
public final class WindowXPPCommand {
    private static final DynamicCommandExceptionType ERROR_NO_ELEMENT =
            new DynamicCommandExceptionType(id -> Component.literal("元素不存在: " + id));
    private static final DynamicCommandExceptionType ERROR_NO_VARIABLE =
            new DynamicCommandExceptionType(name -> Component.literal("变量不存在: " + name));
    private static final DynamicCommandExceptionType ERROR_VARIABLE_TYPE =
            new DynamicCommandExceptionType(name -> Component.literal("变量类型不符: " + name));
    private static final DynamicCommandExceptionType ERROR_VARIABLE_EXISTS =
            new DynamicCommandExceptionType(name -> Component.literal("变量已存在: " + name));
    private static final SimpleCommandExceptionType ERROR_NOT_BOOK =
            new SimpleCommandExceptionType(Component.literal("主手需要拿着书与笔"));

    private WindowXPPCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(tree("windowxpp"));
        event.getDispatcher().register(tree("wxppui"));
        event.getDispatcher().register(tree("wxui"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name) {
        return Commands.literal(name).then(
                Commands.argument("targets", EntityArgument.players())
                        .then(ui())
                        .then(vars())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ui() {
        return Commands.literal("ui").then(
                Commands.argument("console", ConsoleIdArgumentType.consoleId())
                        .then(uiAdd())
                        .then(uiStyle())
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", ConsoleIdArgumentType.consoleId())
                                        .executes(WindowXPPCommand::removeElement)))
                        .then(Commands.literal("clear").executes(WindowXPPCommand::clearElements))
                        .then(Commands.literal("list").executes(WindowXPPCommand::listElements))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> uiAdd() {
        return Commands.literal("add").then(
                Commands.argument("id", ConsoleIdArgumentType.consoleId())
                        .then(Commands.literal("button").then(addGeometry("button", true)))
                        .then(Commands.literal("input").then(addGeometry("input", true)))
                        .then(Commands.literal("label").then(addGeometry("label", false)))
        );
    }

    // 几何参数统一是窗口内像素，绑定节点只在按钮与输入框上出现
    private static ArgumentBuilder<CommandSourceStack, ?> addGeometry(String type, boolean bindable) {
        return Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("width", IntegerArgumentType.integer(1))
                                .then(Commands.argument("height", IntegerArgumentType.integer(1))
                                        .then(addTail(type, bindable)))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addTail(String type, boolean bindable) {
        if (!bindable) {
            return Commands.argument("text", StringArgumentType.greedyString())
                    .executes(context -> addElement(context, type));
        }
        return Commands.argument("bind", UiBindArgumentType.bind())
                .executes(context -> addElement(context, type))
                .then(Commands.argument("label", StringArgumentType.greedyString())
                        .executes(context -> addElement(context, type)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> uiStyle() {
        return Commands.literal("style").then(
                Commands.argument("id", ConsoleIdArgumentType.consoleId())
                        .then(Commands.literal("book").executes(WindowXPPCommand::styleFromBook))
                        .then(Commands.argument("properties", ConsoleOptionsArgumentType.options())
                                .executes(WindowXPPCommand::styleInline))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> vars() {
        return Commands.literal("var")
                .then(Commands.literal("create").then(
                        Commands.argument("name", StringArgumentType.word())
                                .then(Commands.literal("string")
                                        .executes(context -> createVariable(context, UiVariables.Type.STRING)))
                                .then(Commands.literal("bool")
                                        .executes(context -> createVariable(context, UiVariables.Type.BOOL)))
                                .then(Commands.literal("int")
                                        .executes(context -> createVariable(context, UiVariables.Type.INT)))
                ))
                .then(Commands.literal("set").then(
                        Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(WindowXPPCommand::setVariable))
                ))
                .then(Commands.literal("get").then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes(WindowXPPCommand::getVariable)
                ))
                .then(Commands.literal("add").then(
                        Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("delta", IntegerArgumentType.integer())
                                        .executes(WindowXPPCommand::addVariable))
                ))
                .then(Commands.literal("toggle").then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes(WindowXPPCommand::toggleVariable)
                ))
                .then(Commands.literal("remove").then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes(WindowXPPCommand::removeVariable)
                ))
                .then(Commands.literal("list").executes(WindowXPPCommand::listVariables));
    }

    private static int addElement(CommandContext<CommandSourceStack> context, String type)
            throws CommandSyntaxException {
        String consoleId = context.getArgument("console", String.class);
        String elementId = context.getArgument("id", String.class);
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int width = IntegerArgumentType.getInteger(context, "width");
        int height = IntegerArgumentType.getInteger(context, "height");
        String label = stripQuotes(optionalString(context, "button".equals(type) || "input".equals(type)
                ? "label" : "text"));
        UiBind bind = UiBind.NONE;
        if ("button".equals(type) || "input".equals(type)) {
            UiBind.Mode fallback = "input".equals(type) ? UiBind.Mode.LIVE : UiBind.Mode.PRESS;
            bind = UiBind.parse(optionalString(context, "bind"), fallback);
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            UiRegistry.Element element = new UiRegistry.Element(
                    elementId, type, x, y, width, height, label, "", bind
            );
            UiRegistry.define(player.getUUID(), consoleId, element);
            sendDefine(player, consoleId, element);
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已添加 " + type + " " + elementId + " 到 " + consoleId),
                false
        );
        return players.size();
    }

    private static int removeElement(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String consoleId = context.getArgument("console", String.class);
        String elementId = context.getArgument("id", String.class);
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            UiRegistry.remove(player.getUUID(), consoleId, elementId);
            PacketDistributor.sendToPlayer(player, new UiPayloads.Remove(consoleId, elementId));
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已移除元素 " + elementId),
                false
        );
        return players.size();
    }

    private static int clearElements(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String consoleId = context.getArgument("console", String.class);
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            UiRegistry.clear(player.getUUID(), consoleId);
            PacketDistributor.sendToPlayer(player, new UiPayloads.Clear(consoleId));
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已清空 " + consoleId + " 的元素"),
                false
        );
        return players.size();
    }

    private static int listElements(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String consoleId = context.getArgument("console", String.class);
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            List<UiRegistry.Element> elements = List.copyOf(UiRegistry.all(player.getUUID(), consoleId));
            if (elements.isEmpty()) {
                context.getSource().sendSuccess(
                        () -> Component.literal(player.getName().getString() + ": 没有元素"),
                        false
                );
                continue;
            }
            StringBuilder text = new StringBuilder(player.getName().getString()).append(':');
            for (UiRegistry.Element element : elements) {
                text.append('\n').append(element.id())
                        .append(" | ").append(element.type())
                        .append(" | ").append(element.x()).append(' ').append(element.y())
                        .append(" | ").append(element.width()).append('x').append(element.height())
                        .append(" | ").append(bindText(element.bind()))
                        .append(" | ").append(element.label().isEmpty() ? "-" : element.label());
            }
            String message = text.toString();
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        return players.size();
    }

    private static String bindText(UiBind bind) {
        if (bind.source() == UiBind.Source.NONE) {
            return "-";
        }
        String prefix = bind.source() == UiBind.Source.OBJECTIVE ? "#" : "$";
        return prefix + bind.name() + "/" + bind.mode().name().toLowerCase(Locale.ROOT);
    }

    private static int styleFromBook(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String consoleId = context.getArgument("console", String.class);
        String elementId = context.getArgument("id", String.class);
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        int applied = 0;
        for (ServerPlayer player : players) {
            UiRegistry.Element element = UiRegistry.get(player.getUUID(), consoleId, elementId);
            if (element == null) {
                throw ERROR_NO_ELEMENT.create(elementId);
            }
            String style = parseProperties(List.of(readBook(player).split("\\R")));
            if (style.isEmpty()) {
                continue;
            }
            UiRegistry.Element updated = UiRegistry.withStyle(element, style);
            UiRegistry.define(player.getUUID(), consoleId, updated);
            sendDefine(player, consoleId, updated);
            applied++;
        }
        if (applied == 0) {
            context.getSource().sendFailure(Component.literal("书里没有可用的样式行"));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已从书与笔应用样式到 " + elementId),
                false
        );
        return applied;
    }

    private static int styleInline(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String consoleId = context.getArgument("console", String.class);
        String elementId = context.getArgument("id", String.class);
        String properties = context.getArgument("properties", String.class);
        String style = parseProperties(List.of(properties.split(",")));
        if (style.isEmpty()) {
            context.getSource().sendFailure(Component.literal("样式为空，写法是 @key=value,..."));
            return 0;
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            UiRegistry.Element element = UiRegistry.get(player.getUUID(), consoleId, elementId);
            if (element == null) {
                throw ERROR_NO_ELEMENT.create(elementId);
            }
            UiRegistry.Element updated = UiRegistry.withStyle(element, style);
            UiRegistry.define(player.getUUID(), consoleId, updated);
            sendDefine(player, consoleId, updated);
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已应用样式到 " + elementId),
                false
        );
        return players.size();
    }

    // 书与笔的每一页按行拼起来，# 开头是注释
    private static String readBook(ServerPlayer player) throws CommandSyntaxException {
        ItemStack stack = player.getMainHandItem();
        WritableBookContent content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (content == null) {
            throw ERROR_NOT_BOOK.create();
        }
        StringBuilder text = new StringBuilder();
        for (Filterable<String> page : content.pages()) {
            text.append(page.get(false)).append('\n');
        }
        return text.toString();
    }

    // 统一成 k=v;k=v，值里不允许出现分号以免序列化歧义
    private static String parseProperties(Iterable<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = trimmed.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(equals + 1).trim().replace(";", "");
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            values.put(key, value);
        }
        StringBuilder style = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (style.length() > 0) {
                style.append(';');
            }
            style.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return style.toString();
    }

    private static void sendDefine(ServerPlayer player, String consoleId, UiRegistry.Element element) {
        String value = UiEventHandler.currentValue(player, element.bind());
        PacketDistributor.sendToPlayer(
                player,
                new UiPayloads.Define(
                        consoleId,
                        element.id(),
                        element.type(),
                        element.x(),
                        element.y(),
                        element.width(),
                        element.height(),
                        element.label(),
                        element.style(),
                        value
                )
        );
    }

    private static int createVariable(CommandContext<CommandSourceStack> context, UiVariables.Type type)
            throws CommandSyntaxException {
        String name = context.getArgument("name", String.class);
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            if (!UiVariables.create(server, player.getUUID(), name, type)) {
                throw ERROR_VARIABLE_EXISTS.create(name);
            }
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已创建变量 " + name + " (" + type.id() + ")"),
                false
        );
        return players.size();
    }

    private static int setVariable(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String name = context.getArgument("name", String.class);
        String value = stripQuotes(StringArgumentType.getString(context, "value"));
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            if (!UiVariables.set(server, player.getUUID(), name, value)) {
                throw variableError(server, player, name);
            }
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已设置 " + name + " = " + value),
                false
        );
        return players.size();
    }

    private static int getVariable(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String name = context.getArgument("name", String.class);
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            UiVariables.Entry entry = UiVariables.get(server, player.getUUID(), name);
            if (entry == null) {
                throw ERROR_NO_VARIABLE.create(name);
            }
            String message = player.getName().getString() + ": " + entry.value()
                    + " (" + entry.type().id() + ")";
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        return players.size();
    }

    private static int addVariable(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String name = context.getArgument("name", String.class);
        int delta = IntegerArgumentType.getInteger(context, "delta");
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            if (!UiVariables.add(server, player.getUUID(), name, delta)) {
                throw variableError(server, player, name);
            }
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已给 " + name + " 加上 " + delta),
                false
        );
        return players.size();
    }

    private static int toggleVariable(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String name = context.getArgument("name", String.class);
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            if (!UiVariables.toggle(server, player.getUUID(), name)) {
                throw variableError(server, player, name);
            }
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已翻转 " + name),
                false
        );
        return players.size();
    }

    private static int removeVariable(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String name = context.getArgument("name", String.class);
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            if (!UiVariables.remove(server, player.getUUID(), name)) {
                throw ERROR_NO_VARIABLE.create(name);
            }
        }
        context.getSource().sendSuccess(
                () -> Component.literal("已删除变量 " + name),
                false
        );
        return players.size();
    }

    private static int listVariables(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            List<String> variables = UiVariables.list(server, player.getUUID());
            String body = variables.isEmpty() ? "没有变量" : String.join("\n", variables);
            String message = player.getName().getString() + ":\n" + body;
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        return players.size();
    }

    private static CommandSyntaxException variableError(
            MinecraftServer server,
            ServerPlayer player,
            String name
    ) {
        UiVariables.Entry entry = UiVariables.get(server, player.getUUID(), name);
        return entry == null ? ERROR_NO_VARIABLE.create(name) : ERROR_VARIABLE_TYPE.create(name);
    }

    private static String optionalString(CommandContext<CommandSourceStack> context, String name) {
        try {
            return StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String stripQuotes(String text) {
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
