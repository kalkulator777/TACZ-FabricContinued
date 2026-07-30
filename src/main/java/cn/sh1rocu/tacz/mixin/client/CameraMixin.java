package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.ViewportEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets the gun animation state machine steer the camera. Vanilla's {@code setRotation} takes yaw
 * and pitch only, so roll is carried across in a field and folded into the quaternion afterwards.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu) — see the roadmap for why.
 */
@Mixin(Camera.class)
public class CameraMixin {
    @Unique
    private float tacz$roll;

    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void tacz$computeCameraAngles(Camera instance, float yRot, float xRot, Operation<Void> original,
                                          @Local(argsOnly = true) float partialTick) {
        ViewportEvent.ComputeCameraAngles event = new ViewportEvent.ComputeCameraAngles(instance, partialTick, yRot, xRot, 0.0F);
        ViewportEvent.CAMERA.invoker().post(event);
        this.tacz$roll = event.getRoll();
        original.call(instance, event.getYaw(), event.getPitch());
    }

    @ModifyArg(method = "setRotation",
            at = @At(remap = false, value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"),
            index = 2)
    private float tacz$setRollValue(float value) {
        return value + -this.tacz$roll * (float) (Math.PI / 180.0);
    }
}
