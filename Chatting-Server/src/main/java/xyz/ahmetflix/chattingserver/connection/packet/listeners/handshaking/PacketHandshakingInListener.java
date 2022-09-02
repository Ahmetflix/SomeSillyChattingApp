package xyz.ahmetflix.chattingserver.connection.packet.listeners.handshaking;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;

public interface PacketHandshakingInListener extends PacketListener {
    void handleSetProtocol(PacketHandshakingInSetProtocol var1);
}
