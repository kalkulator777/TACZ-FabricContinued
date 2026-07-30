package com.tacz.guns.crafting;

import com.mojang.serialization.MapCodec;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public class GunSmithTableRecipe implements Recipe<SmithingRecipeInput> {
    public static final MapCodec<GunSmithTableRecipe> CODEC = TableRecipe.CODEC.xmap(
            GunSmithTableRecipe::new,
            recipe -> new TableRecipe(recipe.inputs, recipe.result)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> STREAM_CODEC = TableRecipe.STREAM_CODEC.map(
            GunSmithTableRecipe::new,
            recipe -> new TableRecipe(recipe.inputs, recipe.result)
    );

    private final GunSmithTableResult result;
    private final List<GunSmithTableIngredient> inputs;

    public GunSmithTableRecipe(GunSmithTableResult result, List<GunSmithTableIngredient> inputs) {
        this.result = result;
        this.inputs = inputs;
    }

    public GunSmithTableRecipe(TableRecipe tableRecipe) {
        this(tableRecipe.getResult(), tableRecipe.getMaterials());
    }

    @Override
    @Deprecated
    public boolean matches(SmithingRecipeInput smithingRecipeInput, Level level) {
        return false;
    }

    @Override
    @Deprecated
    public ItemStack assemble(SmithingRecipeInput smithingRecipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    /**
     * 工作台有自己的界面，这些配方从来不进原版配方书，所以位置信息是「不可摆放」，
     * 分类是本模组自己注册的一个 —— 配方书只显示它认识的分类，注册一个自己的就等于不显示。
     * 1.21.2 起这两个方法是 Recipe 的抽象方法，不实现不行。
     */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipe.GUN_SMITH_TABLE_CATEGORY;
    }

    @Override
    public RecipeSerializer<GunSmithTableRecipe> getSerializer() {
        return ModRecipe.GUN_SMITH_TABLE_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeType<GunSmithTableRecipe> getType() {
        return ModRecipe.GUN_SMITH_TABLE_CRAFTING;
    }

    public ItemStack getOutput() {
        return result.getResult();
    }

    public List<GunSmithTableIngredient> getInputs() {
        return inputs;
    }

    public GunSmithTableResult getResult() {
        return result;
    }

    public void init(HolderLookup.Provider provider) {
        result.init(provider);
    }

    public Identifier getTab() {
        return result.getGroup();
    }
}