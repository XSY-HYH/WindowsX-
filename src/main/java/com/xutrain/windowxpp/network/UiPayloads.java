package com.xutrain.windowxpp.network;

import com.xutrain.windowxpp.WindowXPP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 实验性 UI 元素相关的网络包
public final class UiPayloads {
    // 按钮按下、按钮松开、输入实时变化、输入提交
    public static final int ACTION_PRESS = 0;
    public static final int ACTION_RELEASE = 1;
    public static final int ACTION_LIVE = 2;
    public static final int ACTION_SUBMIT = 3;

    private UiPayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WindowXPP.MOD_ID, path);
    }

    // style 是 k=v;k=v 形式，客户端只负责按它画
    public record Define(
            String consoleId,
            String elementId,
            String elementType,
            int x,
            int y,
            int width,
            int height,
            String label,
            String style,
            String value
    ) implements CustomPacketPayload {
        public static final Type<Define> TYPE = new Type<>(id("ui_define"));
        public static final StreamCodec<FriendlyByteBuf, Define> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.elementId());
                    buffer.writeUtf(payload.elementType());
                    buffer.writeVarInt(payload.x());
                    buffer.writeVarInt(payload.y());
                    buffer.writeVarInt(payload.width());
                    buffer.writeVarInt(payload.height());
                    buffer.writeUtf(payload.label(), 4096);
                    buffer.writeUtf(payload.style(), 8192);
                    buffer.writeUtf(payload.value(), 4096);
                },
                buffer -> new Define(
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(4096),
                        buffer.readUtf(8192),
                        buffer.readUtf(4096)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Remove(String consoleId, String elementId) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = new Type<>(id("ui_remove"));
        public static final StreamCodec<FriendlyByteBuf, Remove> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.elementId());
                },
                buffer -> new Remove(buffer.readUtf(), buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Clear(String consoleId) implements CustomPacketPayload {
        public static final Type<Clear> TYPE = new Type<>(id("ui_clear"));
        public static final StreamCodec<FriendlyByteBuf, Clear> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeUtf(payload.consoleId()),
                buffer -> new Clear(buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 绑定目标变化后回发当前值，按钮用它显示开关态，输入框用它回填文本
    public record Value(String consoleId, String elementId, String value) implements CustomPacketPayload {
        public static final Type<Value> TYPE = new Type<>(id("ui_value"));
        public static final StreamCodec<FriendlyByteBuf, Value> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.elementId());
                    buffer.writeUtf(payload.value(), 4096);
                },
                buffer -> new Value(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(4096))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Event(String consoleId, String elementId, int action, String text)
            implements CustomPacketPayload {
        public static final Type<Event> TYPE = new Type<>(id("ui_event"));
        public static final StreamCodec<FriendlyByteBuf, Event> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.elementId());
                    buffer.writeVarInt(payload.action());
                    buffer.writeUtf(payload.text(), 4096);
                },
                buffer -> new Event(
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readUtf(4096)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
