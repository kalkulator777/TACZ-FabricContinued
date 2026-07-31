package com.tacz.guns.client.event;

import cn.sh1rocu.tacz.api.event.RenderLivingEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

@Environment(EnvType.CLIENT)
public class RenderHeadShotAABB {
    public static void onRenderEntity(RenderLivingEvent.Post event) {
        // shouldRenderHitBoxes 没了，碰撞箱调试现在直接读那个按键的状态
        boolean canRender = Minecraft.getInstance().options.keyDebugShowHitboxes.isDown();
        if (!canRender) {
            return;
        }
        if (!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
        if (aabb == null) {
            float width = entity.getBbWidth();
            float eyeHeight = entity.getEyeHeight();
            // 扩张 0.01，避免和原版显示重合
            aabb = new AABB(-width / 2, eyeHeight - 0.25, -width / 2, width / 2, eyeHeight + 0.25, width / 2).inflate(0.01);
        }
        // renderLineBox 没了。ShapeRenderer.renderShape 收的是 VoxelShape 和一个打包好的颜色，
        // 而几何体也不再是自己往缓冲里画，得交给收集器
        AABB box = aabb;
        event.getCollector().submitCustomGeometry(event.getPoseStack(), RenderTypes.lines(), (pose, buffer) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);
            ShapeRenderer.renderShape(local, buffer, Shapes.create(box), 0.0, 0.0, 0.0, 0xFFFFFF00, 1.0F);
        });
    }
}
