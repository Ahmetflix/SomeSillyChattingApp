package xyz.ahmetflix.chattingclient;

import xyz.ahmetflix.chattingclient.packet.listeners.play.ClientUserConnection;
import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.UserConnection;
import xyz.ahmetflix.chattingserver.user.UserProfile;

import java.util.UUID;

public class ClientChatUser implements ICommandListener {

    private static int userCount = 0;
    private int id;
    protected UUID uniqueID;
    private final UserProfile profile;
    public int ping;
    public ClientUserConnection connection;

    public ClientChatUser(UserProfile profile) {
        this.id = userCount++;
        this.uniqueID = UserProfile.grabUUID(profile);
        this.profile = profile;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public UUID getUniqueID() {
        return uniqueID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return this.profile.getName();
    }

    @Override
    public void addChatMessage(String message) {

    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        return false;
    }
}
