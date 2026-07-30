package com.tacz.guns.api.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.ApiStatus;

/**
 * 一个简单的NBT包装，用于在Lua中访问NBT数据。<br/>
 * 暂时只支持基本数据类型的读写，不支持数组等复杂数据类型。
 */
@SuppressWarnings("unused")
public record LuaNbtAccessor(CompoundTag nbt) {

    public static LuaNbtAccessor from(ItemStack stack) {
        return new LuaNbtAccessor(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    public static LuaNbtAccessor from(CompoundTag nbt) {
        return new LuaNbtAccessor(nbt);
    }

    public boolean contains(String key) {
        return nbt.contains(key);
    }

    /**
     * 1.21.5 起 CompoundTag.contains 不再接受类型参数，但这个方法是暴露给 Lua 脚本的，
     * 所以按原来的语义自己实现：类型 99（TAG_ANY_NUMERIC）匹配任意数字标签。
     */
    public boolean contains(String key, int type) {
        Tag tag = nbt.get(key);
        if (tag == null) {
            return false;
        }
        if (type == 99) {
            return tag instanceof NumericTag;
        }
        return tag.getId() == type;
    }

    public LuaNbtAccessor newCompoundTag() {
        return new LuaNbtAccessor(new CompoundTag());
    }

    public int getInt(String key) {
        return nbt.getIntOr(key, 0);
    }

    public double getDouble(String key) {
        return nbt.getDoubleOr(key, 0.0);
    }

    public float getFloat(String key) {
        return nbt.getFloatOr(key, 0.0F);
    }

    public long getLong(String key) {
        return nbt.getLongOr(key, 0L);
    }

    public String getString(String key) {
        return nbt.getStringOr(key, "");
    }

    public boolean getBoolean(CompoundTag nbt, String key) {
        return nbt.getBooleanOr(key, false);
    }

    public LuaNbtAccessor getCompound(String key) {
        return nbt.getCompound(key).map(LuaNbtAccessor::from).orElse(null);
    }

    public void putInt(String key, int value) {
        nbt.putInt(key, value);
    }

    public void putDouble(String key, double value) {
        nbt.putDouble(key, value);
    }

    public void putFloat(String key, float value) {
        nbt.putFloat(key, value);
    }

    public void putLong(String key, long value) {
        nbt.putLong(key, value);
    }

    public void putString(String key, String value) {
        nbt.putString(key, value);
    }

    public void putBoolean(String key, boolean value) {
        nbt.putBoolean(key, value);
    }

    /**
     * 向当前的NbtCompound中添加一个新的Compound
     *
     * @param key   键
     * @param value 在脚本中请使用{@link LuaNbtAccessor#newCompoundTag()}创建一个新的LuaNbtAccessor对象
     */
    public void putCompound(String key, LuaNbtAccessor value) {
        if (value != null) {
            nbt.put(key, value.nbt());
        }
    }

    @Override
    @ApiStatus.Internal
    public CompoundTag nbt() {
        return nbt;
    }
}
