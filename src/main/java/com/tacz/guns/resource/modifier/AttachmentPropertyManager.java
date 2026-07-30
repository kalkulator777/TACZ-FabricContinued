package com.tacz.guns.resource.modifier;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.event.ChangeGunPropertyEvent;
import com.tacz.guns.resource.modifier.custom.*;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.util.LuaSandbox;
import org.apache.commons.lang3.StringUtils;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttachmentPropertyManager {
    /**
     * Modifier scripts come from gun packs, so they run in a sandbox — see {@link LuaSandbox}.
     * One evaluator per thread: the script communicates through globals, which is not safe to share
     * between the client and the server thread.
     */
    private static final ThreadLocal<ModifierEvaluator> EVALUATOR = ThreadLocal.withInitial(ModifierEvaluator::new);
    private static final Map<String, IAttachmentModifier<?, ?>> MODIFIERS = Maps.newLinkedHashMap();

    public static void registerModifier() {
        MODIFIERS.put(AdsModifier.ID, new AdsModifier());
        MODIFIERS.put(AmmoSpeedModifier.ID, new AmmoSpeedModifier());
        MODIFIERS.put(ArmorIgnoreModifier.ID, new ArmorIgnoreModifier());
        MODIFIERS.put(DamageModifier.ID, new DamageModifier());
        MODIFIERS.put(EffectiveRangeModifier.ID, new EffectiveRangeModifier());
        MODIFIERS.put(ExplosionModifier.ID, new ExplosionModifier());
        MODIFIERS.put(HeadShotModifier.ID, new HeadShotModifier());
        MODIFIERS.put(IgniteModifier.ID, new IgniteModifier());
        MODIFIERS.put(InaccuracyModifier.ID, new InaccuracyModifier());
        MODIFIERS.put(KnockbackModifier.ID, new KnockbackModifier());
        MODIFIERS.put(PierceModifier.ID, new PierceModifier());
        MODIFIERS.put(RecoilModifier.ID, new RecoilModifier());
        MODIFIERS.put(RpmModifier.ID, new RpmModifier());
        MODIFIERS.put(SilenceModifier.ID, new SilenceModifier());
        MODIFIERS.put(WeightModifier.ID, new WeightModifier());
        MODIFIERS.put(ExtraMovementModifier.ID, new ExtraMovementModifier());
    }

    public static Map<String, IAttachmentModifier<?, ?>> getModifiers() {
        return MODIFIERS;
    }

    public static void postChangeEvent(LivingEntity shooter, ItemStack gunItem) {
        if (!(gunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        Identifier gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            AttachmentCacheProperty cacheProperty = new AttachmentCacheProperty();
            // 发布事件
            AttachmentPropertyEvent event = new AttachmentPropertyEvent(gunItem, cacheProperty);
            ChangeGunPropertyEvent.internalOnAttachmentPropertyEvent(event);
            event.postEventToKubeJS(event);
            AttachmentPropertyEvent.CALLBACK.invoker().post(event);
            // 让脚本更新缓存
            IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
            ShooterDataHolder dataHolder = operator.getDataHolder();
            GunProperties.allCacheModifiableByScript().forEach((id, property) -> {
                // noinspection rawtypes,unchecked
                iGun.modifyProperty(dataHolder, gunItem, shooter, "modify_cached_property", property.name(), (Class) property.type(), cacheProperty.getCache(property));
            });
            // 更新实体的缓存对象
            operator.updateCacheProperty(cacheProperty);
        });
    }

    public static double eval(Modifier modifier, double defaultValue) {
        return eval(Collections.singletonList(modifier), defaultValue);
    }

    public static double eval(List<Modifier> modifiers, double defaultValue) {
        double addend = defaultValue;
        double percent = 1;
        double multiplier = 1;
        for (Modifier modifier : modifiers) {
            addend += modifier.getAddend();
            percent += modifier.getPercent();
            multiplier *= Math.max(modifier.getMultiplier(), 0f);
        }
        percent = Math.max(percent, 0f);
        double value = addend * percent * multiplier;
        for (Modifier modifier : modifiers) {
            String function = modifier.getFunction();
            if (StringUtils.isEmpty(function)) {
                continue;
            }
            value = functionEval(value, defaultValue, function);
        }
        return value;
    }

    public static boolean eval(List<Boolean> modified, boolean defaultValue) {
        if (defaultValue) {
            // 如果默认值为 true，那么只要有一个 false 就返回 false
            return modified.stream().allMatch(s -> s);
        } else {
            // 如果默认值为 false，那么只要有一个 true 就返回 true
            return modified.stream().anyMatch(s -> s);
        }
    }

    public static double functionEval(double value, double defaultValue, String script) {
        return EVALUATOR.get().eval(value, defaultValue, script.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Evaluates the {@code function} field of an attachment modifier. The script reads {@code x}
     * (the value accumulated so far) and {@code r} (the unmodified default) and writes {@code y}.
     */
    private static final class ModifierEvaluator {
        private final Globals globals = LuaSandbox.createGlobals();
        /**
         * Compiled chunks, keyed by source. This runs per shot for properties such as recoil, and the
         * set of distinct scripts is bounded by the loaded gun packs.
         */
        private final Map<String, LuaValue> compiled = new HashMap<>();

        private double eval(double value, double defaultValue, String script) {
            LuaValue chunk = compiled.computeIfAbsent(script, source -> {
                try {
                    return globals.load(source, "attachment_modifier");
                } catch (LuaError e) {
                    GunMod.LOGGER.error("Failed to compile attachment modifier script: {}", source, e);
                    return LuaValue.NIL;
                }
            });
            if (chunk.isnil()) {
                return value;
            }
            globals.set("x", LuaValue.valueOf(value));
            globals.set("r", LuaValue.valueOf(defaultValue));
            // Clear the output, otherwise a script that fails halfway returns the previous call's result
            globals.set("y", LuaValue.NIL);
            try {
                chunk.call();
            } catch (LuaError e) {
                GunMod.LOGGER.error("Failed to run attachment modifier script: {}", script, e);
                return value;
            }
            LuaValue result = globals.get("y");
            return result.isnumber() ? result.todouble() : value;
        }
    }
}
