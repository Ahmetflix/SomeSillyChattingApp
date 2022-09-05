package xyz.ahmetflix.chattingserver.connection.packet.listeners.play;

import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayInChat;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayInKeepAlive;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayInTabComplete;

public interface PacketListenerPlayIn extends PacketListener {

    void handleChat(PacketPlayInChat packet);

    void handleTabComplete(PacketPlayInTabComplete packet);

    void handleKeepAlive(PacketPlayInKeepAlive packet);

}