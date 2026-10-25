package com.xutrain.windowxpp.console;

import java.nio.ByteBuffer;

// 把终端像素交给窗口，由窗口自己的渲染线程完成上传
public final class ConsoleTextures {
    // 纹理像素总量上限，超过显卡纹理限制会导致上传失败、整屏空白
    private static final long MAX_PIXELS = 12_000_000L;

    private ConsoleTextures() {
    }

    public static void prepare(ConsoleSession session) {
        TerminalRenderer renderer = session.renderer();
        TerminalBuffer buffer = session.buffer();
        // 纹理尺寸直接取窗口像素，和窗口 1:1，避免拉伸插值把字糊掉
        int width = session.window().getWidth();
        int height = session.window().getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (!session.needsRender(width, height)) {
            return;
        }
        long total = (long) width * height;
        if (total > MAX_PIXELS) {
            float scale = (float) Math.sqrt((double) MAX_PIXELS / total);
            width = Math.max(1, Math.round(width * scale));
            height = Math.max(1, Math.round(height * scale));
        }

        boolean showCursor = session.inputPending() && buffer.cursorVisible();
        int[] pixels = renderer.render(
                buffer,
                showCursor,
                session.background(),
                session.palette(),
                session.selection(),
                width,
                height,
                session.uiScene()
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
        session.markRendered(session.window().getWidth(), session.window().getHeight());
    }
}
