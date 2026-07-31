package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.entity.state.BulletRenderState;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.compat.iris.IrisCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

public class EntityBulletRenderer extends EntityRenderer<EntityKineticBullet, BulletRenderState> {
    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);
    }

    @Override
    public BulletRenderState createRenderState() {
        return new BulletRenderState();
    }

    @Override
    public void extractRenderState(EntityKineticBullet bullet, BulletRenderState state, float partialTicks) {
        super.extractRenderState(bullet, state, partialTicks);
        state.ammoModel = null;
        state.ammoRenderType = null;
        state.tracer = false;

        state.yRot = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F;
        state.xRot = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());

        Identifier gunId = bullet.getGunId();
        Identifier gunDisplayId = bullet.getGunDisplayId();
        Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(gunDisplayId, gunId);
        if (display.isEmpty()) {
            return;
        }
        float @Nullable [] tracerColor = bullet.getTracerColorOverride().orElse(display.get().getTracerColor());

        Optional<ClientAmmoIndex> maybeIndex = TimelessAPI.getClientAmmoIndex(bullet.getAmmoId());
        if (maybeIndex.isEmpty()) {
            return;
        }
        ClientAmmoIndex ammoIndex = maybeIndex.get();

        Identifier textureLocation = ammoIndex.getAmmoEntityTextureLocation();
        if (ammoIndex.getAmmoEntityModel() != null && textureLocation != null) {
            state.ammoModel = ammoIndex.getAmmoEntityModel();
            state.ammoRenderType = RenderTypes.itemEntityTranslucentCull(textureLocation);
        }

        if (bullet.isTracerAmmo()) {
            state.tracerColor = Objects.requireNonNullElse(tracerColor, ammoIndex.getTracerColor());
            extractTracer(bullet, state, partialTicks);
        }
    }

    /**
     * 曳光那一段。原来这些是在渲染里现算的，其中还有几笔要写回实体（第一人称的枪口偏移只算一次
     * 就记住），所以整段留在摘取阶段 —— 提交阶段碰不到实体。
     */
    private void extractTracer(EntityKineticBullet bullet, BulletRenderState state, float partialTicks) {
        Entity shooter = bullet.getOwner();
        if (shooter == null) {
            return;
        }
        boolean isFirstPerson = this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && shooter instanceof LocalPlayer;
        if (isFirstPerson && !RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE.get()) {
            return;
        }

        Vec3 bulletPosition = bullet.getPosition(partialTicks);
        double disToEye = bulletPosition.distanceTo(shooter.getEyePosition(partialTicks));
        double trailLength = Math.min(0.85 * bullet.getDeltaMovement().length(), disToEye * 0.8);

        // 距离两格外才渲染，只在前 5 tick 判定
        if (bullet.tickCount < 5 && bulletPosition.distanceTo(shooter.getEyePosition()) <= 2) {
            return;
        }

        state.firstPerson = isFirstPerson;
        if (isFirstPerson) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vector3f offset = bullet.getFirstPersonRenderOffset();
            if (offset == null) {
                offset = new Vector3f(GunItemRendererWrapper.muzzleRenderOffset);
                bullet.setCameraXRot(camera.xRot());
                bullet.setCameraYRot(camera.yRot());
                bullet.setFirstPersonRenderOffset(offset);
            }
            state.muzzleOffset = offset;
            // 按照生存时间减少曳光弹的偏移，避免渲染位置距离落点太远
            state.offsetReducer = Math.max(0, (50 - disToEye)) / 50;
            /* 1.21.1 修改了渲染, 现在不需要坐标空间转换, 但是 Iris 还是老样子所以..
             * 在 Iris 光影包开启的时候还是需要做类似的措施. */
            state.compensateShaderPack = IrisCompat.isShaderPackInUse();
            state.cameraXRot = bullet.getCameraXRot();
            state.cameraYRot = bullet.getCameraYRot();
        }

        // 说是 override 其实默认值是 1，所以这里直接乘也没关系
        float width = 0.005f * bullet.getTracerSizeOverride() * (float) Math.max(1.0, disToEye / 3.5);
        state.tracerWidth = width;
        state.trailLength = trailLength;
        state.tracerRenderType = RenderTypes.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15, 15);
        state.tracer = true;
    }

    @Override
    public void submit(BulletRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.ammoModel != null && state.ammoRenderType != null) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
            poseStack.translate(0, 1.5, 0);
            poseStack.scale(-1, -1, 1);
            state.ammoModel.render(poseStack, ItemDisplayContext.GROUND, collector, state.ammoRenderType,
                    state.lightCoords, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        if (state.tracer && state.tracerRenderType != null) {
            submitTracerAmmo(state, poseStack, collector);
        }
    }

    private void submitTracerAmmo(BulletRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        getModel().ifPresent(model -> {
            poseStack.pushPose();
            {
                if (state.firstPerson && state.muzzleOffset != null) {
                    // 摄像机旋转
                    if (state.compensateShaderPack) {
                        poseStack.mulPose(Axis.YN.rotationDegrees(state.cameraYRot + 180f));
                        poseStack.mulPose(Axis.XN.rotationDegrees(state.cameraXRot));
                    }
                    // 应用偏移
                    poseStack.translate(state.muzzleOffset.x * state.offsetReducer,
                            state.muzzleOffset.y * state.offsetReducer,
                            state.muzzleOffset.z * state.offsetReducer);
                    // 逆转摄像机旋转
                    if (state.compensateShaderPack) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(state.cameraXRot));
                        poseStack.mulPose(Axis.YP.rotationDegrees(state.cameraYRot + 180f));
                    }
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
                poseStack.translate(0, state.firstPerson ? 0 : -0.2, state.trailLength / 2.0);
                poseStack.scale(state.tracerWidth, state.tracerWidth, (float) state.trailLength);
                model.render(poseStack, ItemDisplayContext.NONE, collector, state.tracerRenderType,
                        state.lightCoords, OverlayTexture.NO_OVERLAY,
                        state.tracerColor[0], state.tracerColor[1], state.tracerColor[2], 1, null);
            }
            poseStack.popPose();
        });
    }

    @Override
    protected int getBlockLightLevel(@NotNull EntityKineticBullet entityBullet, @NotNull BlockPos blockPos) {
        return 15;
    }

    @Override
    public boolean shouldRender(EntityKineticBullet bullet, Frustum camera, double pCamX, double pCamY, double pCamZ) {
        // Entity 不再自己给剔除用的包围盒，改由渲染器决定
        AABB aabb = this.getBoundingBoxForCulling(bullet).inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0) {
            aabb = new AABB(bullet.getX() - 2.0, bullet.getY() - 2.0, bullet.getZ() - 2.0, bullet.getX() + 2.0, bullet.getY() + 2.0, bullet.getZ() + 2.0);
        }
        return camera.isVisible(aabb);
    }
}
