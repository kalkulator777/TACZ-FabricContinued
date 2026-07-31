package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class ModRecipe {
    public static void init() {
        // 1.21.2 起客户端不再收到完整的配方数据 —— RecipeAccess 只给配方书要的那点东西，
        // 而工作台界面需要把所有配方列出来。Fabric 的这个开关把配方按序列化器同步回客户端。
        RecipeSynchronization.synchronizeRecipeSerializer(GUN_SMITH_TABLE_RECIPE_SERIALIZER);
    }

    public static RecipeSerializer<GunSmithTableRecipe> GUN_SMITH_TABLE_RECIPE_SERIALIZER = registerSerializer("gun_smith_table_crafting", new GunSmithTableSerializer());
    /**
     * 配方书分类在 1.21.2 变成了注册表对象，而 Recipe 必须给出一个。工作台配方不进配方书，
     * 注册一个自己的分类正好达到这个效果：配方书只画它自己那份分类列表里的东西。
     */
    public static RecipeBookCategory GUN_SMITH_TABLE_CATEGORY = Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY,
            Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "gun_smith_table_crafting"), new RecipeBookCategory());
    public static RecipeType<GunSmithTableRecipe> GUN_SMITH_TABLE_CRAFTING = registerRecipe("gun_smith_table_crafting", new RecipeType<>() {
        @Override
        public String toString() {
            return GunMod.MOD_ID + ":gun_smith_table_crafting";
        }
    });

    private static <S extends RecipeSerializer<T>, T extends Recipe<?>> S registerSerializer(String name, S serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(GunMod.MOD_ID, name), serializer);
    }

    private static <T extends Recipe<?>> RecipeType<T> registerRecipe(String name, RecipeType<T> type) {
        return Registry.register(BuiltInRegistries.RECIPE_TYPE, Identifier.fromNamespaceAndPath(GunMod.MOD_ID, name), type);
    }
}
