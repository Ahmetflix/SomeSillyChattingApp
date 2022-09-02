package xyz.ahmetflix.chattingserver.connection.packet.listeners.status;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInPing;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInStart;

public interface PacketStatusInListener extends PacketListener {
    void handlePing(PacketStatusInPing var1);

    void handleStatusStart(PacketStatusInStart var1);
}
