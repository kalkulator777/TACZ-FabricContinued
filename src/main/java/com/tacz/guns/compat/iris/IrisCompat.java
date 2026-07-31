package com.tacz.guns.compat.iris;

import com.tacz.guns.init.CompatRegistry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Iris 用来插队实体绘制的那套 batchedentityrendering 在 1.10 里没有了 —— 原版自己的提交管线
 * 接管了同一件事。所以那个「按我们说的地方冲刷缓冲」的口子也一起删了：它本来就是 §8 里记着的
 * 那笔债，唯一没有 api.v0 对应物、每次 Iris 更新都得重看一遍的地方。
 * <p>
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

}
