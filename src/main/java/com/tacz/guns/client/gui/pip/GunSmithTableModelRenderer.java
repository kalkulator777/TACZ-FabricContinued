package com.tacz.guns.client.gui.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.util.RenderDistance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public class GunSmithTableModelRenderer extends PictureInPictureRenderer<GunSmithTableModelRenderState> {
    public GunSmithTableModelRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<GunSmithTableModelRenderState> getRenderStateClass() {
        return GunSmithTableModelRenderState.class;
    }

    @Override
    protected void renderToTexture(GunSmithTableModelRenderState state, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        // 预览要的是高模
        RenderDistance.markGuiRenderTimestamp();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation()));

        TrackingItemStackRenderState itemState = new TrackingItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(itemState, state.stack(), ItemDisplayContext.FIXED, null, null, 0);

        // 离屏纹理里没有别人会替我们冲刷收集器，所以自己收自己画
        FeatureRenderDispatcher dispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        itemState.submit(poseStack, dispatcher.getSubmitNodeStorage(), 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
    }

    @Override
    protected String getTextureLabel() {
        return "tacz gun smith table preview";
    }
}
