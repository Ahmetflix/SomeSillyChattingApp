package xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking;

import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.handshaking.PacketHandshakingInListener;

import java.io.IOException;

public class PacketHandshakingInSetProtocol implements Packet<PacketHandshakingInListener> {

    public String hostname;
    public int port;
    private EnumProtocol requestedState;


    public PacketHandshakingInSetProtocol() {
    }

    public PacketHandshakingInSetProtocol(String hostname, int port, EnumProtocol requestedState) {
        this.hostname = hostname;
        this.port = port;
        this.requestedState = requestedState;
    }

    @Override
    public void read(PacketDataSerializer serializer) throws IOException {
        this.hostname = serializer.readString(Short.MAX_VALUE);
        this.port = serializer.readUnsignedShort();
        this.requestedState = EnumProtocol.isValidIntention(serializer.readVarInt());
    }

    @Override
    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.hostname);
        serializer.writeShort(this.port);
        serializer.writeVarInt(this.requestedState.getStateId());
    }

    @Override
    public void handle(PacketHandshakingInListener packethandshakinginlistener) {
        packethandshakinginlistener.handleSetProtocol(this);
    }

    public EnumProtocol getRequestedState() {
        return requestedState;
    }
}
