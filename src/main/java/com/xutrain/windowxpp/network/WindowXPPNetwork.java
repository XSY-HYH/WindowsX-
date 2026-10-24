package com.xutrain.windowxpp.network;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.server.ConsoleInputHandler;
import com.xutrain.windowxpp.server.WindowPositionHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// 服务端网络包注册
@EventBusSubscriber(modid = WindowXPP.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WindowXPPNetwork {
    private WindowXPPNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ConsolePayloads.InputResult.TYPE,
                ConsolePayloads.InputResult.STREAM_CODEC,
                ConsoleInputHandler::handle
        );
        registrar.playToServer(
                WindowPayloads.Position.TYPE,
                WindowPayloads.Position.STREAM_CODEC,
                WindowPositionHandler::handle
        );
    }
}
