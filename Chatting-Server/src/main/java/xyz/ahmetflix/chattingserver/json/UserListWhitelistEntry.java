package xyz.ahmetflix.chattingserver.json;

import com.google.gson.JsonObject;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.util.UUID;

public class UserListWhitelistEntry extends UserListEntry<UserProfile>
{
    public UserListWhitelistEntry(UserProfile profile)
    {
        super(profile);
    }

    public UserListWhitelistEntry(JsonObject json)
    {
        super(gameProfileFromJsonObject(json), json);
    }

    protected void onSerialization(JsonObject data)
    {
        if (this.getValue() != null)
        {
            data.addProperty("uuid", (this.getValue()).getId() == null ? "" : (this.getValue()).getId().toString());
            data.addProperty("name", (this.getValue()).getName());
            super.onSerialization(data);
        }
    }

    private static UserProfile gameProfileFromJsonObject(JsonObject json)
    {
        if (json.has("uuid") && json.has("name"))
        {
            String s = json.get("uuid").getAsString();
            UUID uuid;

            try
            {
                uuid = UUID.fromString(s);
            }
            catch (Throwable var4)
            {
                return null;
            }

            return new UserProfile(uuid, json.get("name").getAsString());
        }
        else
        {
            return null;
        }
    }
}
