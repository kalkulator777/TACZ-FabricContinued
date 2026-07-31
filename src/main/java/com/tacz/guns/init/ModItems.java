package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
    public static void init() {
        GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
    }

    public static ModernKineticGunItem MODERN_KINETIC_GUN = register("modern_kinetic_gun", ModernKineticGunItem::new, stackOfOne());

//    public static ThrowableItem M67 = register("m67", ThrowableItem::new, stackOfOne());

    public static Item AMMO = register("ammo", AmmoItem::new, stackOfOne());
    public static AttachmentItem ATTACHMENT = register("attachment", AttachmentItem::new, stackOfOne());

    public static GunSmithTableItem GUN_SMITH_TABLE = register("gun_smith_table", properties -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE, properties), stackOfOne());
    public static GunSmithTableItem WORKBENCH_111 = register("workbench_a", properties -> new GunSmithTableItem(ModBlocks.WORKBENCH_111, properties), stackOfOne());
    public static GunSmithTableItem WORKBENCH_211 = register("workbench_b", properties -> new GunSmithTableItem(ModBlocks.WORKBENCH_211, properties), stackOfOne());
    public static GunSmithTableItem WORKBENCH_121 = register("workbench_c", properties -> new GunSmithTableItem(ModBlocks.WORKBENCH_121, properties), stackOfOne());


    public static Item TARGET = register("target", properties -> new BlockItem(ModBlocks.TARGET, properties), new Item.Properties());
    public static Item STATUE = register("statue", properties -> new BlockItem(ModBlocks.STATUE, properties), new Item.Properties());
    public static Item AMMO_BOX = register("ammo_box", AmmoBoxItem::new, stackOfOne());
    public static Item TARGET_MINECART = register("target_minecart", TargetMinecartItem::new, stackOfOne());

    private static Item.Properties stackOfOne() {
        return new Item.Properties().stacksTo(1);
    }

    /**
     * 和方块一样：1.21.2 起 Item.Properties 必须先带上注册 id，物品才构造得出来，
     * 所以先有键，再造物品。
     */
    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GunMod.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }
}