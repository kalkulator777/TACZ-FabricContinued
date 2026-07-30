package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.world.entity.Avatar;

/**
 * The four layers the third-person gun animation is split across, registered per player. The
 * priorities keep them in a fixed order relative to each other and to other mods' layers.
 * <p>
 * Each layer is a controller that plays whatever {@link AnimationManager} hands it; the state
 * handler has nothing to decide, so it always answers STOP and waits to be triggered.
 */
public class AnimationDataRegisterFactory {
    public static void registerData() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.LOWER_ANIMATION, 93,
                AnimationDataRegisterFactory::controller);
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, 94,
                AnimationDataRegisterFactory::controller);
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, 95,
                AnimationDataRegisterFactory::controller);
        // this one carries the modifier that turns the upper body towards where the player looks
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.ROTATION_ANIMATION, 96,
                player -> new ModifierLayer<>(controller(player), AdjustmentYRotModifier.getModifier(player)));
    }

    /* PAL 1.1.9 跟着 1.21.9 走，把参数从 AbstractClientPlayer 放宽到了 Avatar —— 玩家和
     * 新的人偶实体的共同父类。这里的两个方法照做就行，玩家仍然是 Avatar。*/
    private static PlayerAnimationController controller(Avatar avatar) {
        return new PlayerAnimationController(avatar, (controller, state, setter) -> PlayState.STOP);
    }
}
