package xyz.ahmetflix.chattingserver.user;

import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.util.UUID;

public class ChatUser implements ICommandListener {

    private static int userCount = 0;
    private int id;
    protected UUID uniqueID;
    private final UserProfile profile;

    public ChatUser(UserProfile profile) {
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
