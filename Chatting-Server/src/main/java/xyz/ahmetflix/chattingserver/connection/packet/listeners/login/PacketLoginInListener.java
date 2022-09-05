package xyz.ahmetflix.chattingserver.connection.packet.listeners.login;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginInEncryptionBegin;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginInStart;

public interface PacketLoginInListener extends PacketListener {
    void handleLoginStart(PacketLoginInStart packet);

    void handleEncryption(PacketLoginInEncryptionBegin packet);
}
