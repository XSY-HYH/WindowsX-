package com.xutrain.windowxpp.server;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.network.UiPayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

// 处理客户端回传的点击与输入，按元素绑定写值后把当前值发回去
@EventBusSubscriber(modid = WindowXPP.MOD_ID)
public final class UiEventHandler {
    private UiEventHandler() {
    }

    public static void handle(UiPayloads.Event payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            UiRegistry.Element element = UiRegistry.get(
                    player.getUUID(),
                    payload.consoleId(),
                    payload.elementId()
            );
            if (element == null || element.bind().source() == UiBind.Source.NONE) {
                return;
            }
            UiBind bind = element.bind();
            boolean button = "button".equals(element.type());
            boolean input = "input".equals(element.type());
            switch (payload.action()) {
                case UiPayloads.ACTION_PRESS -> {
                    if (button) {
                        writeButton(player, bind, true);
                    }
                }
                case UiPayloads.ACTION_RELEASE -> {
                    if (button) {
                        writeButton(player, bind, false);
                    }
                }
                case UiPayloads.ACTION_LIVE, UiPayloads.ACTION_SUBMIT -> {
                    if (input) {
                        writeInput(player, bind, payload.text());
                    }
                }
                default -> {
                }
            }
            String value = currentValue(player, bind);
            PacketDistributor.sendToPlayer(
                    player,
                    new UiPayloads.Value(payload.consoleId(), payload.elementId(), value)
            );
        });
    }

    // 按下写 1、松开写 0；toggle 模式在按下时翻转
    private static void writeButton(ServerPlayer player, UiBind bind, boolean pressed) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (bind.source() == UiBind.Source.OBJECTIVE) {
            // toggle 只在按下时翻转，松开写 0 会把刚翻上去的值盖回去
            if (bind.mode() == UiBind.Mode.TOGGLE) {
                if (pressed) {
                    writeObjective(player, bind.name(), readObjective(player, bind.name()) == 0 ? 1 : 0);
                }
                return;
            }
            writeObjective(player, bind.name(), pressed ? 1 : 0);
            return;
        }
        UiVariables.Entry entry = UiVariables.get(server, player.getUUID(), bind.name());
        if (entry == null) {
            return;
        }
        if (bind.mode() == UiBind.Mode.TOGGLE) {
            if (pressed) {
                if (entry.type() == UiVariables.Type.BOOL) {
                    UiVariables.toggle(server, player.getUUID(), bind.name());
                } else if (entry.type() == UiVariables.Type.INT) {
                    boolean on = "1".equals(entry.value());
                    UiVariables.set(server, player.getUUID(), bind.name(), on ? "0" : "1");
                }
            }
            return;
        }
        UiVariables.set(server, player.getUUID(), bind.name(), pressed ? "1" : "0");
    }

    private static void writeInput(ServerPlayer player, UiBind bind, String text) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (bind.source() == UiBind.Source.OBJECTIVE) {
            try {
                writeObjective(player, bind.name(), Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        UiVariables.set(server, player.getUUID(), bind.name(), text);
    }

    // 命令定义元素后也要立刻拿一次当前值回填到客户端
    public static String currentValue(ServerPlayer player, UiBind bind) {
        if (bind.source() == UiBind.Source.OBJECTIVE) {
            return String.valueOf(readObjective(player, bind.name()));
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return "";
        }
        UiVariables.Entry entry = UiVariables.get(server, player.getUUID(), bind.name());
        return entry == null ? "" : entry.value();
    }

    private static int readObjective(ServerPlayer player, String objectiveName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null
                || objective.getCriteria().getDefaultRenderType() != ObjectiveCriteria.RenderType.INTEGER) {
            return 0;
        }
        ScoreAccess access = scoreboard.getOrCreatePlayerScore(player, objective, true);
        return access.get();
    }

    private static void writeObjective(ServerPlayer player, String objectiveName, int value) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null
                || objective.getCriteria().getDefaultRenderType() != ObjectiveCriteria.RenderType.INTEGER) {
            return;
        }
        scoreboard.getOrCreatePlayerScore(player, objective, true).set(value);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID owner = player.getUUID();
            UiRegistry.removeOwner(owner);
        }
    }
}
