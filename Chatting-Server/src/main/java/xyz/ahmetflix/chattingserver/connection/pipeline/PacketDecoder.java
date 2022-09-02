package xyz.ahmetflix.chattingserver.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import xyz.ahmetflix.chattingserver.connection.EnumProtocolDirection;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;

import java.io.IOException;
import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker PACKET_RECEIVED = MarkerManager.getMarker("PACKET_RECEIVED", NetworkManager.networkPacketsMarker);;
    private final EnumProtocolDirection direction;

    public PacketDecoder(EnumProtocolDirection direction) {
        this.direction = direction;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!in.isReadable()) {
            return;
        }

        PacketDataSerializer packetDataHelper = new PacketDataSerializer(in);
        int packetId = packetDataHelper.readVarInt();
        Packet<?> packet = ctx.channel().attr(NetworkManager.protocolAttributeKey).get().createPacket(this.direction, packetId);
        if (packet == null) {
            throw new IOException("Bad packet id " + packetId);
        }

        packet.read(packetDataHelper);

        if (packetDataHelper.isReadable()) {
            throw new IOException("Packet " + ctx.channel().attr(NetworkManager.protocolAttributeKey).get().getStateId()
                    + "/" + packetId + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found "
                    + packetDataHelper.readableBytes() + " bytes extra whilst reading packet " + packetId);
        }
        out.add(packet);
    }
}