package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayIn;

import java.io.IOException;

public class PacketPlayInKeepAlive implements Packet<PacketListenerPlayIn> {
    private int keepAlive;

    public PacketPlayInKeepAlive() {
    }

    public PacketPlayInKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void handle(PacketListenerPlayIn handler) {
        handler.handleKeepAlive(this);
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.keepAlive = serializer.readVarInt();
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeVarInt(this.keepAlive);
    }

    public int getKeepAlive() {
        return keepAlive;
    }
}
