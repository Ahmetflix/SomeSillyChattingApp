package xyz.ahmetflix.chattingserver.json;

import com.google.gson.JsonObject;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.util.UUID;

public class UserListOpsEntry extends UserListEntry<UserProfile>
{
    private final int permissionLevel;
    private final boolean bypassesUserLimit;

    public UserListOpsEntry(UserProfile userProfile, int permissionLevelIn, boolean bypassesUserLimit)
    {
        super(userProfile);
        this.permissionLevel = permissionLevelIn;
        this.bypassesUserLimit = bypassesUserLimit;
    }

    public UserListOpsEntry(JsonObject obj)
    {
        super(constructProfile(obj), obj);
        this.permissionLevel = obj.has("level") ? obj.get("level").getAsInt() : 0;
        this.bypassesUserLimit = obj.has("bypassesUserLimit") && obj.get("bypassesUserLimit").getAsBoolean();
    }

    public int getPermissionLevel()
    {
        return this.permissionLevel;
    }

    public boolean bypassesUserLimit()
    {
        return this.bypassesUserLimit;
    }

    protected void onSerialization(JsonObject data)
    {
        if (this.getValue() != null)
        {
            data.addProperty("uuid", (this.getValue()).getId() == null ? "" : (this.getValue()).getId().toString());
            data.addProperty("name", (this.getValue()).getName());
            super.onSerialization(data);
            data.addProperty("level", this.permissionLevel);
            data.addProperty("bypassesPlayerLimit", this.bypassesUserLimit);
        }
    }

    private static UserProfile constructProfile(JsonObject obj)
    {
        if (obj.has("uuid") && obj.has("name"))
        {
            String s = obj.get("uuid").getAsString();
            UUID uuid;

            try
            {
                uuid = UUID.fromString(s);
            }
            catch (Throwable var4)
            {
                return null;
            }

            return new UserProfile(uuid, obj.get("name").getAsString());
        }
        else
        {
            return null;
        }
    }
}
