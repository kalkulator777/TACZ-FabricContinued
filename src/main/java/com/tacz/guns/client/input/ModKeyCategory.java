package com.tacz.guns.client.input;

import com.tacz.guns.GunMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * 键位分类。
 * <p>
 * 1.21.6 起 {@link KeyMapping} 不再接受一个翻译键字符串做分类，而是要一个注册过的
 * {@link KeyMapping.Category}，它的标签来自 id 而不是自选的字符串 ——
 * {@code tacz:guns} 对应 {@code key.category.tacz.guns}，语言文件里的键跟着改了。
 * <p>
 * {@link KeyMapping.Category#register} 重复注册同一个 id 会抛异常，所以这里只有一个字段，
 * 十一个键位共用它。
 */
@Environment(EnvType.CLIENT)
public final class ModKeyCategory {
    public static final KeyMapping.Category TACZ =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "guns"));

    private ModKeyCategory() {
    }
}
