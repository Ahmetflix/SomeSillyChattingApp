package xyz.ahmetflix.chattingserver.json;

import com.google.gson.JsonObject;
import xyz.ahmetflix.chattingserver.UserProfile;

import java.io.File;

public class UserListBans extends UserList<UserProfile, UserListBansEntry>
{
    public UserListBans(File bansFile)
    {
        super(bansFile);
    }

    protected UserListEntry<UserProfile> createEntry(JsonObject entryData)
    {
        return new UserListBansEntry(entryData);
    }

    public boolean isBanned(UserProfile profile)
    {
        return this.hasEntry(profile);
    }

    public String[] getKeys()
    {
        String[] astring = new String[this.getValues().size()];
        int i = 0;

        for (UserListBansEntry userlistbansentry : this.getValues().values())
        {
            astring[i++] = (userlistbansentry.getValue()).getName();
        }

        return astring;
    }

    /**
     * Gets the key value for the given object
     */
    public String getObjectKey(UserProfile obj)
    {
        return obj.getId().toString();
    }

    public UserProfile isUsernameBanned(String username)
    {
        for (UserListBansEntry userlistbansentry : this.getValues().values())
        {
            if (username.equalsIgnoreCase(((UserProfile)userlistbansentry.getValue()).getName()))
            {
                return (UserProfile)userlistbansentry.getValue();
            }
        }

        return null;
    }
}
