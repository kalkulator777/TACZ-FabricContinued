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

        /* PictureInPictureRenderer.prepare 交过来的是 scale(s, s, -s) —— Y 没有翻，而离屏纹理的
         * 正交投影和 GUI 一样是 Y 向下，物品模型是 Y 向上。旧代码在 RenderSystem 的 modelview 上
         * 写的正是 scale(1, -1, 1)。少了这一下，枪是倒着的，而且因为模型自身的原点偏移也跟着反了，
         * 整个沉到面板下边缘之外 —— 放大截图之后一眼可见。
         * 原版自己的物品 PiP 在这里连 X 一起翻，那是物品在 GUI 里额外镜像的老约定，这个预览没有。*/
        poseStack.scale(1.0F, -1.0F, 1.0F);

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
