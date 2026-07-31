package cn.sh1rocu.tacz.compat.rei.display;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class GunSmithTableDisplay extends BasicDisplay {
    private final GunSmithTableRecipe recipe;
    private final Map.Entry<Identifier, CategoryIdentifier<GunSmithTableDisplay>> entry;

    public GunSmithTableDisplay(GunSmithTableRecipe recipe, Map.Entry<Identifier, CategoryIdentifier<GunSmithTableDisplay>> entry) {
        // Recipe.getIngredients 没了 —— 配方不再对外报一份扁平的材料表，
        // 我们本来就有自己的 getInputs()，用它就行
        super(recipe.getInputs().stream().map(input -> EntryIngredients.ofIngredient(input.getIngredient())).toList(),
                Collections.singletonList(EntryIngredients.of(recipe.getOutput())), Optional.ofNullable(entry.getKey()));
        this.recipe = recipe;
        this.entry = entry;
    }

    public GunSmithTableRecipe getRecipe() {
        return recipe;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return entry.getValue();
    }

    /**
     * 这些 display 是客户端拿本地配方现搭的，从不从服务端同步过来 —— 这正是 REI 文档里
     * 允许返回 null 的那种情况。
     */
    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
