package xyz.ahmetflix.chattingserver;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.json.IPBanList;
import xyz.ahmetflix.chattingserver.json.OpList;
import xyz.ahmetflix.chattingserver.json.UserListBans;
import xyz.ahmetflix.chattingserver.json.WhiteList;
import xyz.ahmetflix.chattingserver.user.ChatUser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UsersList {

    public static final File FILE_PLAYERBANS = new File("banned-players.json");
    public static final File FILE_IPBANS = new File("banned-ips.json");
    public static final File FILE_OPS = new File("ops.json");
    public static final File FILE_WHITELIST = new File("whitelist.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");
    private final Server server;
    public final List<ChatUser> users = new java.util.concurrent.CopyOnWriteArrayList<ChatUser>();
    private final Map<UUID, ChatUser> uuidToUserMap = Maps.newHashMap();
    private final UserListBans bannedPlayers;
    private final IPBanList bannedIPs;
    private final OpList ops;
    private final WhiteList whitelist;
    private boolean hasWhitelist;
    protected int maxPlayers;
    private int playerPingIndex;

    public UsersList(Server server) {
        this.bannedPlayers = new UserListBans(FILE_PLAYERBANS);
        this.bannedIPs = new IPBanList(FILE_IPBANS);
        this.ops = new OpList(FILE_OPS);
        this.whitelist = new WhiteList(FILE_WHITELIST);
        this.server = server;
        this.bannedPlayers.setLanServer(false);
        this.bannedIPs.setLanServer(false);
        this.maxPlayers = 8;
    }

    public int getPlayerCount() {
        return this.users.size();
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public List<ChatUser> getUsers() {
        return users;
    }
}
