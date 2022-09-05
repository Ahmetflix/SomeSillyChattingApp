package xyz.ahmetflix.chattingserver.connection.packet.listeners.play;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.*;

public interface PacketListenerPlayOut extends PacketListener {

    void handleChat(PacketPlayOutChat packet);

    void handleTabComplete(PacketPlayOutTabComplete packet);

    void handleKickDisconnect(PacketPlayOutKickDisconnect packet);

    void handleKeepAlive(PacketPlayOutKeepAlive packet);

    void handleLogin(PacketPlayOutLogin packet);

    void handleUserInfo(PacketPlayOutUserInfo packet);

    void handleSetCompression(PacketPlayOutSetCompression packet);
}
