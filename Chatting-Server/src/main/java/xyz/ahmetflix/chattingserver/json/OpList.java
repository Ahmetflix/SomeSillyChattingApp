package xyz.ahmetflix.chattingserver.json;

import com.google.gson.JsonObject;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.io.File;

public class OpList extends UserList<UserProfile, UserListOpsEntry>
{
    public OpList(File saveFile)
    {
        super(saveFile);
    }

    protected UserListEntry<UserProfile> createEntry(JsonObject entryData)
    {
        return new UserListOpsEntry(entryData);
    }

    public String[] getKeys()
    {
        String[] astring = new String[this.getValues().size()];
        int i = 0;

        for (UserListOpsEntry userlistopsentry : this.getValues().values())
        {
            astring[i++] = (userlistopsentry.getValue()).getName();
        }

        return astring;
    }

    public boolean bypassesUserLimit(UserProfile profile)
    {
        UserListOpsEntry userlistopsentry = this.getEntry(profile);
        return userlistopsentry != null && userlistopsentry.bypassesUserLimit();
    }

    public String getObjectKey(UserProfile obj)
    {
        return obj.getId().toString();
    }

    public UserProfile getGameProfileFromName(String username)
    {
        for (UserListOpsEntry userlistopsentry : this.getValues().values())
        {
            if (username.equalsIgnoreCase((userlistopsentry.getValue()).getName()))
            {
                return userlistopsentry.getValue();
            }
        }

        return null;
    }
}
