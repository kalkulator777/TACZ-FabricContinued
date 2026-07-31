package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.block.state.StatueRenderState;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StatueRenderer implements BlockEntityRenderer<StatueBlockEntity, StatueRenderState> {
    private final ItemModelResolver itemModelResolver;

    public StatueRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.STATUE_MODEL_LOCATION);
    }

    @Override
    public StatueRenderState createRenderState() {
        return new StatueRenderState();
    }

    @Override
    public void extractRenderState(StatueBlockEntity blockEntity, StatueRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.facing = blockEntity.getBlockState().getValue(TargetBlock.FACING);
        state.bobOffset = Math.sin(Util.getMillis() / 500.0) * 0.1;

        Level level = blockEntity.getLevel();
        this.itemModelResolver.updateForTopItem(state.gunItem, blockEntity.getGunItem(), ItemDisplayContext.FIXED,
                level, null, (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(StatueRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        getModel().ifPresent(model -> {
            poseStack.pushPose();
            {
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Axis.YN.rotationDegrees((state.facing.get2DDataValue() + 2) % 4 * 90));
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));

                RenderType renderType = RenderConfig.BLOCK_ENTITY_TRANSLUCENT.get() ?
                        RenderTypes.entityTranslucent(getTextureLocation()) :
                        RenderTypes.entityCutout(getTextureLocation());
                model.render(poseStack, ItemDisplayContext.NONE, collector, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY);

                if (!state.gunItem.isEmpty()) {
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    poseStack.translate(0, -0.875, -1.2);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                    poseStack.translate(0, state.bobOffset, 0);
                    // 展示的枪一直是满亮度的，和以前一样
                    state.gunItem.submit(poseStack, collector, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, 0);
                }
            }
            poseStack.popPose();
        });
    }

    public static Identifier getTextureLocation() {
        return InternalAssetLoader.STATUE_TEXTURE_LOCATION;
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(StatueBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos().above()).closerThan(cameraPos, this.getViewDistance());
    }
}
