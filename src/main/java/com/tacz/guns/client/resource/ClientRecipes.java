package com.tacz.guns.client.resource;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * 客户端这边的工作台配方。
 * <p>
 * 1.21.2 起客户端不再拿到完整的配方表：{@code level.recipeAccess()} 只有配方书用得上的
 * 那点东西，按类型列举和按 id 取都没了。工作台界面两样都要，所以配方靠
 * {@link net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization} 单独同步过来，
 * 从这里取。
 */
@Environment(EnvType.CLIENT)
public final class ClientRecipes {
    private ClientRecipes() {
    }

    @Nullable
    private static SynchronizedRecipes recipes() {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.recipeAccess().getSynchronizedRecipes();
    }

    public static Collection<RecipeHolder<GunSmithTableRecipe>> getAll() {
        SynchronizedRecipes recipes = recipes();
        return recipes == null ? List.of() : recipes.getAllOfType(ModRecipe.GUN_SMITH_TABLE_CRAFTING);
    }

    @Nullable
    public static RecipeHolder<GunSmithTableRecipe> get(Identifier recipeId) {
        SynchronizedRecipes recipes = recipes();
        if (recipes == null) {
            return null;
        }
        return recipes.get(ModRecipe.GUN_SMITH_TABLE_CRAFTING, ResourceKey.create(Registries.RECIPE, recipeId));
    }
}
