package cn.sh1rocu.tacz.compat.rei;

import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.init.ModItems;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

/**
 * registerItemComparators 挂在 REICommonPlugin 上，不在裸的 REIPlugin 上 ——
 * 后者只剩插件本身的生命周期。getPluginProviderClass 也由它给了默认实现。
 */
public class REIPlugin implements REICommonPlugin {
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register(REISubtype.getAmmoSubtype(), ModItems.AMMO);
        registry.register(REISubtype.getAttachmentSubtype(), ModItems.ATTACHMENT);
        registry.register(REISubtype.getAmmoBoxSubtype(), ModItems.AMMO_BOX);
        GunItemManager.getAllGunItems().forEach(item ->
                registry.register(REISubtype.getGunSubtype(), item));
    }
}
