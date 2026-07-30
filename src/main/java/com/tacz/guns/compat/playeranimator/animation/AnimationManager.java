package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.AnimationName;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

public class AnimationManager {
    public static boolean hasPlayerAnimator3rd(GunDisplayInstance display) {
        Identifier location = display.getPlayerAnimator3rd();
        if (location == null) {
            return false;
        }
        return PlayerAnimatorAssetManager.get().containsKey(location);
    }

    public static boolean isFlying(AbstractClientPlayer player) {
        return !player.onGround() && player.getAbilities().flying;
    }

    /**
     * The fade length, in ticks, used everywhere animations are swapped. Eight ticks is what
     * makes hold, walk, run and aim blend into one another instead of snapping.
     */
    private static final int FADE_TICKS = 8;

    private static AbstractFadeModifier fade(int ticks) {
        return AbstractFadeModifier.standardFadeIn(ticks, EasingType.EASE_IN_OUT_SINE);
    }

    /**
     * The controller behind one of the four per-player layers. The rotation layer is wrapped in a
     * {@link ModifierLayer} so it can carry the adjustment modifier; the rest are bare.
     */
    private static PlayerAnimationController controller(AbstractClientPlayer player, Identifier dataId) {
        IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, dataId);
        if (layer instanceof ModifierLayer<?> modifierLayer) {
            layer = modifierLayer.getAnimation();
        }
        return layer instanceof PlayerAnimationController animationController ? animationController : null;
    }

    public static void playRotationAnimation(AbstractClientPlayer player, GunDisplayInstance display) {
        playAnimation(player, display, PlayerAnimatorCompat.ROTATION_ANIMATION, AnimationName.EMPTY, true);
    }

    public static void playLowerAnimation(AbstractClientPlayer player, GunDisplayInstance display, float limbSwingAmount) {
        // 如果玩家趴下，不播放下半身动画
        if (isPlayerLie(player)) {
            return;
        }
        // 如果玩家骑乘
        if (player.getVehicle() != null) {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.RIDE_LOWER);
            return;
        }
        // 如果玩家在天上，下半身动画就是站立动画
        if (isFlying(player)) {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.HOLD_LOWER);
            return;
        }
        if (player.isSprinting()) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.RUN_LOWER);
            }
            return;
        }
        if (limbSwingAmount > 0.05) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.WALK_LOWER);
            }
            return;
        }
        if (player.getPose() == Pose.CROUCHING) {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_LOWER);
        } else {
            playLoopAnimation(player, display, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.HOLD_LOWER);
        }
    }

    public static void playLoopUpperAnimation(AbstractClientPlayer player, GunDisplayInstance display, float limbSwingAmount) {
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        float aimingProgress = operator.getSynAimingProgress();
        if (aimingProgress <= 0) {
            // 疾跑时播放的动画
            if (!isFlying(player) && player.isSprinting()) {
                if (isPlayerLie(player)) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_MOVE);
                } else if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.RUN_UPPER);
                }
                return;
            }

            // 行走时的动画
            if (!isFlying(player) && limbSwingAmount > 0.05) {
                if (isPlayerLie(player)) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_MOVE);
                } else if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.WALK_UPPER);
                }
                return;
            }

            if (isPlayerLie(player)) {
                // 趴下时的动画
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE);
            } else {
                // 普通待命
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.HOLD_UPPER);
            }
        } else {
            if (isPlayerLie(player)) {
                // 趴下时瞄准
                if (!isFlying(player) && limbSwingAmount > 0.05) {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_MOVE);
                } else {
                    playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_AIM);
                }
            } else {
                // 普通瞄准
                playLoopAnimation(player, display, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.AIM_UPPER);
            }
        }
    }

    /**
     * Starts a looping animation, unless it is already the one playing.
     */
    public static void playLoopAnimation(AbstractClientPlayer player, GunDisplayInstance display, Identifier dataId, String animationName) {
        playAnimation(player, display, dataId, animationName, true);
    }

    /**
     * Starts a one-shot animation, unless something on that layer is still playing.
     */
    public static void playOnceAnimation(AbstractClientPlayer player, GunDisplayInstance display, Identifier dataId, String animationName) {
        playAnimation(player, display, dataId, animationName, false);
    }

    /**
     * @param interruptOther whether a different animation already playing on this layer should be
     *                       faded out and replaced, or left to finish
     */
    private static void playAnimation(AbstractClientPlayer player, GunDisplayInstance display, Identifier dataId,
                                      String animationName, boolean interruptOther) {
        Identifier animator3rd = display.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.get().containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.get().getAnimations(animator3rd, animationName).ifPresent(animation -> {
            PlayerAnimationController animationController = controller(player, dataId);
            if (animationController == null) {
                return;
            }
            if (animationController.isActive()) {
                if (!interruptOther) {
                    return;
                }
                Animation current = animationController.getCurrentAnimationInstance();
                // already the one we want, leave it running rather than restarting it every frame
                if (current != null && animationName.equals(current.data().name())) {
                    return;
                }
            }
            animationController.replaceAnimationWithFade(fade(FADE_TICKS), animation);
        });
    }

    public static void stopAllAnimation(AbstractClientPlayer player) {
        stopAllAnimation(player, FADE_TICKS);
    }

    public static void stopAllAnimation(AbstractClientPlayer player, int fadeTime) {
        stopAnimation(player, PlayerAnimatorCompat.LOWER_ANIMATION, fadeTime);
        stopAnimation(player, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, fadeTime);
        stopAnimation(player, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, fadeTime);
        stopAnimation(player, PlayerAnimatorCompat.ROTATION_ANIMATION, fadeTime);
    }


    private static void stopAnimation(AbstractClientPlayer player, Identifier dataId, int fadeTime) {
        PlayerAnimationController animationController = controller(player, dataId);
        if (animationController != null && animationController.isActive()) {
            animationController.replaceAnimationWithFade(fade(fadeTime), (Animation) null);
        }
    }

    private static boolean isPlayerLie(AbstractClientPlayer player) {
        // MOJANG 的奇妙设计，趴下的姿势名称是 SWIMMING
        return !player.isSwimming() && player.getPose() == Pose.SWIMMING;
    }

    public void onFire(GunShootEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(gunItemStack).ifPresent(index -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(player);
            float aimingProgress = operator.getSynAimingProgress();
            if (aimingProgress <= 0) {
                if (isPlayerLie(player)) {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.LIE_NORMAL_FIRE);
                } else {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.NORMAL_FIRE_UPPER);
                }
            } else {
                if (isPlayerLie(player)) {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.LIE_AIM_FIRE);
                } else {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.AIM_FIRE_UPPER);
                }
            }
        });
    }

    public void onReload(GunReloadEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getEntity();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(gunItemStack).ifPresent(index -> {
            if (isPlayerLie(player)) {
                playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.LIE_RELOAD);
            } else {
                playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.RELOAD_UPPER);
            }
        });
    }

    public void onMelee(GunMeleeEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        int randomIndex = shooter.getRandom().nextInt(3);
        String animationName = switch (randomIndex) {
            case 0 -> AnimationName.MELEE_UPPER;
            case 1 -> AnimationName.MELEE_2_UPPER;
            default -> AnimationName.MELEE_3_UPPER;
        };
        TimelessAPI.getGunDisplay(gunItemStack).ifPresent(
                index -> playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, animationName)
        );
    }

    public void onDraw(GunDrawEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player == player) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        }
        ItemStack currentGunItem = event.getCurrentGunItem();
        ItemStack previousGunItem = event.getPreviousGunItem();
        // 在切枪时，重置上半身动画
        if (currentGunItem.getItem() instanceof IGun && previousGunItem.getItem() instanceof IGun) {
            stopAnimation(player, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, FADE_TICKS);
            stopAnimation(player, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, FADE_TICKS);
            stopAnimation(player, PlayerAnimatorCompat.LOWER_ANIMATION, FADE_TICKS);
        }
    }
}
