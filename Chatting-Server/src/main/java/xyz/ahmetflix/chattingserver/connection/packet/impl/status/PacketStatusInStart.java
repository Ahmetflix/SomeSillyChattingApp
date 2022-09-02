package xyz.ahmetflix.chattingserver.connection.packet.impl.status;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.status.PacketStatusInListener;

import java.io.IOException;

public class PacketStatusInStart implements Packet<PacketStatusInListener> {
    public PacketStatusInStart() {
    }

    public void read(PacketDataSerializer var1) throws IOException {
    }

    public void write(PacketDataSerializer var1) throws IOException {
    }

    public void handle(PacketStatusInListener var1) {
        var1.handleStatusStart(this);
    }
}
