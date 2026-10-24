package com.xutrain.windowxpp.mixin;

import com.xutrain.windowx.WindowX;
import com.xutrain.windowx.client.multiwindow.MultiWindowManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 禁止 wx 用同名覆盖已存在的窗口，GUI 与控制台共用同一命名空间
@Mixin(MultiWindowManager.class)
public class MultiWindowManagerMixin {
    @Inject(
            method = "addWindow(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIILjava/lang/String;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void windowxpp$rejectDuplicate(
            String playerUUID,
            String id,
            String title,
            int x,
            int y,
            int width,
            int height,
            String colorName,
            boolean borderless,
            CallbackInfo info
    ) {
        MultiWindowManager manager = (MultiWindowManager) (Object) this;
        if (manager.getWindow(playerUUID, id) != null) {
            WindowX.LOGGER.warn("[WindowsX++] 已存在同名窗口，拒绝创建: {}", id);
            info.cancel();
        }
    }
}
