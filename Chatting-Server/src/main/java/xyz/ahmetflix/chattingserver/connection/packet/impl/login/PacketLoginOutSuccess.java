package xyz.ahmetflix.chattingserver.connection.packet.impl.login;

import xyz.ahmetflix.chattingserver.UserProfile;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginOutListener;

import java.io.IOException;
import java.util.UUID;

public class PacketLoginOutSuccess implements Packet<PacketLoginOutListener> {
    private UserProfile profile;

    public PacketLoginOutSuccess() {
    }

    public PacketLoginOutSuccess(UserProfile profile) {
        this.profile = profile;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        String uid = serializer.readString(36);
        String name = serializer.readString(16);
        UUID uuid = UUID.fromString(uid);
        this.profile = new UserProfile(uuid, name);
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        UUID uuid = this.profile.getId();
        serializer.writeString(uuid == null ? "" : uuid.toString());
        serializer.writeString(this.profile.getName());
    }

    public void handle(PacketLoginOutListener handler) {
        handler.handleSuccess(this);
    }

    public UserProfile getProfile() {
        return profile;
    }
}
