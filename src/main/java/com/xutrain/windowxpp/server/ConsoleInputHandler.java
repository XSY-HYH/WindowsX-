package com.xutrain.windowxpp.server;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.network.ConsolePayloads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 把控制台输入写入指定计分板，并保留文本副本供命令读取
public final class ConsoleInputHandler {
    private static final ResourceLocation STORAGE_ID =
            ResourceLocation.fromNamespaceAndPath(WindowXPP.MOD_ID, "consoles");

    private ConsoleInputHandler() {
    }

    public static void handle(ConsolePayloads.InputResult payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String consoleId = payload.consoleId();
            String text = payload.text();

            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            writeStorage(server, consoleId, text);
            writeObjective(server, player, payload.objective(), text);
        });
    }

    // 取消时落一条空记录，数据包靠 aborted 标记区分「没回答」和「还在等」
    public static void handleAbort(ConsolePayloads.InputAborted payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            CommandStorage storage = server.getCommandStorage();
            CompoundTag root = storage.get(STORAGE_ID).copy();
            CompoundTag entry = new CompoundTag();
            entry.putString("input", "");
            entry.putInt("length", 0);
            entry.putBoolean("aborted", true);
            root.put(payload.consoleId(), entry);
            storage.set(STORAGE_ID, root);
        });
    }

    private static void writeStorage(MinecraftServer server, String consoleId, String text) {
        CommandStorage storage = server.getCommandStorage();
        CompoundTag root = storage.get(STORAGE_ID).copy();
        CompoundTag entry = new CompoundTag();
        entry.putString("input", text);
        entry.putInt("length", text.length());
        root.put(consoleId, entry);
        storage.set(STORAGE_ID, root);
    }

    // 覆盖写入目标计分板，类型不符则忽略
    private static void writeObjective(
            MinecraftServer server,
            ServerPlayer player,
            String objectiveName,
            String text
    ) {
        if (objectiveName == null || objectiveName.isEmpty()) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return;
        }
        if (objective.getCriteria().getDefaultRenderType() != ObjectiveCriteria.RenderType.INTEGER) {
            return;
        }
        // 第三个参数是强制可写标记，传 true 才能覆盖只读准则
        scoreboard.getOrCreatePlayerScore(player, objective, true).set(value);
    }
}
