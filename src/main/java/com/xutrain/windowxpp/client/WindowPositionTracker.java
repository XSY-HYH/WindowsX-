package com.xutrain.windowxpp.client;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.network.WindowPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// 轮询主窗口几何，变化时同步给服务端
@EventBusSubscriber(modid = WindowXPP.MOD_ID, value = Dist.CLIENT)
public final class WindowPositionTracker {
    private static int lastX = Integer.MIN_VALUE;
    private static int lastY = Integer.MIN_VALUE;
    private static int lastWidth = Integer.MIN_VALUE;
    private static int lastHeight = Integer.MIN_VALUE;
    private static int tickCounter;

    private WindowPositionTracker() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        int x = minecraft.getWindow().getX();
        int y = minecraft.getWindow().getY();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        // 定期重发，避免首次上报丢失后窗口不动就再也不同步
        boolean resend = ++tickCounter % 100 == 0;
        if (!resend && x == lastX && y == lastY && width == lastWidth && height == lastHeight) {
            return;
        }
        lastX = x;
        lastY = y;
        lastWidth = width;
        lastHeight = height;
        PacketDistributor.sendToServer(new WindowPayloads.Position(x, y, width, height));
    }
}
