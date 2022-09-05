package xyz.ahmetflix.chattingserver.connection.packet.listeners.login;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutDisconnect;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutEncryptionBegin;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSetCompression;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSuccess;

public interface PacketLoginOutListener extends PacketListener {
    void handleEncryptionBegin(PacketLoginOutEncryptionBegin packet);

    void handleSuccess(PacketLoginOutSuccess packet);

    void handleDisconnect(PacketLoginOutDisconnect packet);

    void handleSetCompression(PacketLoginOutSetCompression packet);
}
