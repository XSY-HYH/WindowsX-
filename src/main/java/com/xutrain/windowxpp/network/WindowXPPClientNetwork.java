package com.xutrain.windowxpp.network;

import com.xutrain.windowxpp.WindowXPP;
import com.xutrain.windowxpp.console.ConsoleClientHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// 客户端专用包注册，避免服务端加载客户端处理类
@EventBusSubscriber(modid = WindowXPP.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WindowXPPClientNetwork {
    private WindowXPPClientNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ConsolePayloads.Create.TYPE,
                ConsolePayloads.Create.STREAM_CODEC,
                ConsoleClientHandler::handleCreate
        );
        registrar.playToClient(
                ConsolePayloads.Print.TYPE,
                ConsolePayloads.Print.STREAM_CODEC,
                ConsoleClientHandler::handlePrint
        );
        registrar.playToClient(
                ConsolePayloads.Clear.TYPE,
                ConsolePayloads.Clear.STREAM_CODEC,
                ConsoleClientHandler::handleClear
        );
        registrar.playToClient(
                ConsolePayloads.Scroll.TYPE,
                ConsolePayloads.Scroll.STREAM_CODEC,
                ConsoleClientHandler::handleScroll
        );
        registrar.playToClient(
                ConsolePayloads.SetRegion.TYPE,
                ConsolePayloads.SetRegion.STREAM_CODEC,
                ConsoleClientHandler::handleSetRegion
        );
        registrar.playToClient(
                ConsolePayloads.PrintAt.TYPE,
                ConsolePayloads.PrintAt.STREAM_CODEC,
                ConsoleClientHandler::handlePrintAt
        );
        registrar.playToClient(
                ConsolePayloads.SetSize.TYPE,
                ConsolePayloads.SetSize.STREAM_CODEC,
                ConsoleClientHandler::handleSetSize
        );
        registrar.playToClient(
                ConsolePayloads.Move.TYPE,
                ConsolePayloads.Move.STREAM_CODEC,
                ConsoleClientHandler::handleMove
        );
        registrar.playToClient(
                ConsolePayloads.Remove.TYPE,
                ConsolePayloads.Remove.STREAM_CODEC,
                ConsoleClientHandler::handleRemove
        );
        registrar.playToClient(
                ConsolePayloads.SetTitle.TYPE,
                ConsolePayloads.SetTitle.STREAM_CODEC,
                ConsoleClientHandler::handleSetTitle
        );
        registrar.playToClient(
                ConsolePayloads.Top.TYPE,
                ConsolePayloads.Top.STREAM_CODEC,
                ConsoleClientHandler::handleTop
        );
        registrar.playToClient(
                ConsolePayloads.SetBackground.TYPE,
                ConsolePayloads.SetBackground.STREAM_CODEC,
                ConsoleClientHandler::handleSetBackground
        );
        registrar.playToClient(
                ConsolePayloads.SetFont.TYPE,
                ConsolePayloads.SetFont.STREAM_CODEC,
                ConsoleClientHandler::handleSetFont
        );
        registrar.playToClient(
                ConsolePayloads.SetOpacity.TYPE,
                ConsolePayloads.SetOpacity.STREAM_CODEC,
                ConsoleClientHandler::handleSetOpacity
        );
        registrar.playToClient(
                ConsolePayloads.SetCursor.TYPE,
                ConsolePayloads.SetCursor.STREAM_CODEC,
                ConsoleClientHandler::handleSetCursor
        );
        registrar.playToClient(
                ConsolePayloads.InputRequest.TYPE,
                ConsolePayloads.InputRequest.STREAM_CODEC,
                ConsoleClientHandler::handleInputRequest
        );
        registrar.playToClient(
                ConsolePayloads.InputCancel.TYPE,
                ConsolePayloads.InputCancel.STREAM_CODEC,
                ConsoleClientHandler::handleInputCancel
        );
        registrar.playToClient(
                ConsolePayloads.InputInject.TYPE,
                ConsolePayloads.InputInject.STREAM_CODEC,
                ConsoleClientHandler::handleInputInject
        );
        registrar.playToClient(
                ConsolePayloads.SetMask.TYPE,
                ConsolePayloads.SetMask.STREAM_CODEC,
                ConsoleClientHandler::handleSetMask
        );
        registrar.playToClient(
                ConsolePayloads.SetEvents.TYPE,
                ConsolePayloads.SetEvents.STREAM_CODEC,
                ConsoleClientHandler::handleSetEvents
        );
        registrar.playToClient(
                ConsolePayloads.SetLayer.TYPE,
                ConsolePayloads.SetLayer.STREAM_CODEC,
                ConsoleClientHandler::handleSetLayer
        );
        registrar.playToClient(
                ConsolePayloads.SetPalette.TYPE,
                ConsolePayloads.SetPalette.STREAM_CODEC,
                ConsoleClientHandler::handleSetPalette
        );
        registrar.playToClient(
                ConsolePayloads.Query.TYPE,
                ConsolePayloads.Query.STREAM_CODEC,
                ConsoleClientHandler::handleQuery
        );
    }
}
