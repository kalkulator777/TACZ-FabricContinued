package com.tacz.guns.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.tacz.guns.GunMod;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;

public class PreLoadConfig {
    public static final String FILE_NAME = "tacz-pre.toml";
    private static final String OVERRIDE_PATH = "gunpack.DefaultPackDebug";

    public static void init() {
        NeoForgeConfigRegistry.INSTANCE.register(GunMod.MOD_ID, ModConfig.Type.COMMON, spec, FILE_NAME);
    }

    private static ModConfigSpec spec;
    public static ModConfigSpec.BooleanValue override;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("gunpack");
        builder.comment("When enabled, the mod will not try to overwrite the default pack under .minecraft/tacz\n" +
                "Since 1.0.4, the overwriting will only run when you start client or a dedicated server");
        override = builder.define("DefaultPackDebug", false);
        builder.pop();
        spec = builder.build();
    }

    /**
     * 这个开关要在资源包发现的时候读，而资源包发现不保证晚于配置加载完成。
     * 此时 {@code override.get()} 会直接抛异常，所以先自己读一次文件，读不到再用默认值。
     * Forge 版是靠提前手动加载这份配置解决同一个问题的。
     */
    public static boolean isOverride() {
        if (spec.isLoaded()) {
            return override.get();
        }
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            return config.getOrElse(OVERRIDE_PATH, false);
        } catch (Exception e) {
            GunMod.LOGGER.warn("Failed to read {} before the config system was ready, assuming the default", FILE_NAME, e);
            return false;
        }
    }
}
