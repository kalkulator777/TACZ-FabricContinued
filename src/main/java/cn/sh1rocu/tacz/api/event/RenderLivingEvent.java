package cn.sh1rocu.tacz.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
/**
 * 1.21.2 起 LivingEntityRenderer 多了一个渲染状态的类型参数，成了三个泛型，而这里从来没用过
 * 它们中的任何一个 —— 事件只是把渲染器原样递出去。所以干脆去掉泛型。
 */
public abstract class RenderLivingEvent extends BaseEvent {
    private final LivingEntity entity;
    private final LivingEntityRenderer<?, ?, ?> renderer;
    private final float partialTick;
    private final PoseStack poseStack;
    private final SubmitNodeCollector collector;
    private final int packedLight;

    public static final Event<PostCallback> POST = EventFactory.createArrayBacked(PostCallback.class, callbacks -> event -> {
        for (PostCallback callback : callbacks) {
            callback.post(event);
        }
    });

    public interface PostCallback {
        void post(Post event);
    }

    protected RenderLivingEvent(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, float partialTick, PoseStack poseStack,
                                SubmitNodeCollector collector, int packedLight) {
        this.entity = entity;
        this.renderer = renderer;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.collector = collector;
        this.packedLight = packedLight;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public LivingEntityRenderer<?, ?, ?> getRenderer() {
        return renderer;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public SubmitNodeCollector getCollector() {
        return collector;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public static class Post extends RenderLivingEvent {
        public Post(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
            super(entity, renderer, partialTick, poseStack, collector, packedLight);
        }
    }
}
