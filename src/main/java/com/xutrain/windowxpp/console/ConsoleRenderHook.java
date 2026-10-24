package com.xutrain.windowxpp.console;

import com.xutrain.windowxpp.WindowXPP;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// MC 渲染线程上把脏的控制台光栅化，GL 上传由窗口线程完成
@EventBusSubscriber(modid = WindowXPP.MOD_ID, value = Dist.CLIENT)
public final class ConsoleRenderHook {
    private ConsoleRenderHook() {
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ConsoleManager manager = ConsoleManager.getInstance();
        // 玩家点关闭按钮后窗口由 wx 销毁，这里同步注销会话
        manager.sweepClosed();
        for (ConsoleSession session : manager.all()) {
            session.resizeIfNeeded();
            ConsoleTextures.prepare(session);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConsoleManager.getInstance().closeAll();
    }
}
