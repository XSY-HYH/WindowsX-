package com.xutrain.windowxpp.console;

import com.xutrain.windowx.client.multiwindow.CustomWindow;
import com.xutrain.windowxpp.network.ConsolePayloads;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;

// 接管控制台窗口的键盘输入，只保留控制台自己需要的按键
public final class ConsoleInput {
    private ConsoleInput() {
    }

    public static void install(CustomWindow window, ConsoleSession session) {
        long handle = window.getHandle();
        if (handle == 0L) {
            return;
        }
        glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) {
                return;
            }
            if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                String objective = session.inputObjective();
                String value = session.submitInput();
                if (value == null) {
                    return;
                }
                String trimmed = value.trim();
                try {
                    Integer.parseInt(trimmed);
                    PacketDistributor.sendToServer(
                            new ConsolePayloads.InputResult(session.id(), trimmed, objective)
                    );
                } catch (NumberFormatException e) {
                    session.print("\u001b[31m需要输入整数\u001b[0m\n");
                    session.beginInput(objective);
                }
            } else if (key == GLFW_KEY_BACKSPACE) {
                session.eraseCharacter();
            } else if (key == GLFW_KEY_ESCAPE) {
                session.cancelInput();
            }
        });
        glfwSetCharCallback(handle, (win, codepoint) -> {
            if (codepoint >= 0x20 && codepoint < 0x10000) {
                session.typeCharacter((char) codepoint);
            }
        });
        // Ctrl+滚轮调字号，Ctrl+Shift+滚轮调不透明度，与 conhost 行为一致
        glfwSetScrollCallback(handle, (win, xOffset, yOffset) -> {
            boolean control = glfwGetKey(handle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS
                    || glfwGetKey(handle, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS;
            if (!control || yOffset == 0.0) {
                return;
            }
            boolean shift = glfwGetKey(handle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
                    || glfwGetKey(handle, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
            if (shift) {
                // conhost 每格调整 12/255
                session.adjustOpacity(yOffset > 0.0 ? 12.0F / 255.0F : -12.0F / 255.0F);
            } else {
                session.adjustFontSize(yOffset > 0.0 ? 1 : -1);
            }
        });
        // 标记了 noclose 的窗口忽略关闭请求
        if (session.flags().contains("noclose")) {
            glfwSetWindowCloseCallback(handle, win -> {
            });
        }
    }
}
