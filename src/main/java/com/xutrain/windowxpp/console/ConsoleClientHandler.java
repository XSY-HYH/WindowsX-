package com.xutrain.windowxpp.console;

import com.xutrain.windowxpp.network.ConsolePayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

// 客户端接收控制台指令
public final class ConsoleClientHandler {
    private static final int DEFAULT_FONT_SIZE = 14;

    private ConsoleClientHandler() {
    }

    public static void handleCreate(ConsolePayloads.Create payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            ConsoleManager.getInstance().create(
                    minecraft.player.getUUID(),
                    payload.consoleId(),
                    payload.title(),
                    payload.x(),
                    payload.y(),
                    payload.width(),
                    payload.height(),
                    payload.color(),
                    DEFAULT_FONT_SIZE,
                    payload.flags(),
                    payload.background()
            );
        });
    }

    public static void handlePrint(ConsolePayloads.Print payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.print(unescape(payload.text()) + "\n");
            }
        });
    }

    // 命令里打不出 ESC，用 \e 与 \n 转义
    private static String unescape(String text) {
        return text.replace("\\e", "\u001b").replace("\\n", "\n");
    }

    public static void handleClear(ConsolePayloads.Clear payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.clear();
            }
        });
    }

    public static void handleRemove(ConsolePayloads.Remove payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            ConsoleManager.getInstance().close(minecraft.player.getUUID(), payload.consoleId());
        });
    }

    public static void handleSetTitle(ConsolePayloads.SetTitle payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setTitle(payload.title());
            }
        });
    }

    public static void handleTop(ConsolePayloads.Top payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                // 置顶靠系统窗口的 floating 属性实现，不改变层级排序
                session.window().setLayer(payload.top() ? "top" : "normal");
            }
        });
    }

    public static void handleSetBackground(ConsolePayloads.SetBackground payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setBackground(payload.color());
            }
        });
    }

    public static void handleInputRequest(ConsolePayloads.InputRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session == null) {
                return;
            }
            // 上一次输入还没提交时不接受新的请求，避免提示叠在一起
            if (session.inputPending()) {
                session.print("\u001b[33m已在等待输入\u001b[0m\n");
                return;
            }
            if (payload.focus()) {
                GLFW.glfwFocusWindow(session.handle());
            }
            if (!payload.prompt().isEmpty()) {
                session.print(payload.prompt());
            }
            session.beginInput(payload.objective());
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
