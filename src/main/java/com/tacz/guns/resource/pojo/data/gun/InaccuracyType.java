package com.tacz.guns.resource.pojo.data.gun;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.util.HitboxHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public enum InaccuracyType {
    /**
     * 站立不动
     */
    @SerializedName("stand")
    STAND,
    /**
     * 移动
     */
    @SerializedName("move")
    MOVE,
    /**
     * 潜行，认为是其他 FPS 游戏中的半蹲
     */
    @SerializedName("sneak")
    SNEAK,
    /**
     * 趴下，原版确实可以趴下
     */
    @SerializedName("lie")
    LIE,
    /**
     * 瞄准状态
     */
    @SerializedName("aim")
    AIM;

    /**
     * 获取当前的不准确度状态
     *
     * @param livingEntity 射手
     * @return 不准度情况
     */
    public static InaccuracyType getInaccuracyType(LivingEntity livingEntity) {
        float aimingProgress = IGunOperator.fromLivingEntity(livingEntity).getSynAimingProgress();
        // 瞄准优先级最高
        if (aimingProgress == 1.0f) {
            return InaccuracyType.AIM;
        }
        // MOJANG 的奇妙设计，趴下的姿势名称是 SWIMMING
        if (!livingEntity.isSwimming() && livingEntity.getPose() == Pose.SWIMMING) {
            return InaccuracyType.LIE;
        }
        if (livingEntity.getPose() == Pose.CROUCHING) {
            return InaccuracyType.SNEAK;
        }
        if (isMove(livingEntity)) {
            return InaccuracyType.MOVE;
        }
        return InaccuracyType.STAND;
    }

    public static Map<InaccuracyType, Float> getDefaultInaccuracy() {
        Map<InaccuracyType, Float> inaccuracy = Maps.newHashMap();
        inaccuracy.put(InaccuracyType.STAND, 5f);
        inaccuracy.put(InaccuracyType.MOVE, 5.75f);
        inaccuracy.put(InaccuracyType.SNEAK, 3.5f);
        inaccuracy.put(InaccuracyType.LIE, 2.5f);
        inaccuracy.put(InaccuracyType.AIM, 0.15f);
        return inaccuracy;
    }

    private static boolean isMove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            return HitboxHelper.getPlayerVelocity(player).length() > 0.05f;
        }
        /* walkDist 和 walkDistO 都没了。接替它们的 walkAnimation.speed 是每 tick 水平位移的
         * 四倍（截到 1，再按 0.4 的系数平滑），所以阈值也乘四。平滑意味着起步和停下各差几 tick，
         * 对「这个生物在不在动」这个判断只有好处。*/
        return livingEntity.walkAnimation.speed() > 0.2f;
    }

    public boolean isAim() {
        return this == AIM;
    }
}
