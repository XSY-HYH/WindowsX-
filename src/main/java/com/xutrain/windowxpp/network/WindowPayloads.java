package com.xutrain.windowxpp.network;

import com.xutrain.windowxpp.WindowXPP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 窗口几何相关的网络包
public final class WindowPayloads {
    private WindowPayloads() {
    }

    public record Position(int x, int y, int width, int height) implements CustomPacketPayload {
        public static final Type<Position> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(WindowXPP.MOD_ID, "window_position")
        );
        public static final StreamCodec<FriendlyByteBuf, Position> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.x());
                    buffer.writeVarInt(payload.y());
                    buffer.writeVarInt(payload.width());
                    buffer.writeVarInt(payload.height());
                },
                buffer -> new Position(
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt()
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
