package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.gl.StencilSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

@Environment(EnvType.CLIENT)
public final class RenderHelper {
    public static void enableItemEntityStencilTest() {
        StencilSupport.enableTest();
    }

    public static void disableItemEntityStencilTest() {
        StencilSupport.disableTest();
    }

    /**
     * 1.21.9 起手臂不能再直接画进顶点缓冲了：AvatarRenderer.renderRightHand 收的是
     * SubmitNodeCollector，几何体先提交、之后由 FeatureRenderDispatcher 统一画。
     * 这里就用它自己那个 storage，和原版 ItemInHandRenderer 走的是同一条路。
     */
    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm hand, PoseStack matrixStack, int combinedLight) {
        Minecraft mc = Minecraft.getInstance();
        AvatarRenderer<AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
        SubmitNodeCollector collector = mc.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage();
        Identifier skin = player.getSkin().body().texturePath();

        if (hand == HumanoidArm.RIGHT) {
            renderer.renderRightHand(matrixStack, collector, combinedLight, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            renderer.renderLeftHand(matrixStack, collector, combinedLight, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
    }
}
