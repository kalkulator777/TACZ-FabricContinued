package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Function;

/**
 * Turns the upper body and head towards where the player is actually looking. Keyframe animation
 * alone cannot follow the camera, so this rides on top of whatever is playing.
 * <p>
 * Bone names are Player Animation Library's, which are snake_case — PlayerAnimator spelled the
 * arms {@code leftArm} and {@code rightArm}.
 */
public class AdjustmentYRotModifier implements Function<String, Optional<AdjustmentModifier.PartModifier>> {
    private final Player player;

    private AdjustmentYRotModifier(Player player) {
        this.player = player;
    }

    @Override
    public Optional<AdjustmentModifier.PartModifier> apply(String partName) {
        Minecraft mc = Minecraft.getInstance();
        if (player.equals(mc.player) && mc.screen != null) {
            return Optional.empty();
        }

        if (player.getVehicle() != null && "body".equals(partName)) {
            return Optional.empty();
        }

        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float yBodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float yHeadRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float xRot = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        float yaw = yHeadRot - yBodyRot;
        yaw = Mth.wrapDegrees(yaw);
        yaw = Mth.clamp(yaw, -85f, 85f);

        float pitch = Mth.wrapDegrees(xRot);

        return switch (partName) {
            case "body" -> {
                if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
                    yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0, 0, -yaw * Mth.DEG_TO_RAD), Vec3f.ZERO));
                }
                yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0, -yaw * Mth.DEG_TO_RAD, 0), Vec3f.ZERO));
            }
            case "head" ->
                    Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch * Mth.DEG_TO_RAD, 0, 0), Vec3f.ZERO));
            case "left_arm", "right_arm" -> {
                if (TimelessAPI.getGunDisplay(player.getMainHandItem()).map(GunDisplayInstance::is3rdFixedHand).orElse(false)) {
                    yield Optional.empty();
                }
                yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch * Mth.DEG_TO_RAD, 0, 0), Vec3f.ZERO));
            }
            default -> Optional.empty();
        };
    }

    public static AdjustmentModifier getModifier(Player player) {
        return new AdjustmentModifier(new AdjustmentYRotModifier(player));
    }
}
