package xyz.ahmetflix.chattingserver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.json.*;
import xyz.ahmetflix.chattingserver.user.ChatUser;

import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class UsersList {

    public static final File FILE_PLAYERBANS = new File("banned-players.json");
    public static final File FILE_IPBANS = new File("banned-ips.json");
    public static final File FILE_OPS = new File("ops.json");
    public static final File FILE_WHITELIST = new File("whitelist.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");
    private final Server server;
    public final List<ChatUser> users = new java.util.concurrent.CopyOnWriteArrayList<ChatUser>();
    private final Map<UUID, ChatUser> uuidToUserMap = Maps.newHashMap();
    private final Map<String,ChatUser> usersByName = new CaseInsensitiveMap<>();
    private final UserListBans bannedUsers;
    private final IPBanList bannedIPs;
    private final OpList ops;
    private final WhiteList whitelist;
    private boolean hasWhitelist;
    protected int maxUsers;
    private int userPingIndex;

    public UsersList(Server server) {
        this.bannedUsers = new UserListBans(FILE_PLAYERBANS);
        this.bannedIPs = new IPBanList(FILE_IPBANS);
        this.ops = new OpList(FILE_OPS);
        this.whitelist = new WhiteList(FILE_WHITELIST);
        this.server = server;
        this.bannedUsers.setLanServer(false);
        this.bannedIPs.setLanServer(false);
        this.maxUsers = 8;
    }

    public void onUserJoin(ChatUser chatUser, String joinMessage) {
        this.users.add(chatUser);
        this.usersByName.put(chatUser.getName(), chatUser);
        this.uuidToUserMap.put(chatUser.getUniqueID(), chatUser);

        if (joinMessage != null && joinMessage.length() > 0) {
            //server.getUsersList().sendAll(new PacketPlayOutChat(line));
        }

        /*PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, user);

        for (ChatUser user : this.users) {
            user.playerConnection.sendPacket(packet);

            chatUser.connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, user));
        }*/
    }

    public String disconnect(ChatUser chatUser) {
        this.users.remove(chatUser);
        this.usersByName.remove(chatUser.getName());
        UUID uuid = chatUser.getUniqueID();
        ChatUser chatUser1 = this.uuidToUserMap.get(uuid);

        if (chatUser1 == chatUser) {
            this.uuidToUserMap.remove(uuid);
        }

        /*PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, chatUser);
        for (ChatUser user : users) {
            user.connection.sendPacket(packet);
        }*/

        return chatUser.getName() + " left the server";
    }
    public ChatUser attemptLogin(/*LoginListener*/Object loginlistener, UserProfile userProfile, String hostname) {
        UUID uuid = UserProfile.grabUUID(userProfile);
        userProfile = new UserProfile(uuid, userProfile.getName());
        List<ChatUser> arraylist = Lists.newArrayList();

        for (ChatUser chatUser : this.users) {
            if (chatUser.getUniqueID().equals(uuid)) {
                arraylist.add(chatUser);
            }
        }

        for (ChatUser chatUser : arraylist) {
            //chatUser.connection.disconnect("You logged in from another location");
        }

        SocketAddress socketaddress = null/*loginlistener.networkManager.getSocketAddress()*/;

        ChatUser user = new ChatUser(userProfile);
        String s;
        boolean disallow = false;

        if (getBannedUsers().isBanned(userProfile) && !getBannedUsers().getEntry(userProfile).hasExpired()) {
            UserListBansEntry banEntry = this.bannedUsers.getEntry(userProfile);

            s = "You are banned from this server!\nReason: " + banEntry.getBanReason();
            if (banEntry.getBanEndDate() != null) {
                s = s + "\nYour ban will be removed on " + dateFormat.format(banEntry.getBanEndDate());
            }

            disallow = true;
        } else if (!this.isWhitelisted(userProfile)) {
            s = "You are not white-listed on this server!";
            disallow = true;
            // Dont check ips now because of login listener is not implemented
        /*} else if (getBannedIPs().isBanned(socketaddress) && !getBannedIPs().getBanEntry(socketaddress).hasExpired()) {
            IPBanEntry ipbanentry = this.bannedIPs.getBanEntry(socketaddress);

            s = "Your IP address is banned from this server!\nReason: " + ipbanentry.getBanReason();
            if (ipbanentry.getBanEndDate() != null) {
                s = s + "\nYour ban will be removed on " + dateFormat.format(ipbanentry.getBanEndDate());
            }

            disallow = true;
        */} else {
            s = "The server is full!";
            if (this.users.size() >= this.maxUsers) {
                disallow = true;
            }
        }

        if (disallow) {
            //loginlistener.disconnect(s);
            return null;
        }
        return user;
    }

    private int tickTimer;

    public void tick() {
        if (++this.tickTimer > 600) {
            //this.sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, this.users));
            this.tickTimer = 0;
        }

    }

    public void sendAll(Packet packet) {
        for (int i = 0; i < this.users.size(); ++i) {
            //this.users.get(i).connection.sendPacket(packet);
        }

    }

    public int getUserCount() {
        return this.users.size();
    }

    public int getMaxUsers() {
        return this.maxUsers;
    }

    public UserListBans getBannedUsers() {
        return bannedUsers;
    }

    public IPBanList getBannedIPs() {
        return bannedIPs;
    }

    public WhiteList getWhitelist() {
        return whitelist;
    }

    public OpList getOps() {
        return ops;
    }

    public boolean isWhitelisted(UserProfile userProfile) {
        return !this.hasWhitelist || this.ops.hasEntry(userProfile) || this.whitelist.hasEntry(userProfile);
    }

    public List<ChatUser> getUsers() {
        return users;
    }
}
