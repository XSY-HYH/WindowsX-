package com.xutrain.windowxpp;

import com.mojang.logging.LogUtils;
import com.xutrain.windowxpp.command.ConsoleIdArgumentType;
import com.xutrain.windowxpp.command.ConsoleOptionsArgumentType;
import com.xutrain.windowxpp.command.WindowXCCommand;
import com.xutrain.windowxpp.command.WindowXPPCommand;
import com.xutrain.windowxpp.score.WindowXCriteria;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(WindowXPP.MOD_ID)
public final class WindowXPP {
    public static final String MOD_ID = "windowxpp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MOD_ID);

    public static final Supplier<ArgumentTypeInfo<?, ?>> CONSOLE_ID_ARGUMENT_TYPE =
            COMMAND_ARGUMENT_TYPES.register("console_id",
                    () -> ArgumentTypeInfos.registerByClass(
                            ConsoleIdArgumentType.class,
                            SingletonArgumentInfo.contextFree(ConsoleIdArgumentType::consoleId)));

    public static final Supplier<ArgumentTypeInfo<?, ?>> CONSOLE_OPTIONS_ARGUMENT_TYPE =
            COMMAND_ARGUMENT_TYPES.register("console_options",
                    () -> ArgumentTypeInfos.registerByClass(
                            ConsoleOptionsArgumentType.class,
                            SingletonArgumentInfo.contextFree(ConsoleOptionsArgumentType::options)));

    public WindowXPP(IEventBus modEventBus, ModContainer container) {
        COMMAND_ARGUMENT_TYPES.register(modEventBus);
        // 让准则在命令解析前进入缓存
        WindowXCriteria.touch();
        NeoForge.EVENT_BUS.addListener(WindowXCCommand::register);
        NeoForge.EVENT_BUS.addListener(WindowXPPCommand::register);
    }
}
