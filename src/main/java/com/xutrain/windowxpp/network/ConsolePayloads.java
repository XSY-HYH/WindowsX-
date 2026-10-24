package com.xutrain.windowxpp.network;

import com.xutrain.windowxpp.WindowXPP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 控制台相关的网络包
public final class ConsolePayloads {
    private ConsolePayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WindowXPP.MOD_ID, path);
    }

    public record Create(
            String consoleId,
            int x,
            int y,
            int width,
            int height,
            String color,
            String title,
            String flags,
            int background
    ) implements CustomPacketPayload {
        public static final Type<Create> TYPE = new Type<>(id("console_create"));
        public static final StreamCodec<FriendlyByteBuf, Create> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.x());
                    buffer.writeVarInt(payload.y());
                    buffer.writeVarInt(payload.width());
                    buffer.writeVarInt(payload.height());
                    buffer.writeUtf(payload.color());
                    buffer.writeUtf(payload.title());
                    buffer.writeUtf(payload.flags());
                    buffer.writeInt(payload.background());
                },
                buffer -> new Create(
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readInt()
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetBackground(String consoleId, int color) implements CustomPacketPayload {
        public static final Type<SetBackground> TYPE = new Type<>(id("console_set_background"));
        public static final StreamCodec<FriendlyByteBuf, SetBackground> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeInt(payload.color());
                },
                buffer -> new SetBackground(buffer.readUtf(), buffer.readInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Print(String consoleId, String text) implements CustomPacketPayload {
        public static final Type<Print> TYPE = new Type<>(id("console_print"));
        public static final StreamCodec<FriendlyByteBuf, Print> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.text(), 32600);
                },
                buffer -> new Print(buffer.readUtf(), buffer.readUtf(32600))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Clear(String consoleId) implements CustomPacketPayload {
        public static final Type<Clear> TYPE = new Type<>(id("console_clear"));
        public static final StreamCodec<FriendlyByteBuf, Clear> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeUtf(payload.consoleId()),
                buffer -> new Clear(buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Remove(String consoleId) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = new Type<>(id("console_remove"));
        public static final StreamCodec<FriendlyByteBuf, Remove> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeUtf(payload.consoleId()),
                buffer -> new Remove(buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetTitle(String consoleId, String title) implements CustomPacketPayload {
        public static final Type<SetTitle> TYPE = new Type<>(id("console_set_title"));
        public static final StreamCodec<FriendlyByteBuf, SetTitle> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.title(), 4096);
                },
                buffer -> new SetTitle(buffer.readUtf(), buffer.readUtf(4096))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Top(String consoleId, boolean top) implements CustomPacketPayload {
        public static final Type<Top> TYPE = new Type<>(id("console_top"));
        public static final StreamCodec<FriendlyByteBuf, Top> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeBoolean(payload.top());
                },
                buffer -> new Top(buffer.readUtf(), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InputRequest(
            String consoleId,
            String prompt,
            String objective,
            boolean focus
    ) implements CustomPacketPayload {
        public static final Type<InputRequest> TYPE = new Type<>(id("console_input_request"));
        public static final StreamCodec<FriendlyByteBuf, InputRequest> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.prompt(), 4096);
                    buffer.writeUtf(payload.objective());
                    buffer.writeBoolean(payload.focus());
                },
                buffer -> new InputRequest(
                        buffer.readUtf(),
                        buffer.readUtf(4096),
                        buffer.readUtf(),
                        buffer.readBoolean()
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InputResult(String consoleId, String text, String objective) implements CustomPacketPayload {
        public static final Type<InputResult> TYPE = new Type<>(id("console_input_result"));
        public static final StreamCodec<FriendlyByteBuf, InputResult> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.text(), 32600);
                    buffer.writeUtf(payload.objective());
                },
                buffer -> new InputResult(buffer.readUtf(), buffer.readUtf(32600), buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
