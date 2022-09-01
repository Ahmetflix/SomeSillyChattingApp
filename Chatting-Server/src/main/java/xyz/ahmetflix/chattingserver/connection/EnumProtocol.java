package xyz.ahmetflix.chattingserver.connection;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;

import java.util.Iterator;
import java.util.Map;

public enum EnumProtocol {
    HANDSHAKING(-1) {
        {
            //this.a(EnumProtocolDirection.SERVERBOUND, PacketHandshakingInSetProtocol.class);
        }
    },
    PLAY(0) {
        {
            /*this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutKeepAlive.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutLogin.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutChat.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutUpdateTime.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityEquipment.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSpawnPosition.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutUpdateHealth.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutRespawn.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutPosition.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutHeldItemSlot.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutBed.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutAnimation.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutNamedEntitySpawn.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutCollect.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSpawnEntity.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSpawnEntityLiving.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSpawnEntityPainting.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSpawnEntityExperienceOrb.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityVelocity.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityDestroy.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntity.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntity.PacketPlayOutRelEntityMove.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntity.PacketPlayOutEntityLook.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityTeleport.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityHeadRotation.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityStatus.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutAttachEntity.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityMetadata.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutEntityEffect.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutRemoveEntityEffect.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutExperience.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutUpdateAttributes.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutMapChunk.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutMultiBlockChange.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutBlockChange.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutBlockAction.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutBlockBreakAnimation.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutMapChunkBulk.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutExplosion.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutWorldEvent.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutNamedSoundEffect.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutWorldParticles.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutGameStateChange.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSpawnEntityWeather.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutOpenWindow.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutCloseWindow.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSetSlot.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutWindowItems.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutWindowData.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutTransaction.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutUpdateSign.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutMap.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutTileEntityData.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutOpenSignEditor.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutStatistic.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutPlayerInfo.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutAbilities.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutTabComplete.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutScoreboardObjective.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutScoreboardScore.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutScoreboardDisplayObjective.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutScoreboardTeam.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutCustomPayload.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutKickDisconnect.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutServerDifficulty.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutCombatEvent.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutCamera.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutWorldBorder.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutTitle.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutSetCompression.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutPlayerListHeaderFooter.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutResourcePackSend.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketPlayOutUpdateEntityNBT.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInKeepAlive.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInChat.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInUseEntity.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInFlying.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInFlying.PacketPlayInPosition.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInFlying.PacketPlayInLook.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInFlying.PacketPlayInPositionLook.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInBlockDig.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInBlockPlace.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInHeldItemSlot.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInArmAnimation.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInEntityAction.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInSteerVehicle.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInCloseWindow.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInWindowClick.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInTransaction.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInSetCreativeSlot.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInEnchantItem.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInUpdateSign.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInAbilities.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInTabComplete.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInSettings.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInClientCommand.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInCustomPayload.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInSpectate.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketPlayInResourcePackStatus.class);*/
        }
    },
    STATUS(1) {
        {
            /*this.a(EnumProtocolDirection.SERVERBOUND, PacketStatusInStart.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketStatusOutServerInfo.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketStatusInPing.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketStatusOutPong.class);*/
        }
    },
    LOGIN(2) {
        {
            /*this.a(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutDisconnect.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutEncryptionBegin.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutSuccess.class);
            this.a(EnumProtocolDirection.CLIENTBOUND, PacketLoginOutSetCompression.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketLoginInStart.class);
            this.a(EnumProtocolDirection.SERVERBOUND, PacketLoginInEncryptionBegin.class);*/
        }
    };

    private static int handshakeState = -1;
    private static int loginState = 2;
    private static final EnumProtocol[] STATES = new EnumProtocol[loginState - handshakeState + 1];
    private static final Map<Class<? extends Packet>, EnumProtocol> STATES_BY_CLASS = Maps.newHashMap();
    private final int id;
    private final Map<EnumProtocolDirection, BiMap<Integer, Class<? extends Packet>>> directionMaps;

    private EnumProtocol(int var3) {
        this.directionMaps = Maps.newEnumMap(EnumProtocolDirection.class);
        this.id = var3;
    }

    protected EnumProtocol registerPacket(EnumProtocolDirection direction, Class<? extends Packet> packetClass) {
        BiMap<Integer, Class<? extends Packet>> map = this.directionMaps.get(direction);

        if (map == null) {
            map = HashBiMap.create();
            this.directionMaps.put(direction, map);
        }

        if (map.containsValue(packetClass)) {
            String log = direction + " packet " + packetClass + " is already known to ID " + map.inverse().get(packetClass);
            LogManager.getLogger().fatal(log);
            throw new IllegalArgumentException(log);
        } else {
            map.put(map.size(), packetClass);
            return this;
        }
    }

    public Integer getPacketId(EnumProtocolDirection direction, Packet packet) {
        return this.directionMaps.get(direction).inverse().get(packet.getClass());
    }

    public Packet getPacket(EnumProtocolDirection direction, int packetId) throws IllegalAccessException, InstantiationException {
        Class<? extends Packet> clazz = this.directionMaps.get(direction).get(packetId);
        return clazz == null ? null : clazz.newInstance();
    }

    public int getId() {
        return this.id;
    }

    public static EnumProtocol getById(int stateId) {
        return stateId >= handshakeState && stateId <= loginState ? STATES[stateId - handshakeState] : null;
    }

    public static EnumProtocol getFromPacket(Packet var0) {
        return STATES_BY_CLASS.get(var0.getClass());
    }

    static {
        for (EnumProtocol protocol : values()) {
            int id = protocol.getId();
            if (id < handshakeState || id > loginState) {
                throw new Error("Invalid protocol ID " + id);
            }

            STATES[id - handshakeState] = protocol;

            for (EnumProtocolDirection direction : protocol.directionMaps.keySet()) {

                for (Class<? extends Packet> pclass : (protocol.directionMaps.get(direction)).values()) {
                    if (STATES_BY_CLASS.containsKey(pclass) && STATES_BY_CLASS.get(pclass) != protocol) {
                        throw new Error("Packet " + pclass + " is already assigned to protocol " + STATES_BY_CLASS.get(pclass) + " - can't reassign to " + protocol);
                    }

                    try {
                        pclass.newInstance();
                    } catch (Throwable var10) {
                        throw new Error("Packet " + pclass + " fails instantiation checks! " + pclass);
                    }
                }
            }
        }

    }
}
