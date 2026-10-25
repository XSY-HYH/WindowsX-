package com.xutrain.windowxpp.console;

import com.xutrain.windowxpp.network.UiPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 客户端接收 UI 元素定义与取值
public final class UiClientHandler {
    private UiClientHandler() {
    }

    public static void handleDefine(UiPayloads.Define payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.defineUi(payload);
            }
        });
    }

    public static void handleRemove(UiPayloads.Remove payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.removeUi(payload.elementId());
            }
        });
    }

    public static void handleClear(UiPayloads.Clear payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.clearUi();
            }
        });
    }

    public static void handleValue(UiPayloads.Value payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setUiValue(payload.elementId(), payload.value());
            }
        });
    }

    private static ConsoleSession local(String consoleId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        return ConsoleManager.getInstance().get(minecraft.player.getUUID(), consoleId);
    }
}
