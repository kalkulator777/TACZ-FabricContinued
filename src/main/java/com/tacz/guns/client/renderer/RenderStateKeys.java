package com.tacz.guns.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.2 起渲染器不再直接看实体：先在 extract 阶段把要用的东西抄进渲染状态，
 * 到了绘制的时候只能看那份状态。
 * <p>
 * 本模组的第三人称动画不是「几个数值」那么简单 —— 它要读枪械物品、玩家的持枪状态机、
 * 姿势，还要驱动 player-animator。把这些一条条搬进渲染状态等于把
 * {@code InnerThirdPersonManager} 重写一遍，所以这里搬的是实体本身：抄的时机仍然是
 * extract，只是抄的是一个引用。Fabric 的 {@link RenderStateDataKey} 就是给这种事准备的口子。
 */
@Environment(EnvType.CLIENT)
public final class RenderStateKeys {
    /**
     * 这份渲染状态是替哪只实体做的。
     */
    public static final RenderStateDataKey<LivingEntity> ENTITY = RenderStateDataKey.create(() -> "tacz:entity");

    private RenderStateKeys() {
    }

    public static void putEntity(EntityRenderState state, LivingEntity entity) {
        ((FabricRenderState) state).setData(ENTITY, entity);
    }

    @Nullable
    public static LivingEntity getEntity(EntityRenderState state) {
        return ((FabricRenderState) state).getData(ENTITY);
    }
}
