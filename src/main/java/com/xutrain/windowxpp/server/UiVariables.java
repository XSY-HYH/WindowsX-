package com.xutrain.windowxpp.server;

import com.xutrain.windowxpp.WindowXPP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// wxpp 专属伪变量：每玩家一份，存字符串、布尔与整数
public final class UiVariables {
    private static final ResourceLocation STORAGE_ID =
            ResourceLocation.fromNamespaceAndPath(WindowXPP.MOD_ID, "ui");

    private UiVariables() {
    }

    public enum Type {
        STRING, BOOL, INT;

        public static Type byName(String name) {
            for (Type type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Entry(Type type, String value) {
    }

    public static boolean create(MinecraftServer server, UUID owner, String name, Type type) {
        CompoundTag root = root(server).copy();
        CompoundTag variables = root.getCompound(owner.toString()).copy();
        if (variables.contains(name)) {
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("type", type.id());
        entry.putString("value", defaultValue(type));
        variables.put(name, entry);
        root.put(owner.toString(), variables);
        save(server, root);
        return true;
    }

    public static Entry get(MinecraftServer server, UUID owner, String name) {
        CompoundTag variables = root(server).getCompound(owner.toString());
        if (!variables.contains(name)) {
            return null;
        }
        CompoundTag entry = variables.getCompound(name);
        Type type = Type.byName(entry.getString("type"));
        if (type == null) {
            return null;
        }
        return new Entry(type, entry.getString("value"));
    }

    public static boolean set(MinecraftServer server, UUID owner, String name, String value) {
        CompoundTag root = root(server).copy();
        CompoundTag variables = root.getCompound(owner.toString()).copy();
        if (!variables.contains(name)) {
            return false;
        }
        CompoundTag entry = variables.getCompound(name).copy();
        Type type = Type.byName(entry.getString("type"));
        if (type == null) {
            return false;
        }
        String normalized = normalize(type, value);
        if (normalized == null) {
            return false;
        }
        entry.putString("value", normalized);
        variables.put(name, entry);
        root.put(owner.toString(), variables);
        save(server, root);
        return true;
    }

    public static boolean add(MinecraftServer server, UUID owner, String name, int delta) {
        Entry entry = get(server, owner, name);
        if (entry == null || entry.type() != Type.INT) {
            return false;
        }
        int current;
        try {
            current = Integer.parseInt(entry.value());
        } catch (NumberFormatException e) {
            current = 0;
        }
        return set(server, owner, name, String.valueOf(current + delta));
    }

    public static boolean toggle(MinecraftServer server, UUID owner, String name) {
        Entry entry = get(server, owner, name);
        if (entry == null || entry.type() != Type.BOOL) {
            return false;
        }
        return set(server, owner, name, "1".equals(entry.value()) ? "0" : "1");
    }

    public static boolean remove(MinecraftServer server, UUID owner, String name) {
        CompoundTag root = root(server).copy();
        CompoundTag variables = root.getCompound(owner.toString()).copy();
        if (!variables.contains(name)) {
            return false;
        }
        variables.remove(name);
        if (variables.isEmpty()) {
            root.remove(owner.toString());
        } else {
            root.put(owner.toString(), variables);
        }
        save(server, root);
        return true;
    }

    public static List<String> list(MinecraftServer server, UUID owner) {
        CompoundTag variables = root(server).getCompound(owner.toString());
        List<String> result = new ArrayList<>();
        for (String name : variables.getAllKeys()) {
            CompoundTag entry = variables.getCompound(name);
            Type type = Type.byName(entry.getString("type"));
            if (type == null) {
                continue;
            }
            result.add(name + " (" + type.id() + ") = " + entry.getString("value"));
        }
        return result;
    }

    // 外部写入统一走这里，返回规范化后的值，类型不符返回 null
    public static String normalize(Type type, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return switch (type) {
            case STRING -> value;
            case INT -> {
                try {
                    yield String.valueOf(Integer.parseInt(trimmed));
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            case BOOL -> switch (trimmed.toLowerCase(Locale.ROOT)) {
                case "true", "1", "on", "yes" -> "1";
                case "false", "0", "off", "no" -> "0";
                default -> null;
            };
        };
    }

    private static String defaultValue(Type type) {
        return switch (type) {
            case STRING -> "";
            case BOOL, INT -> "0";
        };
    }

    private static CompoundTag root(MinecraftServer server) {
        return server.getCommandStorage().get(STORAGE_ID);
    }

    private static void save(MinecraftServer server, CompoundTag root) {
        server.getCommandStorage().set(STORAGE_ID, root);
    }
}
