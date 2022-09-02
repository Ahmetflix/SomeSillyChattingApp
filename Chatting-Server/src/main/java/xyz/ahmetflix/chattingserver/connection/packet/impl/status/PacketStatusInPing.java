package xyz.ahmetflix.chattingserver.connection.packet.impl.status;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.status.PacketStatusInListener;

import java.io.IOException;

public class PacketStatusInPing implements Packet<PacketStatusInListener> {
    private long ping;

    public PacketStatusInPing() {

    }

    public PacketStatusInPing(long ping) {
        this.ping = ping;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.ping = serializer.readLong();
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeLong(this.ping);
    }

    public void handle(PacketStatusInListener handler) {
        handler.handlePing(this);
    }

    public long getPing() {
        return ping;
    }
}
