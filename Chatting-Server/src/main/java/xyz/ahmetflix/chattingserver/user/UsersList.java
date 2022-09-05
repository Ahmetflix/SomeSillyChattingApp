package xyz.ahmetflix.chattingserver.user;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutChat;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutLogin;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutUserInfo;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.LoginListener;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.UserConnection;
import xyz.ahmetflix.chattingserver.json.*;
import xyz.ahmetflix.chattingserver.util.CaseInsensitiveMap;

import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class UsersList {

    public static final File FILE_PLAYERBANS = new File("banned-players.json");
    public static final File FILE_IPBANS = new File("banned-ips.json");
    public static final File FILE_OPS = new File("ops.json");
    public static final File FILE_WHITELIST = new File("whitelist.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");
    private final Server server;
    public final List<ChatUser> users = new java.util.concurrent.CopyOnWriteArrayList<ChatUser>();
    public final Map<String, ChatUser> userMap = new Object2ObjectArrayMap<String, ChatUser>() {

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        @Override
        public ChatUser put(String key, ChatUser value) {
            lock.writeLock().lock();
            try {
                return super.put(key.toLowerCase(), value);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public ChatUser get(Object key) {
            lock.readLock().lock();
            try {
                ChatUser user = super.get(key instanceof String ? ((String) key).toLowerCase() : key);
                return (user != null && user.connection != null) ? user : null;
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public ChatUser remove(Object key) {
            lock.writeLock().lock();
            try {
                return super.remove(key instanceof String ? ((String) key).toLowerCase() : key);
            } finally {
                lock.writeLock().unlock();
            }
        }
    };
    public final Map<UUID, ChatUser> uuidToUserMap = new Object2ObjectArrayMap<UUID, ChatUser>() {

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        @Override
        public ChatUser get(Object key) {
            lock.readLock().lock();
            try {
                ChatUser user = super.get(key instanceof String ? ((String) key).toLowerCase() : key);
                return (user != null && user.connection != null) ? user : null;
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public ChatUser put(UUID key, ChatUser value) {
            lock.writeLock().lock();
            try {
                return super.put(key, value);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public ChatUser remove(Object key) {
            lock.writeLock().lock();
            try {
                return super.remove(key);
            } finally {
                lock.writeLock().unlock();
            }
        }
    };
    private final Map<String,ChatUser> usersByName = new CaseInsensitiveMap<>();
    private final UserListBans bannedUsers;
    private final IPBanList bannedIPs;
    private final OpList ops;
    private final Set<UUID> fastOperator = new HashSet<>();
    private final WhiteList whitelist;
    private boolean hasWhitelist;
    protected int maxUsers;
    private int userPingIndex;

    public UsersList(Server server) {
        this.bannedUsers = new UserListBans(FILE_PLAYERBANS);
        this.bannedIPs = new IPBanList(FILE_IPBANS);
        this.ops = new OpList(FILE_OPS);
        for (UserListOpsEntry value : this.ops.getValues().values()) {
            this.fastOperator.add(value.getValue().getId());
        }
        this.whitelist = new WhiteList(FILE_WHITELIST);
        this.server = server;
        this.bannedUsers.setLanServer(false);
        this.bannedIPs.setLanServer(false);
        this.maxUsers = 8;
    }

    public void initializeConnection(NetworkManager networkmanager, ChatUser chatUser) {
        UserProfile userProfile = chatUser.getProfile();
        UserCache usercache = this.server.getUserCache();
        UserProfile cachedProfile = usercache.getProfileByUUID(userProfile.getId());
        String name = cachedProfile == null ? userProfile.getName() : cachedProfile.getName();
        if (!Objects.equals(userProfile.getName(), name)) {
            networkmanager.close(disconnect(chatUser));
            return;
        }
        usercache.addNewProfile(userProfile);

        String s1 = "local";

        if (networkmanager.getSocketAddress() != null) {
            s1 = networkmanager.getSocketAddress().toString();
        }

        UserConnection userConnection = new UserConnection(this.server, networkmanager, chatUser);

        userConnection.sendPacket(new PacketPlayOutLogin(chatUser.getId(), Math.min(this.getMaxUsers(), 60)));

        this.server.refreshStatusNextTick();

        String joinMessage = chatUser.getName() + " joined the server";

        this.onUserJoin(chatUser, joinMessage);

        LOGGER.info(chatUser.getName() + "[" + s1 + "] logged in with id " + chatUser.getId());
    }

    public void onUserJoin(ChatUser chatUser, String joinMessage) {
        this.users.add(chatUser);
        this.usersByName.put(chatUser.getName(), chatUser);
        this.uuidToUserMap.put(chatUser.getUniqueID(), chatUser);
        this.userMap.put(chatUser.getName(), chatUser);

        if (joinMessage != null && joinMessage.length() > 0) {
            server.getUsersList().sendAll(new PacketPlayOutChat(joinMessage));
        }

        PacketPlayOutUserInfo packet = new PacketPlayOutUserInfo(PacketPlayOutUserInfo.EnumUserInfoAction.ADD_USER, chatUser);

        for (ChatUser user : this.users) {
            user.connection.sendPacket(packet);

            chatUser.connection.sendPacket(new PacketPlayOutUserInfo(PacketPlayOutUserInfo.EnumUserInfoAction.ADD_USER, user));
        }
    }

    public String disconnect(ChatUser chatUser) {
        this.users.remove(chatUser);
        this.uuidToUserMap.remove(chatUser.getUniqueID());
        this.userMap.remove(chatUser.getName());
        this.usersByName.remove(chatUser.getName());
        UUID uuid = chatUser.getUniqueID();
        ChatUser chatUser1 = this.uuidToUserMap.get(uuid);

        if (chatUser1 == chatUser) {
            this.uuidToUserMap.remove(uuid);
            this.userMap.remove(chatUser1.getName());
        }

        PacketPlayOutUserInfo packet = new PacketPlayOutUserInfo(PacketPlayOutUserInfo.EnumUserInfoAction.REMOVE_USER, chatUser);
        for (ChatUser user : users) {
            user.connection.sendPacket(packet);
        }

        return chatUser.getName() + " left the server";
    }
    public ChatUser attemptLogin(LoginListener loginlistener, UserProfile userProfile, String hostname) {
        UUID uuid = UserProfile.grabUUID(userProfile);
        userProfile = new UserProfile(uuid, userProfile.getName());
        List<ChatUser> arraylist = Lists.newArrayList();

        for (ChatUser chatUser : this.users) {
            if (chatUser.getUniqueID().equals(uuid)) {
                arraylist.add(chatUser);
            }
        }

        for (ChatUser chatUser : arraylist) {
            chatUser.connection.disconnect("You logged in from another location");
        }

        SocketAddress socketaddress = loginlistener.networkManager.getSocketAddress();

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
        } else if (getBannedIPs().isBanned(socketaddress) && !getBannedIPs().getBanEntry(socketaddress).hasExpired()) {
            IPBanEntry ipbanentry = this.bannedIPs.getBanEntry(socketaddress);

            s = "Your IP address is banned from this server!\nReason: " + ipbanentry.getBanReason();
            if (ipbanentry.getBanEndDate() != null) {
                s = s + "\nYour ban will be removed on " + dateFormat.format(ipbanentry.getBanEndDate());
            }

            disallow = true;
        } else {
            s = "The server is full!";
            if (this.users.size() >= this.maxUsers && !this.bypassesUserLimit(userProfile)) {
                disallow = true;
            }
        }

        if (disallow) {
            loginlistener.disconnect(s);
            return null;
        }
        return user;
    }

    public void tick() {
        if (++this.userPingIndex > 600) {
            this.sendAll(new PacketPlayOutUserInfo(PacketPlayOutUserInfo.EnumUserInfoAction.UPDATE_LATENCY, this.users));
            this.userPingIndex = 0;
        }
    }

    public void sendAll(Packet packet) {
        for (ChatUser user : this.users) {
            user.connection.sendPacket(packet);
        }
    }

    public String[] usersNameArray() {
        String[] array = new String[this.users.size()];

        for (int i = 0; i < this.users.size(); ++i) {
            array[i] = this.users.get(i).getName();
        }

        return array;
    }

    public UserProfile[] usersProfileArray() {
        UserProfile[] array = new UserProfile[this.users.size()];

        for (int i = 0; i < this.users.size(); ++i) {
            array[i] = this.users.get(i).getProfile();
        }

        return array;
    }

    public void addOp(UserProfile profile) {
        this.ops.addEntry(new UserListOpsEntry(profile, 4, this.ops.bypassesUserLimit(profile)));
        this.fastOperator.add(profile.getId());
    }

    public void removeOp(UserProfile profile) {
        this.ops.removeEntry(profile);
        this.fastOperator.remove(profile.getId());
    }

    public boolean isWhitelisted(UserProfile userProfile) {
        return !this.hasWhitelist || this.fastOperator.contains(userProfile.getId()) || this.whitelist.hasEntry(userProfile);
    }

    public boolean isOp(UserProfile profile) {
        return this.fastOperator.contains(profile.getId());
    }

    public ChatUser getUser(String name) {
        return this.userMap.get(name);
    }

    public void addWhitelist(UserProfile profile) {
        this.whitelist.addEntry(new UserListWhitelistEntry(profile));
    }

    public void removeWhitelist(UserProfile profile) {
        this.whitelist.removeEntry(profile);
    }

    public void reloadWhitelist() {
    }

    public String[] getWhitelisted() {
        return this.whitelist.getKeys();
    }

    public String[] getOpsArray() {
        return this.ops.getKeys();
    }


    public int getUserCount() {
        return this.users.size();
    }

    public int getMaxUsers() {
        return this.maxUsers;
    }

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }

    public boolean getHasWhitelist() {
        return hasWhitelist;
    }

    public void setHasWhitelist(boolean hasWhitelist) {
        this.hasWhitelist = hasWhitelist;
    }

    public void serverShutdown() {
        for (ChatUser user : this.users) {
            user.connection.disconnect("Server shutdown");
        }
    }

    public void sendMessage(String msg) {
        this.server.addChatMessage(msg);
        this.sendAll(new PacketPlayOutChat(msg));
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

    public ChatUser getUserByUUID(UUID uuid) {
        return this.uuidToUserMap.get(uuid);
    }

    public List<ChatUser> getUsers() {
        return users;
    }

    public Server getServer() {
        return server;
    }

    public boolean bypassesUserLimit(UserProfile profile) {
        return false;
    }
}
