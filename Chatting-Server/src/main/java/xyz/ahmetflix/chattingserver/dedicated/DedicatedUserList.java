package xyz.ahmetflix.chattingserver.dedicated;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.PropertyManager;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.user.UsersList;

import java.io.IOException;

public class DedicatedUserList extends UsersList {

    private static final Logger LOGGER = LogManager.getLogger();

    public DedicatedUserList(DedicatedServer server) {
        super(server);
        PropertyManager props = server.getPropertyManager();
        this.maxUsers = props.getInt("max-players", 20);
        this.setHasWhitelist(props.getBoolean("white-list", false));
        this.loadBans();
        this.saveBans();
        this.loadIPBans();
        this.saveIPBans();
        this.loadOps();
        this.loadWhiteList();
        this.saveOps();
        if (!this.getWhitelist().getSaveFile().exists()) {
            this.saveWhiteList();
        }
    }

    public void setHasWhitelist(boolean hasWhitelist) {
        super.setHasWhitelist(hasWhitelist);
        this.getServer().getPropertyManager().setProperty("white-list", hasWhitelist);
        this.getServer().getPropertyManager().savePropertiesFile();
    }

    public void addOp(UserProfile profile) {
        super.addOp(profile);
        this.saveOps();
    }

    public void removeOp(UserProfile profile) {
        super.removeOp(profile);
        this.saveOps();
    }

    public void removeWhitelist(UserProfile profile) {
        super.removeWhitelist(profile);
        this.saveWhiteList();
    }

    public void addWhitelist(UserProfile profile) {
        super.addWhitelist(profile);
        this.saveWhiteList();
    }

    public void reloadWhitelist() {
        this.loadWhiteList();
    }

    private void loadBans() {
        try {
            this.getBannedUsers().load();
        } catch (IOException ex) {
            LOGGER.warn("Failed to load user banlist: ", ex);
        }
    }

    private void saveBans() {
        try {
            this.getBannedUsers().writeChanges();
        } catch (IOException var2) {
            LOGGER.warn("Failed to save user banlist: ", var2);
        }
    }

    private void loadIPBans() {
        try {
            this.getBannedIPs().load();
        } catch (IOException ex) {
            LOGGER.warn("Failed to load ip banlist: ", ex);
        }
    }

    private void saveIPBans() {
        try {
            this.getBannedIPs().writeChanges();
        } catch (IOException var2) {
            LOGGER.warn("Failed to save ip banlist: ", var2);
        }
    }

    private void loadOps() {
        try {
            this.getOps().load();
        } catch (IOException ex) {
            LOGGER.warn("Failed to load operators list: ", ex);
        }
    }

    private void saveOps() {
        try {
            this.getOps().writeChanges();
        } catch (IOException var2) {
            LOGGER.warn("Failed to save operators list: ", var2);
        }
    }

    private void loadWhiteList() {
        try {
            this.getWhitelist().load();
        } catch (IOException ex) {
            LOGGER.warn("Failed to load white-list: ", ex);
        }
    }

    private void saveWhiteList() {
        try {
            this.getWhitelist().writeChanges();
        } catch (IOException var2) {
            LOGGER.warn("Failed to save white-list: ", var2);
        }
    }

    public boolean isWhitelisted(UserProfile profile) {
        return !this.getHasWhitelist() || this.isOp(profile) || this.getWhitelist().hasEntry(profile);
    }

    @Override
    public DedicatedServer getServer() {
        return (DedicatedServer)super.getServer();
    }

    @Override
    public boolean bypassesUserLimit(UserProfile profile) {
        return this.getOps().bypassesUserLimit(profile);
    }
}
