package com.tacz.guns.loot;

import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.loot.LootTableInjection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.function.Consumer;

public class LootTableInjectorModifier {
    /**
     * 把注入的战利品追加到 output 上。output 由调用方给出，正常路径上它已经是 vanilla 的
     * 分堆消费者，所以注入进去的物品也会正确分堆。
     */
    public static void appendInjections(LootTable table, LootContext context, Consumer<ItemStack> output) {
        CommonAssetsManager manager = CommonAssetsManager.getInstance();
        if (manager == null) {
            return;
        }
        MinecraftServer server = context.getLevel().getServer();
        if (server == null) {
            return;
        }
        /* 注入用的战利品表本身没有注册，这里查不到 id，于是注入表内部再次走到这里时会直接返回。
         * 可重载注册表现在只给出 HolderLookup，没有反查方法，所以只能按引用扫一遍。
         * §7 里那条「战利品表 id 缓存」的记录也是说这里。*/
        Identifier lootTableId = server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE)
                .listElements()
                .filter(holder -> holder.value() == table)
                .findFirst()
                .map(holder -> holder.key().identifier())
                .orElse(null);
        if (lootTableId == null) {
            return;
        }

        List<LootTableInjection> injections = manager.getLootTableInjections(lootTableId);
        if (injections.isEmpty()) {
            return;
        }

        for (LootTableInjection injection : injections) {
            injection.createStacks(context).forEach(output);
        }
    }
}
