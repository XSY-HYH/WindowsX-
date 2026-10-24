package com.xutrain.windowxpp.console;

import java.nio.ByteBuffer;

// 把终端像素交给窗口，由窗口自己的渲染线程完成上传
public final class ConsoleTextures {
    private ConsoleTextures() {
    }

    public static void prepare(ConsoleSession session) {
        if (!session.isDirty()) {
            return;
        }
        TerminalRenderer renderer = session.renderer();
        TerminalBuffer buffer = session.buffer();
        int width = renderer.pixelWidth(buffer.cols());
        int height = renderer.pixelHeight(buffer.rows());
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean showCursor = session.inputPending() && buffer.cursorVisible();
        int[] pixels = renderer.render(
                buffer, showCursor, session.background(), session.palette(), session.selection()
        );
        ByteBuffer data = session.nextPixelBuffer(width, height);
        // OpenGL 纹理原点在左下，图片原点在左上，逐行翻转
        for (int y = height - 1; y >= 0; y--) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int argb = pixels[row + x];
                data.put((byte) (argb >> 16));
                data.put((byte) (argb >> 8));
                data.put((byte) argb);
                data.put((byte) (argb >> 24));
            }
        }
        data.flip();

        session.window().setOverlayPixels(data, width, height, 1, 1, "STRETCH");
        session.markClean();
    }
}
