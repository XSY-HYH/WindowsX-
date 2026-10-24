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

    public record Clear(String consoleId, int mode) implements CustomPacketPayload {
        public static final Type<Clear> TYPE = new Type<>(id("console_clear"));
        public static final StreamCodec<FriendlyByteBuf, Clear> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.mode());
                },
                buffer -> new Clear(buffer.readUtf(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Scroll(String consoleId, int lines) implements CustomPacketPayload {
        public static final Type<Scroll> TYPE = new Type<>(id("console_scroll"));
        public static final StreamCodec<FriendlyByteBuf, Scroll> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.lines());
                },
                buffer -> new Scroll(buffer.readUtf(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetRegion(String consoleId, int top, int bottom) implements CustomPacketPayload {
        public static final Type<SetRegion> TYPE = new Type<>(id("console_set_region"));
        public static final StreamCodec<FriendlyByteBuf, SetRegion> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.top());
                    buffer.writeVarInt(payload.bottom());
                },
                buffer -> new SetRegion(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PrintAt(String consoleId, int x, int y, String text) implements CustomPacketPayload {
        public static final Type<PrintAt> TYPE = new Type<>(id("console_print_at"));
        public static final StreamCodec<FriendlyByteBuf, PrintAt> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.x());
                    buffer.writeVarInt(payload.y());
                    buffer.writeUtf(payload.text(), 32600);
                },
                buffer -> new PrintAt(
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(32600)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetSize(String consoleId, int height, int width) implements CustomPacketPayload {
        public static final Type<SetSize> TYPE = new Type<>(id("console_set_size"));
        public static final StreamCodec<FriendlyByteBuf, SetSize> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.height());
                    buffer.writeVarInt(payload.width());
                },
                buffer -> new SetSize(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Move(String consoleId, int x, int y) implements CustomPacketPayload {
        public static final Type<Move> TYPE = new Type<>(id("console_move"));
        public static final StreamCodec<FriendlyByteBuf, Move> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.x());
                    buffer.writeVarInt(payload.y());
                },
                buffer -> new Move(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt())
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

    public record SetFont(String consoleId, int size, String family) implements CustomPacketPayload {
        public static final Type<SetFont> TYPE = new Type<>(id("console_set_font"));
        public static final StreamCodec<FriendlyByteBuf, SetFont> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.size());
                    buffer.writeUtf(payload.family());
                },
                buffer -> new SetFont(buffer.readUtf(), buffer.readVarInt(), buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetOpacity(String consoleId, float value) implements CustomPacketPayload {
        public static final Type<SetOpacity> TYPE = new Type<>(id("console_set_opacity"));
        public static final StreamCodec<FriendlyByteBuf, SetOpacity> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeFloat(payload.value());
                },
                buffer -> new SetOpacity(buffer.readUtf(), buffer.readFloat())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetCursor(String consoleId, boolean visible) implements CustomPacketPayload {
        public static final Type<SetCursor> TYPE = new Type<>(id("console_set_cursor"));
        public static final StreamCodec<FriendlyByteBuf, SetCursor> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeBoolean(payload.visible());
                },
                buffer -> new SetCursor(buffer.readUtf(), buffer.readBoolean())
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
            boolean focus,
            boolean live
    ) implements CustomPacketPayload {
        public static final Type<InputRequest> TYPE = new Type<>(id("console_input_request"));
        public static final StreamCodec<FriendlyByteBuf, InputRequest> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.prompt(), 4096);
                    buffer.writeUtf(payload.objective());
                    buffer.writeBoolean(payload.focus());
                    buffer.writeBoolean(payload.live());
                },
                buffer -> new InputRequest(
                        buffer.readUtf(),
                        buffer.readUtf(4096),
                        buffer.readUtf(),
                        buffer.readBoolean(),
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

    public record InputAborted(String consoleId) implements CustomPacketPayload {
        public static final Type<InputAborted> TYPE = new Type<>(id("console_input_aborted"));
        public static final StreamCodec<FriendlyByteBuf, InputAborted> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeUtf(payload.consoleId()),
                buffer -> new InputAborted(buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InputCancel(String consoleId, String message) implements CustomPacketPayload {
        public static final Type<InputCancel> TYPE = new Type<>(id("console_input_cancel"));
        public static final StreamCodec<FriendlyByteBuf, InputCancel> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.message(), 4096);
                },
                buffer -> new InputCancel(buffer.readUtf(), buffer.readUtf(4096))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InputInject(
            String consoleId,
            String text,
            boolean submit
    ) implements CustomPacketPayload {
        public static final Type<InputInject> TYPE = new Type<>(id("console_input_inject"));
        public static final StreamCodec<FriendlyByteBuf, InputInject> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.text(), 32600);
                    buffer.writeBoolean(payload.submit());
                },
                buffer -> new InputInject(buffer.readUtf(), buffer.readUtf(32600), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetMask(String consoleId, boolean masked) implements CustomPacketPayload {
        public static final Type<SetMask> TYPE = new Type<>(id("console_set_mask"));
        public static final StreamCodec<FriendlyByteBuf, SetMask> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeBoolean(payload.masked());
                },
                buffer -> new SetMask(buffer.readUtf(), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetLayer(String consoleId, String layer) implements CustomPacketPayload {
        public static final Type<SetLayer> TYPE = new Type<>(id("console_set_layer"));
        public static final StreamCodec<FriendlyByteBuf, SetLayer> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.layer());
                },
                buffer -> new SetLayer(buffer.readUtf(), buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetSmooth(String consoleId, int mode) implements CustomPacketPayload {
        public static final Type<SetSmooth> TYPE = new Type<>(id("console_set_smooth"));
        public static final StreamCodec<FriendlyByteBuf, SetSmooth> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.mode());
                },
                buffer -> new SetSmooth(buffer.readUtf(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetEvents(String consoleId, boolean enabled) implements CustomPacketPayload {
        public static final Type<SetEvents> TYPE = new Type<>(id("console_set_events"));
        public static final StreamCodec<FriendlyByteBuf, SetEvents> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeBoolean(payload.enabled());
                },
                buffer -> new SetEvents(buffer.readUtf(), buffer.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 按键回传，只发按下事件，key 是 GLFW 键码
    public record KeyEvent(String consoleId, int key, int mods) implements CustomPacketPayload {
        public static final Type<KeyEvent> TYPE = new Type<>(id("console_key_event"));
        public static final StreamCodec<FriendlyByteBuf, KeyEvent> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.key());
                    buffer.writeVarInt(payload.mods());
                },
                buffer -> new KeyEvent(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 点击回传，坐标是字符格且从 1 开始
    public record ClickEvent(String consoleId, int x, int y, int button) implements CustomPacketPayload {
        public static final Type<ClickEvent> TYPE = new Type<>(id("console_click_event"));
        public static final StreamCodec<FriendlyByteBuf, ClickEvent> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.x());
                    buffer.writeVarInt(payload.y());
                    buffer.writeVarInt(payload.button());
                },
                buffer -> new ClickEvent(
                        buffer.readUtf(),
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

    public record SetPalette(String consoleId, String name) implements CustomPacketPayload {
        public static final Type<SetPalette> TYPE = new Type<>(id("console_set_palette"));
        public static final StreamCodec<FriendlyByteBuf, SetPalette> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeUtf(payload.name());
                },
                buffer -> new SetPalette(buffer.readUtf(), buffer.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // 查询请求，kind 0 列表 1 详情 2 整屏文本
    public record Query(String consoleId, int kind) implements CustomPacketPayload {
        public static final Type<Query> TYPE = new Type<>(id("console_query"));
        public static final StreamCodec<FriendlyByteBuf, Query> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.kind());
                },
                buffer -> new Query(buffer.readUtf(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record QueryResult(
            String consoleId,
            int kind,
            boolean ok,
            String text
    ) implements CustomPacketPayload {
        public static final Type<QueryResult> TYPE = new Type<>(id("console_query_result"));
        public static final StreamCodec<FriendlyByteBuf, QueryResult> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.consoleId());
                    buffer.writeVarInt(payload.kind());
                    buffer.writeBoolean(payload.ok());
                    buffer.writeUtf(payload.text(), 32600);
                },
                buffer -> new QueryResult(
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readBoolean(),
                        buffer.readUtf(32600)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
