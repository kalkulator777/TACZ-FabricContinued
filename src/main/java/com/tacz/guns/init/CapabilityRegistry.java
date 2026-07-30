package com.tacz.guns.init;

import com.tacz.guns.entity.sync.core.DataHolderCapabilityProvider;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

public class CapabilityRegistry implements EntityComponentInitializer {
    @Override
    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(Entity.class, DataHolderCapabilityProvider.CAPABILITY)
                .filter(SyncedEntityData.instance()::hasSyncedDataKey)
                .end(DataHolderCapabilityProvider::new);
    }

    public static void init() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (!(entity instanceof ServerPlayer)) {
                DataHolderCapabilityProvider.CAPABILITY.maybeGet(entity).ifPresent(DataHolderCapabilityProvider::invalidate);
            }
        });
    }
}