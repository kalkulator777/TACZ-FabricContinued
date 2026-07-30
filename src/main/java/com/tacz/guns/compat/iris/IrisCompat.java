package com.tacz.guns.compat.iris;

import com.tacz.guns.init.CompatRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Nothing here touches an Iris class directly — that happens in {@link IrisCompatInner}, which is
 * only reached once {@link #installed} is true, so the JVM never has to resolve an Iris type on an
 * installation without Iris. This is the same shape the Shoulder Surfing and Controllable
 * integrations use.
 */
public final class IrisCompat {
    private static boolean installed = false;

    private IrisCompat() {
    }

    public static void init() {
        installed = FabricLoader.getInstance().isModLoaded(CompatRegistry.IRIS);
    }

    /**
     * Whether a shader pack is currently driving the pipeline.
     */
    public static boolean isShaderPackInUse() {
        return installed && IrisCompatInner.isShaderPackInUse();
    }

    /**
     * Whether the current draw is part of the shadow pass. Several effects are skipped there.
     */
    public static boolean isRenderShadow() {
        return installed && IrisCompatInner.isRenderingShadowPass();
    }

    /**
     * @return true if the buffer really was flushed, false if it is not one of Iris's batched sources
     */
    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        return installed && IrisCompatInner.endBatch(bufferSource);
    }
}
