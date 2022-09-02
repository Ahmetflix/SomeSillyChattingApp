package xyz.ahmetflix.chattingserver.connection.packet.impl.login;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginOutListener;

import java.io.IOException;

public class PacketLoginOutSetCompression implements Packet<PacketLoginOutListener> {
    private int threshold;

    public PacketLoginOutSetCompression() {
    }

    public PacketLoginOutSetCompression(int var1) {
        this.threshold = var1;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.threshold = serializer.readVarInt();
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeVarInt(this.threshold);
    }

    public void handle(PacketLoginOutListener handler) {
        handler.handleSetCompression(this);
    }

    public int getThreshold() {
        return threshold;
    }
}
