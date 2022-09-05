package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;

import java.io.IOException;

public class PacketPlayOutKeepAlive implements Packet<PacketListenerPlayOut> {
    private int keepAlive;

    public PacketPlayOutKeepAlive() {
    }

    public PacketPlayOutKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void handle(PacketListenerPlayOut handler) {
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
