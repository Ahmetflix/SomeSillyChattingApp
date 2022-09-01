package xyz.ahmetflix.chattingserver.connection.packet;

public interface PacketListener
{
    void onDisconnect(String reason);
}