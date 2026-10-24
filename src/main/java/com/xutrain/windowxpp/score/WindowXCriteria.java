package com.xutrain.windowxpp.score;

import com.xutrain.windowxpp.WindowXPP;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.lang.reflect.Field;
import java.util.Map;

// 窗口几何准则，供 /scoreboard objectives add <name> windowxpp:window_width 使用
public final class WindowXCriteria extends ObjectiveCriteria {
    public static final WindowXCriteria WINDOW_X = new WindowXCriteria("windowxpp:window_x");
    public static final WindowXCriteria WINDOW_Y = new WindowXCriteria("windowxpp:window_y");
    public static final WindowXCriteria WINDOW_WIDTH = new WindowXCriteria("windowxpp:window_width");
    public static final WindowXCriteria WINDOW_HEIGHT = new WindowXCriteria("windowxpp:window_height");

    private static final WindowXCriteria[] ALL = {
            WINDOW_X, WINDOW_Y, WINDOW_WIDTH, WINDOW_HEIGHT
    };

    private WindowXCriteria(String name) {
        // 只读由模组写入，玩家不能手动改
        super(name, true, RenderType.INTEGER);
    }

    // 原版补全只列 CUSTOM_CRITERIA，把自己的准则补进去才能在 add 时选到
    @SuppressWarnings("unchecked")
    public static void touch() {
        try {
            Field field = ObjectiveCriteria.class.getDeclaredField("CUSTOM_CRITERIA");
            field.setAccessible(true);
            Map<String, ObjectiveCriteria> custom = (Map<String, ObjectiveCriteria>) field.get(null);
            for (WindowXCriteria criteria : ALL) {
                custom.put(criteria.getName(), criteria);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            WindowXPP.LOGGER.warn("[WindowsX++] 无法注册计分板准则补全: {}", e.getMessage());
        }
    }
}
