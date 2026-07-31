package cn.sh1rocu.tacz.mixin.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.init.ModRecipe;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 枪包里的配方把自定义材料写成 {@code {"item": {"type": "..."}}}，而 Fabric 的自定义材料
 * 编解码器认的键是 {@code fabric:type}，所以要在解析成配方之前把它补上。
 * <p>
 * 以前这件事挂在 {@code RecipeManager.apply} 上，那时候它收的还是一张
 * {@code Map<Identifier, JsonElement>}。1.21.2 之后 prepare 里扫目录和解码是一步完成的，
 * 原始 JSON 只在 {@code scanDirectory} 里露过一面，所以钩子搬到了那里。这个方法是所有数据
 * 文件共用的，但下面那串判断只认我们自己的配方类型，别的东西一概原样放行。
 */
@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class RecipeManagerMixin {
    @ModifyExpressionValue(method = "scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StrictJsonParser;parse(Ljava/io/Reader;)Lcom/google/gson/JsonElement;"))
    private static JsonElement tacz$addFabricIngredientType(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("materials") && obj.has("type") && obj.get("type").getAsString().equals(ModRecipe.GUN_SMITH_TABLE_CRAFTING.toString())) {
                JsonArray materials = obj.getAsJsonArray("materials");
                materials.forEach(material -> {
                    if (material.isJsonObject()) {
                        JsonElement item = material.getAsJsonObject().get("item");
                        if (item.isJsonObject() && item.getAsJsonObject().has("type")) {
                            JsonObject itemObj = item.getAsJsonObject();
                            itemObj.addProperty(CustomIngredientImpl.TYPE_KEY, itemObj.get("type").getAsString());
                        }
                    }
                });
            }
        }
        return element;
    }
}
