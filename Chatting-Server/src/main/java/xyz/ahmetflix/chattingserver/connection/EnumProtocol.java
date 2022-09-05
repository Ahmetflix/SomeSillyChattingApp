package xyz.ahmetflix.chattingserver.connection;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.LogManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.*;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.*;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInPing;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInStart;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutPong;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutServerInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public enum EnumProtocol {

    HANDSHAKING(-1) {
        {
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketHandshakingInSetProtocol.class,
                    PacketHandshakingInSetProtocol::new);
        }
    },
    PLAY(0) {
        {
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutKeepAlive.class,
                    PacketPlayOutKeepAlive::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutLogin.class, PacketPlayOutLogin::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutChat.class, PacketPlayOutChat::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutUserInfo.class,
                    PacketPlayOutUserInfo::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutTabComplete.class,
                    PacketPlayOutTabComplete::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutKickDisconnect.class,
                    PacketPlayOutKickDisconnect::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSetCompression.class,
                    PacketPlayOutSetCompression::new);
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketPlayInKeepAlive.class,
                    PacketPlayInKeepAlive::new);
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketPlayInChat.class, PacketPlayInChat::new);
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketPlayInTabComplete.class,
                    PacketPlayInTabComplete::new);

        }
    },
    STATUS(1) {
        {
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketStatusInStart.class, PacketStatusInStart::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketStatusOutServerInfo.class,
                    PacketStatusOutServerInfo::new);
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketStatusInPing.class, PacketStatusInPing::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketStatusOutPong.class, PacketStatusOutPong::new);
        }
    },
    LOGIN(2) {
        {
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutDisconnect.class,
                    PacketLoginOutDisconnect::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutEncryptionBegin.class,
                    PacketLoginOutEncryptionBegin::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutSuccess.class,
                    PacketLoginOutSuccess::new);
            this.registerPacket(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutSetCompression.class,
                    PacketLoginOutSetCompression::new);
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketLoginInStart.class, PacketLoginInStart::new);
            this.registerPacket(EnumProtocolDirection.SERVERBOUND, PacketLoginInEncryptionBegin.class,
                    PacketLoginInEncryptionBegin::new);
        }
    };

    private static final int handshakeId = -1;
    private static final int loginId = 2;

    private static final EnumProtocol[] STATES = new EnumProtocol[loginId - handshakeId + 1]; // 4

    private static final Map<Class<? extends Packet<?>>, EnumProtocol> packetClass2State = Maps.newHashMap();

    private final Object2IntMap<Class<? extends Packet<?>>> packetClassToId = new Object2IntOpenHashMap<>(16, 0.5f);
    private final Map<EnumProtocolDirection, IntObjectMap<Supplier<Packet<?>>>> packetMap = Maps
            .newEnumMap(EnumProtocolDirection.class);

    private final int stateId;

    EnumProtocol(int stateId) {
        this.stateId = stateId;
    }

    public int getStateId() {
        return this.stateId;
    }

    protected void registerPacket(EnumProtocolDirection dir, Class<? extends Packet<?>> clazz, Supplier<Packet<?>> packet) {
        IntObjectMap<Supplier<Packet<?>>> map = this.packetMap.computeIfAbsent(dir, k -> new IntObjectHashMap<>(16, 0.5f));
        int packetId = map.size();
        this.packetClassToId.put(clazz, packetId);
        map.put(packetId, packet);
    }

    public Packet<?> createPacket(EnumProtocolDirection direction, int packetId) {
        Supplier<Packet<?>> packet = this.packetMap.get(direction).get(packetId);
        return packet == null ? null : packet.get();
    }

    public Integer getPacketIdForPacket(Packet<?> packet) {
        return this.packetClassToId.getInt(packet.getClass());
    }

    public static EnumProtocol getProtocolForPacket(Packet<?> packet) {
        return packetClass2State.get(packet.getClass());
    }

    public static EnumProtocol isValidIntention(int state) {
        return state >= handshakeId && state <= loginId ? STATES[state - handshakeId] : null;
    }

    static {
        for (EnumProtocol state : values()) {
            int id = state.getStateId();
            if (id < handshakeId || id > loginId) {
                throw new Error("Invalid protocol ID " + id);
            }
            STATES[id - handshakeId] = state;
            for (Class<? extends Packet<?>> packetClass : state.packetClassToId.keySet()) {
                packetClass2State.put(packetClass, state);
            }
        }
    }
}
