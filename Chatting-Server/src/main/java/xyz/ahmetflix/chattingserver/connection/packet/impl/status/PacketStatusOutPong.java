package xyz.ahmetflix.chattingserver.connection.packet.impl.status;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.status.PacketStatusOutListener;

import java.io.IOException;

public class PacketStatusOutPong implements Packet<PacketStatusOutListener> {
    private long ping;

    public PacketStatusOutPong() {
    }

    public PacketStatusOutPong(long ping) {
        this.ping = ping;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.ping = serializer.readLong();
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeLong(this.ping);
    }

    public void handle(PacketStatusOutListener handler) {
        handler.handlePong(this);
    }
}
