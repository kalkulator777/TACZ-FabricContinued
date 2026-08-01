package com.tacz.guns.inventory;

import net.minecraft.world.Container;
import cn.sh1rocu.tacz.util.EntityInventory;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonBlockIndex;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class GunSmithTableMenu extends AbstractContainerMenu {
    public static final MenuType<GunSmithTableMenu> TYPE = new ExtendedScreenHandlerType<>(GunSmithTableMenu::new, Identifier.STREAM_CODEC);

    private final Identifier blockId;
    private final RecipeFilter filter;

    public GunSmithTableMenu(int id, Inventory inventory, @Nullable Identifier resourceLocation) {
        super(TYPE, id);
        this.blockId = resourceLocation;
        this.filter = TimelessAPI.getCommonBlockIndex(getBlockId()).map(CommonBlockIndex::getFilter).orElse(null);
    }

    @Nullable
    public Identifier getBlockId() {
        return blockId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Nullable
    private GunSmithTableRecipe getRecipe(Identifier recipeId, RecipeManager recipeManager) {
        if (!DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) || SyncConfig.ENABLE_TABLE_FILTER.get()) {
            if (filter != null && !filter.contains(recipeId)) {
                return null;
            }
        }
        // byKey 现在要的是 ResourceKey，不是裸 id
        RecipeHolder<?> holder = recipeManager.byKey(ResourceKey.create(Registries.RECIPE, recipeId)).orElse(null);
        if (holder != null && holder.value() instanceof GunSmithTableRecipe gunSmithTableRecipe) {
            boolean flag = TimelessAPI.getCommonBlockIndex(getBlockId()).map(blockIndex -> {
                return blockIndex.getData().getTabs().stream().noneMatch(tab -> tab.id().equals(gunSmithTableRecipe.getTab()));
            }).orElse(true);
            if (DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
                flag = false;
            }
            if (flag) {
                return null;
            }
            return gunSmithTableRecipe;
        }
        return null;
    }

    public void doCraft(Identifier recipeId, Player player) {
        /* Level 只给出 RecipeAccess，按 id 取配方要服务端的 RecipeManager。
         * 这个方法本来就只在服务端跑 —— 下面就是发实体和扣材料。*/
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        GunSmithTableRecipe recipe = getRecipe(recipeId, serverLevel.getServer().getRecipeManager());
        if (recipe == null) {
            return;
        }
        {
            Container handler = EntityInventory.of(player);
            // 是创造模式，就不扣材料
            if (!player.isCreative()) {
                Int2IntArrayMap recordCount = new Int2IntArrayMap();
                List<GunSmithTableIngredient> ingredients = recipe.getInputs();

                /* recordCount 是所有材料共用的，键只有槽位号，所以每种材料都必须按「这个槽
                 * 还剩多少没被别的材料占掉」来算，占用要累加而不是覆盖。原来是直接 put，
                 * 后一种材料把前一种的占用抹掉：配方要 10 个铁锭再加 1 个 #c:ingots/iron，
                 * 而玩家的铁都在同一个槽里，两次检查都通过，最后只扣 1 个。*/
                for (GunSmithTableIngredient ingredient : ingredients) {
                    int need = ingredient.getCount();
                    for (int slotIndex = 0; slotIndex < handler.getContainerSize() && need > 0; slotIndex++) {
                        ItemStack stack = handler.getItem(slotIndex);
                        if (stack.isEmpty() || !ingredient.getIngredient().test(stack)) {
                            continue;
                        }
                        int available = stack.getCount() - recordCount.get(slotIndex);
                        if (available <= 0) {
                            continue;
                        }
                        int take = Math.min(available, need);
                        recordCount.put(slotIndex, recordCount.get(slotIndex) + take);
                        need -= take;
                    }
                    // 数量不够，不执行后续逻辑，合成失败
                    if (need > 0) {
                        return;
                    }
                }

                // 开始扣材料
                for (int slotIndex : recordCount.keySet()) {
                    handler.removeItem(slotIndex, recordCount.get(slotIndex));
                }
            }

            // 给玩家对应的物品
            Level level = player.level();
            if (!level.isClientSide()) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), recipe.getOutput().copy());
                itemEntity.setPickUpDelay(0);
                level.addFreshEntity(itemEntity);
            }
            // 更新，否则客户端显示不正确
            player.inventoryMenu.broadcastFullState();
            if (player instanceof ServerPlayer serverPlayer)
                NetworkHandler.sendToClientPlayer(new ServerMessageCraft(this.containerId), serverPlayer);
        }
    }
}
