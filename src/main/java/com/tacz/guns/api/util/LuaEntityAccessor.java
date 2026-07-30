package com.tacz.guns.api.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public record LuaEntityAccessor(LivingEntity entity) {
    /**
     * Entity.sendSystemMessage 没有了。对玩家来说等价的是聊天栏消息，其他实体收不到消息，
     * 原来那个方法对它们也是空操作。
     */
    public void sendSystemMessage(Component message) {
        if (entity instanceof Player player) {
            player.displayClientMessage(message, false);
        }
    }

    public void sendActionBar(Component message) {
        if (entity instanceof Player player) {
            player.displayClientMessage(message, true);
        }
    }

    public float getHealth() {
        return entity.getHealth();
    }

    public boolean hurt(float amount) {
        // hurt 现在返回 void，还会回答「打中了没有」的是 hurtOrSimulate
        return entity.hurtOrSimulate(entity.level().damageSources().generic(), amount);
    }

    public Component literal(String text) {
        return Component.literal(text);
    }

    public Component translatable(String key) {
        return Component.translatable(key);
    }

    public Component translatable(String key, Component... components) {
        return Component.translatable(key, (Object[]) components);
    }
}
