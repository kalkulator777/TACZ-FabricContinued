package cn.sh1rocu.tacz.compat.rei.display;

import cn.sh1rocu.tacz.compat.rei.REIClientPlugin;
import cn.sh1rocu.tacz.compat.rei.entry.AttachmentQueryEntry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachmentQueryDisplay implements Display {
    private final AttachmentQueryEntry entry;

    public AttachmentQueryDisplay(AttachmentQueryEntry entry) {
        this.entry = entry;
    }

    public AttachmentQueryEntry getEntry() {
        return entry;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        List<EntryIngredient> inputs = new ArrayList<>();
        entry.getAllowGunStacks().forEach(gun -> inputs.add(EntryIngredients.of(gun)));
        if (!entry.getExtraAllowGunStacks().isEmpty())
            inputs.add(EntryIngredients.ofItemStacks(entry.getExtraAllowGunStacks()));
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return List.of(EntryIngredients.of(entry.getAttachmentStack()));
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return REIClientPlugin.ATTACHMENT_QUERY;
    }

    /**
     * 配件查询表不是从某个配方来的，没有对应的 id。
     */
    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.empty();
    }

    /**
     * 这些 display 是客户端现搭的，从不从服务端同步过来 —— 这正是 REI 文档里
     * 允许返回 null 的那种情况。
     */
    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
