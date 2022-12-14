package xyz.ahmetflix.chattingserver.connection.packet.listeners.status;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutPong;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutServerInfo;

public interface PacketStatusOutListener extends PacketListener {
    void handleServerInfo(PacketStatusOutServerInfo packet);

    void handlePong(PacketStatusOutPong packet);
}
