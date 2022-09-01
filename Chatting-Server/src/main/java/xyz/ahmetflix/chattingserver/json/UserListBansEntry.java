package xyz.ahmetflix.chattingserver.json;

import com.google.gson.JsonObject;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.util.Date;
import java.util.UUID;

public class UserListBansEntry extends BanEntry<UserProfile>
{
    public UserListBansEntry(UserProfile profile)
    {
        this(profile, (Date)null, (String)null, (Date)null, (String)null);
    }

    public UserListBansEntry(UserProfile profile, Date startDate, String banner, Date endDate, String banReason)
    {
        super(profile, endDate, banner, endDate, banReason);
    }

    public UserListBansEntry(JsonObject json)
    {
        super(toUserProfile(json), json);
    }

    protected void onSerialization(JsonObject data)
    {
        if (this.getValue() != null)
        {
            data.addProperty("uuid", ((UserProfile)this.getValue()).getId() == null ? "" : ((UserProfile)this.getValue()).getId().toString());
            data.addProperty("name", ((UserProfile)this.getValue()).getName());
            super.onSerialization(data);
        }
    }

    private static UserProfile toUserProfile(JsonObject json)
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
