package com.tacz.guns.event;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 修正跨纬度时，枪械数据不刷新的问题，这是服务端的刷新
 */
public class TravelToDimensionEvent {
    public static void onTravelToDimension(Entity originalEntity, Entity newEntity, ServerLevel origin, ServerLevel destination) {
        refreshGunData(newEntity);
    }

    /**
     * Fabric splits the world change event in two and dispatches the entity one only for
     * non-players, so a player carrying a gun through a portal needs this overload. Without it
     * their firing, reloading and aiming state goes stale — the very bug this class exists for.
     */
    public static void onPlayerTravelToDimension(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        refreshGunData(player);
    }

    private static void refreshGunData(Entity entity) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.getMainHandItem().getItem() instanceof IGun) {
            IGunOperator.fromLivingEntity(livingEntity).initialData();
        }
    }
}
