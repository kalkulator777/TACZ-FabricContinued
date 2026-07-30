package com.tacz.guns.api.item.gun;

import com.google.gson.annotations.SerializedName;

public enum FireMode {
    /**
     * 全自动
     */
    @SerializedName("auto")
    AUTO,
    /**
     * 半自动
     */
    @SerializedName("semi")
    SEMI,
    /**
     * 多连发
     */
    @SerializedName("burst")
    BURST,
    /**
     * 未知的其他情况？
     */
    @SerializedName("unknown")
    UNKNOWN;

    private static final FireMode[] VALUES = values();

    /**
     * Resolves a name that came from item NBT, which can hold anything — an old save, a
     * {@code /data} command, another mod. Unlike {@link #valueOf(String)} this does not throw:
     * it is read from the tooltip, the firing path and the tick loop.
     *
     * @return the matching mode, or {@link #UNKNOWN} if the name does not name one
     */
    public static FireMode fromName(String name) {
        for (FireMode mode : VALUES) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return UNKNOWN;
    }
}
