package com.tacz.guns.client.model.papi;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class PapiManager {
    private static final Map<String, Function<ItemStack, String>> PAPI = Maps.newHashMap();

    // 注册，不知道放哪里，先放这
    static {
        addPapi(PlayerNamePapi.NAME, new PlayerNamePapi());
        addPapi(AmmoCountPapi.NAME, new AmmoCountPapi());
    }

    public static void addPapi(String textKey, Function<ItemStack, String> function) {
        textKey = "%" + textKey + "%";
        PAPI.put(textKey, function);
    }

    public static String getTextShow(String textKey, ItemStack stack) {
        String text = I18n.language.getOrDefault(textKey);
        for (var entry : PAPI.entrySet()) {
            String placeholder = entry.getKey();
            /* 先看串里有没有这个占位符再去求值。这里每帧按每个文本节点跑一次，而求值不便宜 ——
             * 弹药数那个要查枪械 id、查索引、再读两次弹药数，玩家名那个每次都从 Component
             * 现拼一个 String —— 一个有三个文本节点的枪，为了通常只用到一个的占位符，
             * 每帧要白算六次。*/
            if (!text.contains(placeholder)) {
                continue;
            }
            String data = entry.getValue().apply(stack);
            text = text.replace(placeholder, data);
        }
        return text;
    }
}
