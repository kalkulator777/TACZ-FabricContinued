package com.tacz.guns.event;

import cn.sh1rocu.tacz.api.event.PlayerTickEvent;
import com.tacz.guns.util.HitboxHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class HitboxHelperEvent {
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // 这里要的是逻辑端，不是物理端：单人游戏和开放局域网的世界里内置服务端跑在客户端发行版上，
        // 用 EnvType 判断会让命中箱历史和位置历史一次都不记录
        if (player.level().isClientSide) {
            return;
        }
        HitboxHelper.onPlayerTick(player);
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        HitboxHelper.onPlayerLoggedOut(player);
    }
}
