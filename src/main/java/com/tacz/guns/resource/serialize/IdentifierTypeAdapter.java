package com.tacz.guns.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;

/**
 * Gson 适配器，把 {@link Identifier} 读写成一个字符串。
 * <p>
 * 原版自带的 {@code Identifier.Serializer}（当时还叫 {@code ResourceLocation.Serializer}）
 * 在 1.21.11 没有了 —— 原版内部改用 codec 了。枪包的 JSON 走的是 Gson，所以这份还得有。
 */
public class IdentifierTypeAdapter implements JsonDeserializer<Identifier>, JsonSerializer<Identifier> {
    @Override
    public Identifier deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return Identifier.parse(GsonHelper.convertToString(json, "identifier"));
    }

    @Override
    public JsonElement serialize(Identifier id, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(id.toString());
    }
}
