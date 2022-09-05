package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;
import xyz.ahmetflix.chattingserver.user.ChatUser;

import java.io.IOException;
import java.util.List;

public class PacketPlayOutUserInfo implements Packet<PacketListenerPlayOut> {
    private EnumUserInfoAction action;
    private final List<UserInfoData> users = Lists.newArrayList();

    public PacketPlayOutUserInfo() {
    }

    public PacketPlayOutUserInfo(EnumUserInfoAction action, ChatUser... users) {
        this.action = action;

        for (ChatUser user : users) {
            this.users.add(new UserInfoData(user.getProfile(), user.ping));
        }

    }

    public PacketPlayOutUserInfo(EnumUserInfoAction action, Iterable<ChatUser> users) {
        this.action = action;

        for (ChatUser user : users) {
            this.users.add(new UserInfoData(user.getProfile(), user.ping));
        }
    }

    public EnumUserInfoAction getAction() {
        return action;
    }

    public void setAction(EnumUserInfoAction action) {
        this.action = action;
    }

    public List<UserInfoData> getUsers() {
        return users;
    }

    @Override
    public void read(PacketDataSerializer serializer) throws IOException {
        this.action = serializer.readEnum(EnumUserInfoAction.class);
        int i = serializer.readVarInt();

        for (int j = 0; j < i; ++j) {
            UserProfile profile = null;
            int ping = 0;
            switch (this.action) {
                case ADD_USER:
                    profile = new UserProfile(serializer.readUUID(), serializer.readString(16));
                    ping = serializer.readVarInt();
                    break;
                case UPDATE_LATENCY:
                    profile = new UserProfile(serializer.readUUID(), null);
                    ping = serializer.readVarInt();
                    break;
                case REMOVE_USER:
                    profile = new UserProfile(serializer.readUUID(), null);
            }

            this.users.add(new UserInfoData(profile, ping));
        }

    }

    @Override
    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeEnum(this.action);
        serializer.writeVarInt(this.users.size());

        for (UserInfoData data : this.users) {
            switch (this.action) {
                case ADD_USER:
                    serializer.writeUUID(data.getProfile().getId());
                    serializer.writeString(data.getProfile().getName());

                    serializer.writeVarInt(data.getPing());
                    break;
                case UPDATE_LATENCY:
                    serializer.writeUUID(data.getProfile().getId());
                    serializer.writeVarInt(data.getPing());
                    break;
                case REMOVE_USER:
                    serializer.writeUUID(data.getProfile().getId());
            }
        }
    }

    @Override
    public void handle(PacketListenerPlayOut handler) {
        handler.handleUserInfo(this);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("action", this.action).add("entries", this.users).toString();
    }

    public static class UserInfoData {
        private final int ping;
        private final UserProfile profile;

        public UserInfoData(UserProfile profile, int ping) {
            this.profile = profile;
            this.ping = ping;
        }

        public UserProfile getProfile() {
            return profile;
        }

        public int getPing() {
            return ping;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("latency", this.ping).add("profile", this.profile).toString();
        }
    }

    public enum EnumUserInfoAction {
        ADD_USER, UPDATE_LATENCY, REMOVE_USER;

    }
}
