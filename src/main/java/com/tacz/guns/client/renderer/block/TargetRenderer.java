package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.renderer.block.state.TargetRenderState;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TargetRenderer implements BlockEntityRenderer<TargetBlockEntity, TargetRenderState> {
    private static final String UPPER_NAME = "target_upper";
    private static final String HEAD_NAME = "head";

    public TargetRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MODEL_LOCATION);
    }

    @Override
    public TargetRenderState createRenderState() {
        return new TargetRenderState();
    }

    @Override
    public void extractRenderState(TargetBlockEntity blockEntity, TargetRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.facing = blockEntity.getBlockState().getValue(TargetBlock.FACING);
        state.tiltDegrees = -Mth.lerp(partialTick, blockEntity.oRot, blockEntity.rot);

        /* 主人的皮肤。以前是自己去 SkinManager 要一张不安全的皮肤纹理；现在方块实体存的是
         * 未解析的档案，解析和缓存都归客户端的皮肤缓存管，它直接给出可以用的渲染类型。*/
        ResolvableProfile owner = blockEntity.getOwner();
        state.skullRenderType = owner == null ? null
                : Minecraft.getInstance().playerSkinRenderCache().getOrDefault(owner).renderType();
    }

    @Override
    public void submit(TargetRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        getModel().ifPresent(model -> {
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart upperModel = model.getNode(UPPER_NAME);
            float deg = state.tiltDegrees;
            boolean showHead = state.skullRenderType != null;

            poseStack.pushPose();
            poseStack.translate(0.5, 0.225, 0.5);
            poseStack.mulPose(Axis.YN.rotationDegrees(state.facing.get2DDataValue() * 90));
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.translate(0, -1.275, 0.0125);

            RenderType renderType = RenderTypes.entityTranslucent(InternalAssetLoader.TARGET_TEXTURE_LOCATION);
            /* 模型是所有靶子共用的一份，而几何回调是延后跑的，所以这一个靶子的倾角要在画之前
             * 设，不能在提交前设 —— 否则视野里有两个倾角不同的靶子时，两个都会用后提交的那个。*/
            model.render(poseStack, ItemDisplayContext.NONE, collector, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY,
                    () -> {
                        upperModel.xRot = (float) Math.toRadians(deg);
                        headModel.visible = false;
                    });

            if (showHead) {
                poseStack.translate(0, 1.25, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(deg));
                int light = state.lightCoords;
                collector.submitCustomGeometry(poseStack, state.skullRenderType, (pose, builder) -> {
                    PoseStack local = new PoseStack();
                    local.last().set(pose);
                    headModel.visible = true;
                    headModel.render(local, ItemDisplayContext.NONE, builder, light, OverlayTexture.NO_OVERLAY);
                });
            }
            poseStack.popPose();
        });
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
