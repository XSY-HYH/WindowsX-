package com.xutrain.windowxpp.console;

import com.xutrain.windowx.client.multiwindow.CustomWindow;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;

// 接管控制台窗口的输入：键盘用于输入，鼠标用于选中与复制
public final class ConsoleInput {
    private ConsoleInput() {
    }

    public static void install(CustomWindow window, ConsoleSession session) {
        long handle = window.getHandle();
        if (handle == 0L) {
            return;
        }
        installKeyboard(handle, session);
        installMouse(handle, window, session);
        // 标记了 noclose 的窗口忽略关闭请求，系统标题栏的关闭键无法置灰，只能拦下并提示
        if (session.flags().contains("noclose")) {
            glfwSetWindowCloseCallback(handle, win -> session.notifyCloseBlocked());
        }
    }

    private static void installKeyboard(long handle, ConsoleSession session) {
        glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) {
                return;
            }
            // UI 输入框有焦点时键盘只归它，终端输入与事件回传都让位
            if (session.uiInputFocused()) {
                if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                    session.commitUiInput();
                } else if (key == GLFW_KEY_BACKSPACE) {
                    session.eraseUiCharacter();
                } else if (key == GLFW_KEY_ESCAPE) {
                    session.blurUiInput();
                } else if (key == GLFW_KEY_V && (mods & GLFW_MOD_CONTROL) != 0) {
                    session.pasteUi(glfwGetClipboardString(handle));
                }
                return;
            }
            // 等待文本输入时键盘归输入框，否则全部作为事件回传
            if (session.inputPending()) {
                if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                    session.commitInput();
                } else if (key == GLFW_KEY_BACKSPACE) {
                    session.eraseCharacter();
                } else if (key == GLFW_KEY_ESCAPE) {
                    session.cancelInput();
                } else if (key == GLFW_KEY_V && (mods & GLFW_MOD_CONTROL) != 0) {
                    session.paste(glfwGetClipboardString(handle));
                }
                return;
            }
            if (key == GLFW_KEY_ESCAPE) {
                session.clearSelection();
            }
            // 长按不重复上报，避免一次按住刷出一串事件
            if (action == GLFW_PRESS) {
                session.publishKey(key, mods);
            }
        });
        glfwSetCharCallback(handle, (win, codepoint) -> {
            if (session.uiInputFocused()) {
                if (codepoint >= 0x20 && codepoint < 0x10000) {
                    session.typeUiCharacter((char) codepoint);
                }
                return;
            }
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
    }

    private static void installMouse(long handle, CustomWindow window, ConsoleSession session) {
        glfwSetMouseButtonCallback(handle, (win, button, action, mods) -> {
            double[] position = cursorPosition(handle, window);
            // 先做 UI 命中，点中按钮或输入框就不再走终端选中与粘贴，label 只是展示不拦截
            if (action == GLFW_PRESS) {
                UiElement element = session.uiAt(position[0], position[1]);
                if (element != null) {
                    if ("button".equals(element.type())) {
                        session.blurUiInput();
                        session.pressUi(element.id());
                        return;
                    }
                    if ("input".equals(element.type())) {
                        session.focusUiInput(element.id());
                        return;
                    }
                }
                // 点到 UI 之外或 label 上时把键盘还给终端，否则输入框只能靠 ESC 退出
                session.blurUiInput();
                // 点击事件与选中互不影响，数据包没开启时这里什么都不做
                session.publishClick(
                        session.cellColumn(position[0]),
                        session.cellRow(position[1]),
                        button
                );
            }
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    session.beginSelection(
                            session.cellColumn(position[0]),
                            session.cellRow(position[1])
                    );
                } else if (action == GLFW_RELEASE) {
                    session.releaseUi();
                    session.endSelection();
                }
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
                if (session.hasSelection()) {
                    String text = session.selectionText();
                    if (!text.isEmpty()) {
                        glfwSetClipboardString(handle, text);
                        session.clearSelection();
                    }
                } else {
                    session.paste(glfwGetClipboardString(handle));
                }
            }
        });
        glfwSetCursorPosCallback(handle, (win, x, y) -> {
            if (!session.selecting()) {
                return;
            }
            double[] position = toPixel(handle, window, x, y);
            session.updateSelection(
                    session.cellColumn(position[0]),
                    session.cellRow(position[1])
            );
        });
    }

    private static double[] cursorPosition(long handle, CustomWindow window) {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(handle, x, y);
        return toPixel(handle, window, x[0], y[0]);
    }

    // 光标坐标是窗口逻辑坐标，字符格按像素算，需要按缩放比例换算
    private static double[] toPixel(long handle, CustomWindow window, double x, double y) {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(handle, width, height);
        double scaleX = width[0] <= 0 ? 1.0 : (double) window.getWidth() / width[0];
        double scaleY = height[0] <= 0 ? 1.0 : (double) window.getHeight() / height[0];
        return new double[]{x * scaleX, y * scaleY};
    }
}
