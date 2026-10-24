package com.xutrain.windowxpp.console;

import com.xutrain.windowxpp.network.ConsolePayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

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
                // 不再自动换行，换行只由文本里的 \n 决定
                session.print(unescape(payload.text()));
            }
        });
    }

    // 命令里打不出 ESC，用 \e 与 \n \r 转义；greedyString 会把成对引号带进来
    private static String unescape(String text) {
        String value = text;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value
                .replace("\\e", "\u001b")
                .replace("\\n", "\n")
                .replace("\\r", "\r");
    }

    public static void handleClear(ConsolePayloads.Clear payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.clear(payload.mode());
            }
        });
    }

    public static void handleScroll(ConsolePayloads.Scroll payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.scroll(payload.lines());
            }
        });
    }

    public static void handleSetRegion(ConsolePayloads.SetRegion payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setRegion(payload.top(), payload.bottom());
            }
        });
    }

    public static void handlePrintAt(ConsolePayloads.PrintAt payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.printAt(payload.x(), payload.y(), unescape(payload.text()));
            }
        });
    }

    public static void handleSetSize(ConsolePayloads.SetSize payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.resizeWindow(payload.height(), payload.width());
            }
        });
    }

    public static void handleMove(ConsolePayloads.Move payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.move(payload.x(), payload.y());
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

    public static void handleSetFont(ConsolePayloads.SetFont payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session == null) {
                return;
            }
            // size 为 0 表示只换字体族，不动字号
            if (payload.size() > 0) {
                session.setFontSize(payload.size());
            }
            if (!payload.family().isEmpty()) {
                session.setFontFamily(payload.family());
            }
        });
    }

    public static void handleSetOpacity(ConsolePayloads.SetOpacity payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setOpacity(payload.value());
            }
        });
    }

    public static void handleSetCursor(ConsolePayloads.SetCursor payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setCursorVisible(payload.visible());
            }
        });
    }

    public static void handleInputCancel(ConsolePayloads.InputCancel payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.cancelInput(unescape(payload.message()));
            }
        });
    }

    public static void handleInputInject(ConsolePayloads.InputInject payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.inject(unescape(payload.text()), payload.submit());
            }
        });
    }

    public static void handleSetMask(ConsolePayloads.SetMask payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setMasked(payload.masked());
            }
        });
    }

    public static void handleSetLayer(ConsolePayloads.SetLayer payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session != null) {
                session.setLayer(payload.layer());
            }
        });
    }

    public static void handleSetPalette(ConsolePayloads.SetPalette payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ConsoleSession session = local(payload.consoleId());
            if (session == null) {
                return;
            }
            TerminalPalette palette = TerminalPalette.byName(payload.name());
            if (palette != null) {
                session.applyPalette(palette);
            }
        });
    }

    // 查询在客户端收集，回包交给服务端打印或写入 storage
    public static void handleQuery(ConsolePayloads.Query payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            UUID owner = minecraft.player.getUUID();
            boolean ok;
            String text;
            switch (payload.kind()) {
                case 1 -> {
                    ConsoleSession session = ConsoleManager.getInstance().get(owner, payload.consoleId());
                    ok = session != null;
                    text = ok ? session.describe() : "";
                }
                case 2 -> {
                    ConsoleSession session = ConsoleManager.getInstance().get(owner, payload.consoleId());
                    ok = session != null;
                    text = ok ? truncate(session.dump()) : "";
                }
                default -> {
                    ok = true;
                    text = listText(owner);
                }
            }
            PacketDistributor.sendToServer(
                    new ConsolePayloads.QueryResult(payload.consoleId(), payload.kind(), ok, text)
            );
        });
    }

    private static String listText(UUID owner) {
        StringBuilder text = new StringBuilder();
        for (ConsoleSession session : ConsoleManager.getInstance().allOf(owner)) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(session.summary());
        }
        return text.toString();
    }

    // 网络包字段上限，超长时截断并注明
    private static String truncate(String text) {
        if (text.length() <= 32000) {
            return text;
        }
        return text.substring(0, 32000) + "\n... 已截断";
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
                session.print(unescape(payload.prompt()));
            }
            session.beginInput(payload.objective(), payload.live());
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
