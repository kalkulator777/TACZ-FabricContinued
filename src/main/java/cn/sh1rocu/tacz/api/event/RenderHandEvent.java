package cn.sh1rocu.tacz.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Fired before vanilla draws a held item in first person. Cancelling it replaces vanilla's
 * rendering — that is how the gun model gets drawn.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu) — see the roadmap for why.
 */
@Environment(EnvType.CLIENT)
public class RenderHandEvent extends BaseEvent implements ICancellableEvent {
    private final AbstractClientPlayer player;
    private final InteractionHand hand;
    private final ItemStack stack;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final float partialTick;
    private final float pitch;
    private final float swingProgress;
    private final float equipProgress;
    private final int packedLight;

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    });

    public RenderHandEvent(AbstractClientPlayer player, InteractionHand hand, ItemStack stack, PoseStack poseStack,
                           MultiBufferSource multiBufferSource, float partialTick, float pitch, float swingProgress,
                           float equipProgress, int packedLight) {
        this.player = player;
        this.hand = hand;
        this.stack = stack;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.partialTick = partialTick;
        this.pitch = pitch;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        this.packedLight = packedLight;
    }

    public AbstractClientPlayer getPlayer() {
        return this.player;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public ItemStack getItemStack() {
        return this.stack;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public MultiBufferSource getMultiBufferSource() {
        return this.multiBufferSource;
    }

    public float getPartialTick() {
        return this.partialTick;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getSwingProgress() {
        return this.swingProgress;
    }

    public float getEquipProgress() {
        return this.equipProgress;
    }

    public int getPackedLight() {
        return this.packedLight;
    }

    public interface Callback {
        void post(RenderHandEvent event);
    }
}
