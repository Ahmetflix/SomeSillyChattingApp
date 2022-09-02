package xyz.ahmetflix.chattingserver.connection.packet.listeners.login;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutDisconnect;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSetCompression;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSuccess;

public interface PacketLoginOutListener extends PacketListener {

    void handleSuccess(PacketLoginOutSuccess var1);

    void handleDisconnect(PacketLoginOutDisconnect var1);

    void handleSetCompression(PacketLoginOutSetCompression var1);
}
