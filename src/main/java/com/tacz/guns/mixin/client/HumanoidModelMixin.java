package com.tacz.guns.mixin.client;

import com.tacz.guns.client.animation.third.InnerThirdPersonManager;
import net.minecraft.client.model.HumanoidModel;
import com.tacz.guns.client.renderer.RenderStateKeys;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {
    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart body;
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightArm;

    /**
     * setupAnim 现在只收渲染状态，摆姿势的时候看不到实体了。第三人称动画要读枪械物品、
     * 持枪状态机和姿势，所以实体在 extract 阶段被存进了状态，见 RenderStateKeys。
     */
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At(value = "TAIL"))
    private void setRotationAnglesHead(T state, CallbackInfo ci) {
        // 第一人称渲染时 ageInTicks 正好是 0，那一趟不做第三人称动画
        if (state.ageInTicks == 0) {
            return;
        }
        LivingEntity entity = RenderStateKeys.getEntity(state);
        if (entity == null) {
            return;
        }
        InnerThirdPersonManager.setRotationAnglesHead(entity, rightArm, leftArm, body, head, state.walkAnimationSpeed);
    }
}
