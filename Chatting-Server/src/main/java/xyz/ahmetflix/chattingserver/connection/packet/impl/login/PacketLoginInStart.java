package xyz.ahmetflix.chattingserver.connection.packet.impl.login;

import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginInListener;

import java.io.IOException;

public class PacketLoginInStart implements Packet<PacketLoginInListener> {
    private UserProfile userProfile;

    public PacketLoginInStart() {
    }

    public PacketLoginInStart(UserProfile profile) {
        this.userProfile = profile;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.userProfile = new UserProfile(null, serializer.readString(16));
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.userProfile.getName());
    }

    public void handle(PacketLoginInListener handler) {
        handler.handleLoginStart(this);
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }
}
