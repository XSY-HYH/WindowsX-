package com.xutrain.windowxpp.server;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.network.WindowPayloads;
import com.xutrain.windowxpp.score.WindowXCriteria;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 把客户端主窗口几何写入所有使用窗口准则的计分板
public final class WindowPositionHandler {
    private WindowPositionHandler() {
    }

    public static void handle(WindowPayloads.Position payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            WindowXPP.LOGGER.debug(
                    "[WindowsX++] 窗口几何 {}x{} @ {},{}",
                    payload.width(), payload.height(), payload.x(), payload.y()
            );

            Scoreboard scoreboard = server.getScoreboard();
            for (Objective objective : scoreboard.getObjectives()) {
                Integer value = valueFor(objective.getCriteria().getName(), payload);
                if (value != null) {
                    // 第三个参数是强制可写标记，只读准则必须传 true 才能更新
                    scoreboard.getOrCreatePlayerScore(player, objective, true).set(value);
                }
            }
        });
    }

    // 按准则名匹配，避免实例身份不一致导致漏写
    private static Integer valueFor(String criteriaName, WindowPayloads.Position payload) {
        if (WindowXCriteria.WINDOW_X.getName().equals(criteriaName)) {
            return payload.x();
        }
        if (WindowXCriteria.WINDOW_Y.getName().equals(criteriaName)) {
            return payload.y();
        }
        if (WindowXCriteria.WINDOW_WIDTH.getName().equals(criteriaName)) {
            return payload.width();
        }
        if (WindowXCriteria.WINDOW_HEIGHT.getName().equals(criteriaName)) {
            return payload.height();
        }
        return null;
    }
}
