package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.RenderStateKeys;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.9 之后渲染层从渲染状态出发，不再拿到实体：{@code render} 变成
 * {@code submit(PoseStack, SubmitNodeCollector, int, S, float, float)}，
 * {@code renderArmWithItem} 变成 {@code submitArmWithItem(...)}。
 * <p>
 * 副手和快捷栏那份枪的显示要读玩家背包，所以实体在 extract 阶段被存进了状态，
 * 见 {@link RenderStateKeys}。
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("TAIL"))
    private void tacz$submitTail(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                 ArmedEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
        LivingEntity entity = RenderStateKeys.getEntity(state);
        if (entity != null) {
            HumanoidOffhandRender.renderGun(entity, poseStack, collector, packedLight);
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void tacz$submitArmWithItemHead(ArmedEntityRenderState state, ItemStackRenderState itemState, ItemStack itemStack,
                                            HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector,
                                            int packedLight, CallbackInfo ci) {
        LivingEntity entity = RenderStateKeys.getEntity(state);
        if (entity == null) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (entity.equals(player)) {
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
        }
        // 主手持枪时左手那件物品交给枪械模型自己画
        if (IGun.mainHandHoldGun(entity) && arm == HumanoidArm.LEFT) {
            ci.cancel();
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("TAIL"))
    private void tacz$submitArmWithItemTail(ArmedEntityRenderState state, ItemStackRenderState itemState, ItemStack itemStack,
                                            HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector,
                                            int packedLight, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
    }
}
