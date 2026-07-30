package com.tacz.guns.compat.playeranimator.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.zigythebird.playeranimcore.easing.EasingType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rewrites keyframe easing names from PlayerAnimator's spelling to the one Player Animation
 * Library uses.
 * <p>
 * The Blockbench exporter that gun packs are authored with writes PlayerAnimator's names —
 * {@code INOUTSINE}, {@code OUTQUAD} and so on. PAL calls the same curves {@code easeinoutsine}
 * and {@code easeoutquad}, and {@link EasingType#fromString} answers {@code LINEAR} for anything
 * it does not recognise, without logging. Handing a pack straight over would therefore turn every
 * keyframe in it linear — no crash, no warning, just animations that no longer look like what
 * their author made. The three built-in animations alone have 2619 keyframes and every one of
 * them names an easing this way.
 * <p>
 * The table is derived from {@link EasingType#values()} rather than written out, so a curve PAL
 * adds later is covered without anyone remembering to come back here.
 */
public final class EasingNames {
    /**
     * PlayerAnimator's name, lowercased, to PAL's name. Only entries PAL would otherwise miss:
     * {@code linear}, {@code constant}, {@code step} and {@code catmullrom} are spelled the same
     * in both and never reach this map.
     */
    private static final Map<String, String> ALIASES = buildAliases();

    private EasingNames() {
    }

    private static Map<String, String> buildAliases() {
        Map<String, String> aliases = new HashMap<>();
        for (EasingType type : EasingType.values()) {
            String name = type.name.toLowerCase(Locale.ROOT);
            // easeinoutsine -> inoutsine, which is exactly how PlayerAnimator spells it
            if (name.startsWith("ease")) {
                aliases.putIfAbsent(name.substring("ease".length()), name);
            }
        }
        return Map.copyOf(aliases);
    }

    /**
     * @return PAL's name for this easing, or the input unchanged if it is already one PAL knows
     * or one neither library has heard of
     */
    public static String translate(String easing) {
        return ALIASES.getOrDefault(easing.toLowerCase(Locale.ROOT), easing);
    }

    /**
     * Rewrites every easing name in a parsed animation file, in place.
     * <p>
     * Both spellings of the key are handled: Bedrock keyframes say {@code lerp_mode}, and the
     * library's own format says {@code easing}.
     */
    public static void translateInPlace(JsonElement element) {
        if (element instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (isEasingKey(entry.getKey()) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    entry.setValue(new JsonPrimitive(translate(value.getAsString())));
                } else {
                    translateInPlace(value);
                }
            }
        } else if (element instanceof JsonArray array) {
            for (JsonElement value : array) {
                translateInPlace(value);
            }
        }
    }

    private static boolean isEasingKey(String key) {
        return "lerp_mode".equals(key) || "easing".equals(key);
    }
}
