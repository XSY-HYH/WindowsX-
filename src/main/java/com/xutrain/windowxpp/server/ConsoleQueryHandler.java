package com.xutrain.windowxpp.server;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.network.ConsolePayloads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.CommandStorage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 查询结果：列表与详情打印到聊天，整屏文本写入 storage
public final class ConsoleQueryHandler {
    private static final ResourceLocation STORAGE_ID =
            ResourceLocation.fromNamespaceAndPath(WindowXPP.MOD_ID, "consoles");

    private ConsoleQueryHandler() {
    }

    public static void handle(ConsolePayloads.QueryResult payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!payload.ok()) {
                player.sendSystemMessage(Component.literal("控制台不存在: " + payload.consoleId()));
                return;
            }
            if (payload.kind() == 2) {
                writeDump(player, payload.consoleId(), payload.text());
                return;
            }
            if (payload.text().isEmpty()) {
                player.sendSystemMessage(Component.literal("没有控制台窗口"));
                return;
            }
            for (String line : payload.text().split("\n", -1)) {
                player.sendSystemMessage(Component.literal(line));
            }
        });
    }

    private static void writeDump(ServerPlayer player, String consoleId, String text) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CommandStorage storage = server.getCommandStorage();
        CompoundTag root = storage.get(STORAGE_ID).copy();
        CompoundTag entry = root.getCompound(consoleId).copy();
        String[] lines = text.isEmpty() ? new String[0] : text.split("\n", -1);
        entry.putString("dump", text);
        entry.putInt("lines", lines.length);
        root.put(consoleId, entry);
        storage.set(STORAGE_ID, root);
        player.sendSystemMessage(Component.literal(
                "已写入 storage windowxpp:consoles " + consoleId + ".dump，共 " + lines.length + " 行"
        ));
    }
}
