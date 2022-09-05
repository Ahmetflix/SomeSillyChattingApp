package xyz.ahmetflix.chattingserver.user;

import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutChat;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.UserConnection;
import xyz.ahmetflix.chattingserver.json.UserListOpsEntry;

import java.util.UUID;

public class ChatUser implements ICommandListener {

    private static int userCount = 0;
    private int id;
    protected UUID uniqueID;
    private final UserProfile profile;
    public int ping;
    public UserConnection connection;

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
        this.connection.sendPacket(new PacketPlayOutChat(message));
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        if (!"help".equals(commandName)) {
            if (Server.getInstance().getUsersList().isOp(this.getProfile())) {
                UserListOpsEntry entry = Server.getInstance().getUsersList().getOps().getEntry(this.getProfile());
                if (entry != null) {
                    return entry.getPermissionLevel() >= permLevel;
                } else {
                    return 4 >= permLevel;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
