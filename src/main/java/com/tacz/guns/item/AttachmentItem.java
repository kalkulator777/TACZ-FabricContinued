package com.tacz.guns.item;

import cn.sh1rocu.tacz.api.extension.IItem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.renderer.item.AttachmentItemRenderer;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.tacz.guns.api.client.renderer.IDynamicItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import com.tacz.guns.util.datafixer.AttachmentIdFix;

import static com.tacz.guns.util.datafixer.AttachmentIdFix.updateAttachmentIdInTag;

public class AttachmentItem extends Item implements AttachmentItemDataAccessor, IItem {
    public AttachmentItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    @Nonnull
    @Environment(EnvType.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        Identifier attachmentId = this.getAttachmentId(stack);
        Optional<ClientAttachmentIndex> attachmentIndex = TimelessAPI.getClientAttachmentIndex(attachmentId);
        if (attachmentIndex.isPresent()) {
            return Component.translatable(attachmentIndex.get().getName());
        }
        return super.getName(stack);
    }

    private static Comparator<Map.Entry<Identifier, CommonAttachmentIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    public static NonNullList<ItemStack> fillItemCategory(AttachmentType type) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonAttachmentIndex().stream().sorted(idNameSort()).forEach(entry -> {
            if (entry.getValue().getPojo().isHidden()) {
                return;
            }
            if (type.equals(entry.getValue().getType())) {
                ItemStack itemStack = AttachmentItemBuilder.create().setId(entry.getKey()).build();
                stacks.add(itemStack);
            }
        });
        return stacks;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public IDynamicItemRenderer getCustomRenderer() {
        return AttachmentItemRenderer.INSTANCE.get();
    }

    @Override
    @Nonnull
    public AttachmentType getType(ItemStack attachmentStack) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (iAttachment != null) {
            Identifier id = iAttachment.getAttachmentId(attachmentStack);
            return TimelessAPI.getCommonAttachmentIndex(id).map(CommonAttachmentIndex::getType).orElse(AttachmentType.NONE);
        } else {
            return AttachmentType.NONE;
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new AttachmentItemTooltip(this.getAttachmentId(stack), this.getType(stack), stack));
    }

    /**
     * 把 1.20.1 时代的旧配件 id 改写成现在的。同 {@code AmmoItem}：
     * {@code Item.verifyComponentsAfterLoad} 没有了，暂时挂在 inventoryTick 上，
     * 只有 id 确实需要改写时才动 NBT。归宿是三阶段那个 datafixer。
     */
    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull ServerLevel level, @Nonnull Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (AttachmentIdFix.needsUpdate(this.getAttachmentId(stack))) {
            stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(this::verifyTagAfterLoad));
        }
    }

    public void verifyTagAfterLoad(@NotNull CompoundTag tag) {
        updateAttachmentIdInTag(tag);
    }
}
