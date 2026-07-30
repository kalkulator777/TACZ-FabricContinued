package com.tacz.guns.compat.playeranimator.animation;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.tacz.guns.GunMod;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PlayerAnimatorAssetManager extends SimplePreparableReloadListener<Map<Identifier, HashMap<String, Animation>>> implements IdentifiableResourceReloadListener {
    private static PlayerAnimatorAssetManager INSTANCE;

    private final FileToIdConverter filetoidconverter = new FileToIdConverter("player_animator", ".json");
    private final HashMap<Identifier, HashMap<String, Animation>> animations = new HashMap<>();

    public static PlayerAnimatorAssetManager get() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerAnimatorAssetManager();
        }
        return INSTANCE;
    }

    void putAnimation(Identifier id, InputStream stream) throws IOException {
        animations.computeIfAbsent(id, k -> Maps.newHashMap()).putAll(read(stream));
    }

    /**
     * Parses one animation file into its animations, keyed by lowercased name.
     * <p>
     * The easing names are rewritten first — see {@link EasingNames} for why that is not
     * optional. The loader handles both the Bedrock form gun packs are exported in and the
     * library's own, and returns them already keyed by name.
     */
    private static Map<String, Animation> read(InputStream stream) throws IOException {
        JsonElement json;
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader);
        }
        if (!(json instanceof JsonObject object)) {
            throw new JsonParseException("Expected a json object, found " + json);
        }
        EasingNames.translateInPlace(object);

        Map<String, Animation> parsed = UniversalAnimLoader.loadAnimations(object);
        Map<String, Animation> byLowercaseName = Maps.newHashMapWithExpectedSize(parsed.size());
        parsed.forEach((name, animation) -> byLowercaseName.put(name.toLowerCase(Locale.ENGLISH), animation));
        return byLowercaseName;
    }

    Optional<Animation> getAnimations(Identifier id, String name) {
        var animationHashMap = this.animations.get(id);
        if (animationHashMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(animationHashMap.get(name));
    }

    public boolean containsKey(Identifier id) {
        return animations.containsKey(id);
    }

    public void clearAll() {
        animations.clear();
    }

    @Override
    protected Map<Identifier, HashMap<String, Animation>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, HashMap<String, Animation>> output = Maps.newHashMap();
        for (Map.Entry<Identifier, Resource> entry : filetoidconverter.listMatchingResources(manager).entrySet()) {
            Identifier file = entry.getKey();
            Identifier id = filetoidconverter.fileToId(file);

            try (InputStream stream = entry.getValue().open()) {
                output.computeIfAbsent(id, k -> Maps.newHashMap()).putAll(read(stream));
            } catch (IOException | RuntimeException e) {
                GunMod.LOGGER.warn("Failed to read player animation file {}", file, e);
            }
        }
        return output;
    }

    @Override
    protected void apply(Map<Identifier, HashMap<String, Animation>> map, ResourceManager manager, ProfilerFiller profiler) {
        animations.clear();
        animations.putAll(map);
    }

    public static final Identifier ID = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pa_asset_manager");

    @Override
    public Identifier getFabricId() {
        return ID;
    }
}
