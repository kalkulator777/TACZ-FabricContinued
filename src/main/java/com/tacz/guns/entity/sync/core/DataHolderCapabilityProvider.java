package com.tacz.guns.entity.sync.core;

import com.tacz.guns.GunMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import java.util.Optional;

public class DataHolderCapabilityProvider implements Component {
    public static final ComponentKey<DataHolderCapabilityProvider> CAPABILITY = ComponentRegistry.getOrCreate(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "synced_entity_data"), DataHolderCapabilityProvider.class);
    private static final String DATA_KEY = "DataHolder";
    private static final String ENTRIES_KEY = "Entries";
    private final DataHolder holder = new DataHolder();
    /**
     * 只用来在写出时拿到注册表 —— {@link ValueOutput} 不像 {@link ValueInput} 那样带 lookup()。
     */
    private final Entity entity;

    public DataHolderCapabilityProvider(Entity entity) {
        this.entity = entity;
    }
    /**
     * Cleared when the entity leaves the world, so its data is no longer handed out.
     */
    private boolean valid = true;

    public void invalidate() {
        this.valid = false;
    }

    public Optional<DataHolder> getDataHolder() {
        return this.valid ? Optional.of(this.holder) : Optional.empty();
    }

    private ListTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        ListTag list = new ListTag();
        this.holder.dataMap.forEach((key, entry) -> {
            if (key.save()) {
                CompoundTag keyTag = new CompoundTag();
                keyTag.putString("ClassKey", key.classKey().id().toString());
                keyTag.putString("DataKey", key.id().toString());
                keyTag.put("Value", entry.writeValue(provider));
                list.add(keyTag);
            }
        });
        return list;
    }

    private void deserializeNBT(HolderLookup.@NotNull Provider provider, ListTag listTag) {
        this.holder.dataMap.clear();
        listTag.forEach(entryTag -> {
            CompoundTag keyTag = (CompoundTag) entryTag;
            Identifier classKey = Identifier.tryParse(keyTag.getStringOr("ClassKey", ""));
            Identifier dataKey = Identifier.tryParse(keyTag.getStringOr("DataKey", ""));
            Tag value = keyTag.get("Value");
            SyncedClassKey<?> syncedClassKey = SyncedEntityData.instance().getClassKey(classKey);
            if (syncedClassKey == null) {
                return;
            }
            SyncedDataKey<?, ?> syncedDataKey = SyncedEntityData.instance().getKey(syncedClassKey, dataKey);
            if (syncedDataKey == null || !syncedDataKey.save()) {
                return;
            }
            DataEntry<?, ?> entry = new DataEntry<>(syncedDataKey);
            entry.readValue(provider, value);
            this.holder.dataMap.put(syncedDataKey, entry);
        });
    }

    /* CCA 跟着原版走，把组件的读写换成了 ValueInput/ValueOutput，它们只认 codec，
     * 而这里每种数据的读写是各自的 IDataSerializer，给出的是裸 Tag。所以整份数据仍然
     * 按原来的方式序列化成一个 ListTag，再包一层 CompoundTag 用 CompoundTag.CODEC 存进去。
     * 存档里的形状因此多了一层，读的一侧也照此。*/
    @Override
    public void readData(@NotNull ValueInput input) {
        input.read(DATA_KEY, CompoundTag.CODEC)
                .ifPresent(wrapper -> deserializeNBT(input.lookup(), wrapper.getListOrEmpty(ENTRIES_KEY)));
    }

    @Override
    public void writeData(@NotNull ValueOutput output) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(ENTRIES_KEY, serializeNBT(this.entity.registryAccess()));
        output.store(DATA_KEY, CompoundTag.CODEC, wrapper);
    }
}
