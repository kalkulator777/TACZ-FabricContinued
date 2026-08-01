package com.tacz.guns.network.message.handshake;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.SyncedDataKey;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import com.tacz.guns.network.IHandshakeMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;

public class SyncedEntityDataMappingS2CPacket implements IHandshakeMessage {
    public static final CustomPacketPayload.Type<SyncedEntityDataMappingS2CPacket> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "server_synced_entity_data_mapping")
    );
    public static final StreamCodec<FriendlyByteBuf, SyncedEntityDataMappingS2CPacket> STREAM_CODEC = StreamCodec.of(
            SyncedEntityDataMappingS2CPacket::encode,
            SyncedEntityDataMappingS2CPacket::decode
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final Marker HANDSHAKE = MarkerFactory.getMarker("TACZ_HANDSHAKE");
    /** Far above any real key count; this only has to stop a bogus length from driving the loop. */
    private static final int MAX_KEYS = 65536;
    private Map<Identifier, List<Pair<Identifier, Integer>>> keyMap;

    public SyncedEntityDataMappingS2CPacket() {
        this.keyMap = new HashMap<>();
    }

    private SyncedEntityDataMappingS2CPacket(Map<Identifier, List<Pair<Identifier, Integer>>> keyMap) {
        this.keyMap = keyMap;
    }

    public static void encode(FriendlyByteBuf buffer, SyncedEntityDataMappingS2CPacket message) {
        Set<SyncedDataKey<?, ?>> keys = SyncedEntityData.instance().getKeys();
        buffer.writeInt(keys.size());
        keys.forEach(key -> {
            int id = SyncedEntityData.instance().getInternalId(key);
            buffer.writeIdentifier(key.classKey().id());
            buffer.writeIdentifier(key.id());
            buffer.writeVarInt(id);
        });
    }

    public static SyncedEntityDataMappingS2CPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        // 长度是对面给的，用它开循环之前先看一眼。原版的集合编解码器同样是先夹紧再分配
        if (size < 0 || size > MAX_KEYS) {
            throw new IllegalArgumentException("Synced data key count out of range: " + size);
        }
        Map<Identifier, List<Pair<Identifier, Integer>>> keyMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Identifier classId = buffer.readIdentifier();
            Identifier keyId = buffer.readIdentifier();
            int id = buffer.readVarInt();
            keyMap.computeIfAbsent(classId, c -> new ArrayList<>()).add(Pair.of(keyId, id));
        }
        return new SyncedEntityDataMappingS2CPacket(keyMap);
    }

    @Override
    public void handle(ClientConfigurationNetworking.Context context) {
        GunMod.LOGGER.debug(HANDSHAKE, "Received synced key mappings from server");
        /* 配置阶段的处理器和游戏阶段的不一样，它跑在 netty 的事件循环上，所以原来那个
         * CountDownLatch 是拿网络线程去等主线程排空任务队列。而且等的意义也不存在：
         * HandshakeNetworking.SyncedEntityDataTask.run 发完包就直接 completeTask，服务端
         * 根本不等这个 ack。
         * 应答改到 execute 里发，顺序仍然是「先套用映射，再应答」，只是不再堵住网络线程。
         * 配置阶段结束的那个包同样要经 ensureRunningOnSameThread 排到主线程队列，排在这件
         * 事之后，所以进入游戏阶段之前映射一定已经生效。*/
        context.client().execute(() -> {
            if (!SyncedEntityData.instance().updateMappings(this.keyMap)) {
                context.responseSender().disconnect(Component.literal("Connection closed - [TacZ] Received unknown synced data keys."));
                return;
            }
            context.responseSender().sendPacket(AcknowledgeC2SPacket.INSTANCE);
        });
    }
}
