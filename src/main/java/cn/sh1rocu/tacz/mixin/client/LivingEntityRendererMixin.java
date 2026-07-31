package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.RenderLivingEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.RenderStateKeys;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 渲染拆成了两步。extract 那步是唯一还能看到实体的地方，所以在那里把实体存进渲染状态；
 * submit 那步真正下达绘制，POST 事件跟着它走。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void tacz$rememberEntity(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        RenderStateKeys.putEntity(state, entity);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("TAIL"))
    private void tacz$onPostEvent(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        LivingEntity entity = RenderStateKeys.getEntity(state);
        if (entity == null) {
            return;
        }
        var event = new RenderLivingEvent.Post(entity, (LivingEntityRenderer<?, ?, ?>) (Object) this,
                state.ageInTicks, poseStack, collector, state.lightCoords);
        RenderLivingEvent.POST.invoker().post(event);
    }
}
