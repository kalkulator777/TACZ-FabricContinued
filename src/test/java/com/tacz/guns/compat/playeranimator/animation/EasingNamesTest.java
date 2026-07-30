package com.tacz.guns.compat.playeranimator.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Gun packs are authored with a Blockbench exporter that writes PlayerAnimator's easing names.
 * Player Animation Library uses different ones and answers LINEAR for anything it does not
 * recognise, silently — so without this translation every keyframe in every pack would go linear
 * and nobody would see an error. These tests are what stops that coming back.
 */
class EasingNamesTest {
    @Test
    void theNameTheGunPacksActuallyUseResolvesToTheCurveTheyMean() {
        // 2619 keyframes in the three built-in animations say exactly this
        assertSame(EasingType.EASE_IN_OUT_SINE, EasingType.fromString(EasingNames.translate("INOUTSINE")));
    }

    @Test
    void withoutTranslationThatNameWouldSilentlyBecomeLinear() {
        assertSame(EasingType.LINEAR, EasingType.fromString("INOUTSINE"));
    }

    @Test
    void everyEasingPlayerAnimatorNamesSurvivesTheRoundTrip() {
        // PlayerAnimator's naming is Player Animation Library's minus the "ease" prefix
        for (EasingType type : EasingType.values()) {
            if (!type.name.startsWith("ease")) {
                continue;
            }
            String playerAnimatorName = type.name.substring("ease".length()).toUpperCase(java.util.Locale.ROOT);
            assertSame(type, EasingType.fromString(EasingNames.translate(playerAnimatorName)),
                    playerAnimatorName + " must still resolve to " + type.name);
        }
    }

    @Test
    void namesBothLibrariesSpellTheSameArePassedThrough() {
        assertSame(EasingType.LINEAR, EasingType.fromString(EasingNames.translate("linear")));
        assertSame(EasingType.CONSTANT, EasingType.fromString(EasingNames.translate("constant")));
        assertSame(EasingType.STEP, EasingType.fromString(EasingNames.translate("step")));
        assertSame(EasingType.CATMULLROM, EasingType.fromString(EasingNames.translate("catmullrom")));
    }

    @Test
    void aNameNeitherLibraryKnowsIsLeftAlone() {
        assertEquals("nonsense", EasingNames.translate("nonsense"));
    }

    @Test
    void bothSpellingsOfTheKeyAreRewrittenAtEveryDepth() {
        JsonElement json = JsonParser.parseString("""
                {
                  "animations": {
                    "hold_upper": {
                      "bones": {
                        "right_arm": {
                          "rotation": {
                            "0.0": {"post": [1, 2, 3], "lerp_mode": "INOUTSINE"},
                            "0.5": {"post": [4, 5, 6], "easing": "OUTQUAD"}
                          }
                        }
                      }
                    }
                  }
                }
                """);
        EasingNames.translateInPlace(json);

        var rotation = json.getAsJsonObject()
                .getAsJsonObject("animations").getAsJsonObject("hold_upper")
                .getAsJsonObject("bones").getAsJsonObject("right_arm")
                .getAsJsonObject("rotation");
        assertEquals("easeinoutsine", rotation.getAsJsonObject("0.0").get("lerp_mode").getAsString());
        assertEquals("easeoutquad", rotation.getAsJsonObject("0.5").get("easing").getAsString());
    }

    /**
     * The whole chain against a file that actually ships, rather than a snippet: the built-in
     * rifle animations parse under Player Animation Library and come out named.
     */
    @Test
    void theBuiltInAnimationsLoad() throws Exception {
        Path file = Path.of("src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz",
                "player_animator/rifle_default.player_animation.json");
        assumeTrue(Files.isRegularFile(file), "run from the project root");

        JsonElement json = JsonParser.parseString(Files.readString(file));
        EasingNames.translateInPlace(json);
        Map<String, Animation> animations = UniversalAnimLoader.loadAnimations(json.getAsJsonObject());

        assertTrue(animations.containsKey("hold_upper"), "expected the animations to be keyed by name, got " + animations.keySet());
        assertTrue(animations.size() > 1, "expected more than one animation in the file");
    }

    @Test
    void nothingElseInTheFileIsTouched() {
        JsonElement json = JsonParser.parseString("""
                {"format_version": "1.8.0", "name": "INOUTSINE", "values": ["INOUTSINE"]}
                """);
        EasingNames.translateInPlace(json);

        var object = json.getAsJsonObject();
        assertEquals("1.8.0", object.get("format_version").getAsString());
        assertEquals("INOUTSINE", object.get("name").getAsString(), "only easing keys are rewritten");
        assertEquals("INOUTSINE", object.getAsJsonArray("values").get(0).getAsString());
    }
}
