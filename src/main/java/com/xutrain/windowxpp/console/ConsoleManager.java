package com.xutrain.windowxpp.console;

import com.xutrain.windowx.WindowXConfig;
import com.xutrain.windowx.client.multiwindow.CustomWindow;
import com.xutrain.windowx.client.multiwindow.MultiWindowManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 控制台窗口的统一生命周期管理：创建、主动关闭、被动关闭、世界退出
public final class ConsoleManager {
    private static final ConsoleManager INSTANCE = new ConsoleManager();

    private final Map<UUID, Map<String, ConsoleSession>> sessions = new HashMap<>();

    private ConsoleManager() {
    }

    public static ConsoleManager getInstance() {
        return INSTANCE;
    }

    public ConsoleSession create(
            UUID owner,
            String id,
            String title,
            int x,
            int y,
            int width,
            int height,
            String colorName,
            int fontSize,
            String flags,
            int background
    ) {
        // 窗口 id 与 wx 共用同一命名空间，已存在同名窗口时不再创建
        if (MultiWindowManager.getInstance().getWindow(owner.toString(), id) != null) {
            return null;
        }

        MultiWindowManager manager = MultiWindowManager.getInstance();
        // 控制台窗口走独立上下文，像素交给窗口线程上传，避开跨上下文共享纹理
        boolean previousSharing = WindowXConfig.disableContextSharing;
        WindowXConfig.disableContextSharing = true;
        try {
            manager.addWindow(owner.toString(), id, title, x, y, width, height, colorName);
        } finally {
            WindowXConfig.disableContextSharing = previousSharing;
        }
        CustomWindow window = manager.getWindow(owner.toString(), id);
        if (window == null) {
            return null;
        }
        ConsoleSession session = new ConsoleSession(owner, id, window, fontSize, flags, background);
        sessions.computeIfAbsent(owner, key -> new HashMap<>()).put(id, session);
        applySizeFlags(window, flags);
        ConsoleInput.install(window, session);
        return session;
    }

    public ConsoleSession get(UUID owner, String id) {
        Map<String, ConsoleSession> owned = sessions.get(owner);
        return owned == null ? null : owned.get(id);
    }

    public boolean exists(UUID owner, String id) {
        return get(owner, id) != null;
    }

    public List<ConsoleSession> all() {
        List<ConsoleSession> result = new ArrayList<>();
        for (Map<String, ConsoleSession> owned : sessions.values()) {
            result.addAll(owned.values());
        }
        return result;
    }

    // 主动关闭：注销会话并让 wx 销毁窗口
    public void close(UUID owner, String id) {
        ConsoleSession session = get(owner, id);
        if (session == null) {
            return;
        }
        detach(session);
        MultiWindowManager.getInstance().removeWindow(owner.toString(), id);
    }

    // 被动关闭：玩家点了窗口的关闭按钮，窗口由 wx 销毁，这里只注销引用
    public void sweepClosed() {
        List<ConsoleSession> closed = new ArrayList<>();
        for (ConsoleSession session : all()) {
            if (session.window().shouldClose()) {
                closed.add(session);
            }
        }
        for (ConsoleSession session : closed) {
            detach(session);
        }
    }

    // 世界退出：全部关闭
    public void closeAll() {
        for (ConsoleSession session : all()) {
            detach(session);
            MultiWindowManager.getInstance().removeWindow(session.owner().toString(), session.id());
        }
        sessions.clear();
    }

    // 禁止缩小或放大通过设置尺寸上下限实现
    private void applySizeFlags(CustomWindow window, String flags) {
        long handle = window.getHandle();
        if (handle == 0L) {
            return;
        }
        boolean noShrink = flags.contains("noshrink");
        boolean noGrow = flags.contains("nogrow");
        if (!noShrink && !noGrow) {
            return;
        }
        int currentWidth = window.getWidth();
        int currentHeight = window.getHeight();
        GLFW.glfwSetWindowSizeLimits(
                handle,
                noShrink ? currentWidth : GLFW.GLFW_DONT_CARE,
                noShrink ? currentHeight : GLFW.GLFW_DONT_CARE,
                noGrow ? currentWidth : GLFW.GLFW_DONT_CARE,
                noGrow ? currentHeight : GLFW.GLFW_DONT_CARE
        );
    }

    private void detach(ConsoleSession session) {
        Map<String, ConsoleSession> owned = sessions.get(session.owner());
        if (owned != null) {
            owned.remove(session.id());
            if (owned.isEmpty()) {
                sessions.remove(session.owner());
            }
        }
    }
}
