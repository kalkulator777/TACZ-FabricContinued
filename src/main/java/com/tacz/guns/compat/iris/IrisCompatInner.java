package com.tacz.guns.compat.iris;

import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Everything that actually names an Iris class. Only reached through {@link IrisCompat} after the
 * mod has been found, so this class is never loaded otherwise.
 */
final class IrisCompatInner {
    private IrisCompatInner() {
    }

    static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    /**
     * Iris batches entity draws and replays them in its own order. Our gun model writes and reads the
     * stencil buffer across several draws, so those draws have to be flushed where we say they are,
     * not where Iris feels like replaying them.
     * <p>
     * This is the one thing here with no {@code api.v0} equivalent, so it reaches into Iris's
     * internals and has to be re-checked on every Iris update — {@code FullyBufferedMultiBufferSource}
     * carries no compatibility promise. The {@code instanceof} means a rename degrades into the
     * effect not being applied rather than a crash, but a moved class would be a hard failure at load.
     */
    static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        if (bufferSource instanceof FullyBufferedMultiBufferSource fullyBuffered) {
            fullyBuffered.endBatch();
            return true;
        }
        return false;
    }
}
