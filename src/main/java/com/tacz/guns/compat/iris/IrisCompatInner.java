package com.tacz.guns.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;

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

}
