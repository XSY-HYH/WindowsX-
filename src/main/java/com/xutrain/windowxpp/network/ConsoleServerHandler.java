package com.xutrain.windowxpp.network;

import com.xutrain.windowxpp.WindowXPP;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 服务端接收控制台输入
public final class ConsoleServerHandler {
    private ConsoleServerHandler() {
    }

    public static void handleInputResult(ConsolePayloads.InputResult payload, IPayloadContext context) {
        context.enqueueWork(() -> WindowXPP.LOGGER.debug(
                "[WindowsX++] input from {} on {}: {}",
                context.player().getName().getString(),
                payload.consoleId(),
                payload.text()
        ));
    }
}
