package xyz.ahmetflix.chattingserver;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;

public class UserProfile {

    private final UUID id;
    private final String name;

    public UserProfile(UUID var1, String var2) {
        if (var1 == null && StringUtils.isBlank(var2)) {
            throw new IllegalArgumentException("Name and ID cannot both be blank");
        } else {
            this.id = var1;
            this.name = var2;
        }
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isComplete() {
        return this.id != null && StringUtils.isNotBlank(this.getName());
    }

    public int hashCode() {
        int var1 = this.id != null ? this.id.hashCode() : 0;
        var1 = 31 * var1 + (this.name != null ? this.name.hashCode() : 0);
        return var1;
    }

    public String toString() {
        return (new ToStringBuilder(this)).append("id", this.id).append("name", this.name).toString();
    }

    public static UserProfile makeUUIDIfNotExists(UserProfile profile) {
        UUID uuid = profile.getId();

        if (uuid == null) {
            uuid = createUUID(profile.getName());
        }

        return new UserProfile(uuid, profile.getName());
    }

    public static UUID grabUUID(UserProfile userProfile) {
        UUID uuid = userProfile.getId();

        if (uuid == null) {
            uuid = createUUID(userProfile.getName());
        }

        return uuid;
    }

    public static UUID createUUID(String name) {
        return UUID.nameUUIDFromBytes(("ChatUser:" + name).getBytes(Charsets.UTF_8));
    }

}
