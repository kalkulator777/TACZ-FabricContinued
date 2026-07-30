package com.tacz.guns.event;

import cn.sh1rocu.tacz.api.event.PlayerTickEvent;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.util.HitboxHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public class HitboxHelperEvent {
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!OtherConfig.SERVER_HITBOX_LATENCY_FIX.get()) {
            return;
        }
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            HitboxHelper.onPlayerTick(event.getEntity());
        }
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        HitboxHelper.onPlayerLoggedOut(player);
    }
}
