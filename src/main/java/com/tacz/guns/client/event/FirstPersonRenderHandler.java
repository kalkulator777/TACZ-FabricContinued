package com.tacz.guns.client.event;

import cn.sh1rocu.tacz.api.event.RenderHandEvent;
import cn.sh1rocu.tacz.api.event.RenderTickEvent;
import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import com.tacz.guns.api.client.renderer.IFPAnimationInstance;
import com.tacz.guns.api.client.renderer.IFPGeoItemRenderer;
import com.tacz.guns.client.animation.FirstPersonClock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Drives what the player sees in their own hands. Vanilla draws a held item and stops; a gun has
 * a draw animation, a put-away animation and a state machine that has to be told when the item
 * under it changed. This is what tells it.
 * <p>
 * Two instances exist during a switch: the outgoing item keeps animating until its put-away
 * duration elapses, then the incoming one is created and drawn.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu). The library's first-person particle
 * system went with it — nothing here ever created an emitter, so it ticked and drew nothing.
 */
@Environment(EnvType.CLIENT)
public final class FirstPersonRenderHandler {
    private static int realSelectedSlot = -1;
    private static ItemStack realMainHand = ItemStack.EMPTY;
    private static boolean transitioning = false;
    private static IFPAnimationInstance activeInstance = null;
    private static IFPAnimationInstance previousInstance = null;
    private static ItemStack pendingTarget = ItemStack.EMPTY;
    private static long switchStartTime = 0L;
    private static long currentSheatheDuration = 0L;
    private static boolean lockVanilla = false;
    private static boolean nextIsCustom = false;
    private static boolean forceHandSwapFlag = false;

    private FirstPersonRenderHandler() {
    }

    public static void onPlayerLoggedOut(ClientPacketListener handler, Minecraft client) {
        reset();
    }

    public static void reset() {
        realSelectedSlot = -1;
        realMainHand = ItemStack.EMPTY;
        transitioning = false;
        activeInstance = null;
        previousInstance = null;
        pendingTarget = ItemStack.EMPTY;
        lockVanilla = false;
        nextIsCustom = false;
        forceHandSwapFlag = false;
        switchStartTime = 0L;
        currentSheatheDuration = 0L;
    }

    public static void onSwapItemWithOffHand(SwapItemWithOffHand event) {
        forceHandSwapFlag = true;
    }

    public static void onClientTick(Minecraft client) {
        if (!FirstPersonClock.shouldTick()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int slot = player.getInventory().getSelectedSlot();
        ItemStack mainHand = player.getMainHandItem();
        boolean slotChanged = slot != realSelectedSlot || forceHandSwapFlag;
        boolean itemChanged = !isSameItemStacks(realMainHand, mainHand);
        forceHandSwapFlag = false;
        realSelectedSlot = slot;
        realMainHand = mainHand;

        if (slotChanged || itemChanged) {
            beginSwitch(mainHand);
        }
        if (activeInstance != null) {
            activeInstance.updateItem(mainHand);
        }
        if (transitioning && getSheatheProgress() >= 1.0F) {
            transitioning = false;
            lockVanilla = false;
            activeInstance = createInstance(pendingTarget);
            previousInstance = null;
        }
    }

    /**
     * Upstream had two identical copies of this, one for a slot change and one for the item in the
     * selected slot changing under the player. The only difference was that the slot-change copy
     * also cleared the transition flag on the branch where it is already false.
     */
    private static void beginSwitch(ItemStack newStack) {
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);
        if (transitioning) {
            return;
        }
        boolean hadInstance = activeInstance != null;
        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);
        if (hadInstance) {
            transitioning = true;
            lockVanilla = true;
            switchStartTime = FirstPersonClock.nowMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());
            previousInstance.triggerPutAway();
        } else {
            lockVanilla = false;
        }
    }

    public static void tickAnimation(RenderTickEvent event) {
        if (event.phase != RenderTickEvent.Phase.START || !FirstPersonClock.shouldTick()) {
            return;
        }
        IFPAnimationInstance instance = getActiveAnimationInstance();
        if (instance != null) {
            instance.triggerDraw();
            instance.tick(event.getTimer().getGameTimeDeltaPartialTick(true));
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IFPAnimationInstance instance = getActiveAnimationInstance();
        if (instance == null) {
            return;
        }
        ItemStack stack = instance.currentItem();
        if (stack.isEmpty()) {
            return;
        }
        Optional<IFPGeoItemRenderer> renderer = getRenderer(stack);
        if (renderer.isEmpty()) {
            return;
        }
        IFPGeoItemRenderer geoRenderer = renderer.get();
        /* Upstream cancels the off-hand pass here and then falls through and renders anyway, so
         * with something in the off hand the gun is drawn a second time against the off-hand pose.
         * That looks wrong — the mod's own disabled FirstPersonRenderEvent returns at this point —
         * but it is what ships today, and changing it changes what players see. Kept as is until
         * someone can check it in game; see §7 of the roadmap. */
        if (event.getHand() == InteractionHand.OFF_HAND && geoRenderer.blockOffhandRender()) {
            event.setCanceled(true);
        }
        geoRenderer.renderFirstPerson(
                player,
                stack,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                event.getPartialTick()
        );
        event.setCanceled(true);
    }

    /**
     * True while an item is being put away, when vanilla's own equip animation must not run.
     */
    public static boolean shouldLockVanilla() {
        return lockVanilla;
    }

    public static float getTargetHeight() {
        return nextIsCustom ? 1.0F : 0.0F;
    }

    public static IFPAnimationInstance getActiveAnimationInstance() {
        return transitioning ? previousInstance : activeInstance;
    }

    private static IFPAnimationInstance createInstance(ItemStack stack) {
        return getRenderer(stack).map(r -> r.createAnimationInstance(stack, Minecraft.getInstance().getCameraEntity())).orElse(null);
    }

    private static boolean isCustomItem(ItemStack stack) {
        return getRenderer(stack).isPresent();
    }

    private static long calculateSheatheDuration(ItemStack stack) {
        return getRenderer(stack).map(r -> r.getPutAwayDuration(stack)).orElse(0L);
    }

    private static float getSheatheProgress() {
        if (currentSheatheDuration <= 0L) {
            return 1.0F;
        }
        long elapsed = FirstPersonClock.nowMillis() - switchStartTime;
        return Math.min(1.0F, (float) elapsed / (float) currentSheatheDuration);
    }

    private static Optional<IFPGeoItemRenderer> getRenderer(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return BuiltinItemRendererRegistry.INSTANCE.get(stack.getItem()) instanceof IFPGeoItemRenderer renderer
                ? Optional.of(renderer)
                : Optional.empty();
    }

    private static boolean isSameItemStacks(ItemStack oldStack, ItemStack newStack) {
        if (oldStack == newStack) {
            return true;
        }
        if (oldStack.isEmpty() && newStack.isEmpty()) {
            return true;
        }
        if (oldStack.isEmpty() || newStack.isEmpty()) {
            return false;
        }
        return getRenderer(oldStack)
                .map(r -> r.isSameItem(oldStack, newStack))
                .orElseGet(() -> ItemStack.isSameItem(oldStack, newStack));
    }
}
