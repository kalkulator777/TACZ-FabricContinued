package com.tacz.guns.api.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;

/**
 * The per-item animation state that {@link com.tacz.guns.client.event.FirstPersonRenderHandler}
 * drives: one of these exists for the item currently in hand, and a second one for the item being
 * put away while a switch is in progress.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu) — see the roadmap for why. Upstream also
 * declared a pose and a camera rotation, for the library's own renderer to sample. This mod's
 * animation lives in its own state machine, so those were implemented as constant stubs and
 * nothing ever read them; they went, and the {@code mae} dependency they were the only use of
 * went with them.
 */
@Environment(EnvType.CLIENT)
public interface IFPAnimationInstance {
    ItemStack currentItem();

    void tick(float partialTick);

    void updateItem(ItemStack stack);

    /**
     * Called once when this item becomes the active one.
     */
    void triggerDraw();

    /**
     * Called when this item starts being put away. The handler keeps ticking it until
     * {@link IFPGeoItemRenderer#getPutAwayDuration} has elapsed.
     */
    void triggerPutAway();
}
