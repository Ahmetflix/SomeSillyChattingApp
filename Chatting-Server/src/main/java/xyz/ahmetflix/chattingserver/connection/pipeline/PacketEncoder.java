package xyz.ahmetflix.chattingserver.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import xyz.ahmetflix.chattingserver.connection.EnumProtocolDirection;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;

import java.io.IOException;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {

    private final EnumProtocolDirection direction;

    public PacketEncoder(EnumProtocolDirection enumprotocoldirection) {
        this.direction = enumprotocoldirection;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf byteBuf)
            throws Exception {
        Integer packetId = ctx.channel().attr(NetworkManager.protocolAttributeKey).get().getPacketIdForPacket(packet);

        if (packetId == null) {
            throw new IOException("Can't serialize unregistered packet");
        } else {
            PacketDataSerializer serializer = new PacketDataSerializer(byteBuf);
            serializer.writeVarInt(packetId);

            packet.write(serializer);
        }
    }
}
