package xyz.ahmetflix.chattingserver.json;

import com.google.gson.JsonObject;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.io.File;

public class WhiteList extends UserList<UserProfile, UserListWhitelistEntry>
{
    public WhiteList(File file)
    {
        super(file);
    }

    protected UserListEntry<UserProfile> createEntry(JsonObject entryData)
    {
        return new UserListWhitelistEntry(entryData);
    }

    public String[] getKeys()
    {
        String[] astring = new String[this.getValues().size()];
        int i = 0;

        for (UserListWhitelistEntry userlistwhitelistentry : this.getValues().values())
        {
            astring[i++] = (userlistwhitelistentry.getValue()).getName();
        }

        return astring;
    }

    public String getObjectKey(UserProfile obj)
    {
        return obj.getId().toString();
    }

    public UserProfile getBannedProfile(String name)
    {
        for (UserListWhitelistEntry userlistwhitelistentry : this.getValues().values())
        {
            if (name.equalsIgnoreCase((userlistwhitelistentry.getValue()).getName()))
            {
                return userlistwhitelistentry.getValue();
            }
        }

        return null;
    }
}
