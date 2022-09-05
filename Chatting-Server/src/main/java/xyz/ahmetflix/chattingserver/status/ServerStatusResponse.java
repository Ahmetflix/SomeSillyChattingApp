package xyz.ahmetflix.chattingserver.status;

import com.google.gson.*;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.json.JsonUtils;

import java.lang.reflect.Type;
import java.util.UUID;

public class ServerStatusResponse {

    private String description;
    private UserCountData playerCount;
    private String favicon;

    public String getServerDescription()
    {
        return this.description;
    }

    public void setServerDescription(String motd)
    {
        this.description = motd;
    }

    public UserCountData getPlayerCountData()
    {
        return this.playerCount;
    }

    public void setPlayerCountData(UserCountData countData)
    {
        this.playerCount = countData;
    }

    public void setFavicon(String faviconBlob)
    {
        this.favicon = faviconBlob;
    }

    public String getFavicon()
    {
        return this.favicon;
    }

    public static class UserCountData
    {
        private final int maxUsers;
        private final int onlineUserCount;
        private UserProfile[] users;

        public UserCountData(int maxOnlineUsers, int onlineUsers)
        {
            this.maxUsers = maxOnlineUsers;
            this.onlineUserCount = onlineUsers;
        }

        public int getMaxUsers()
        {
            return this.maxUsers;
        }

        public int getOnlineUserCount()
        {
            return this.onlineUserCount;
        }

        public UserProfile[] getUsers()
        {
            return this.users;
        }

        public void setUsers(UserProfile[] usersIn)
        {
            this.users = usersIn;
        }

        public static class Serializer implements JsonDeserializer<UserCountData>, JsonSerializer<UserCountData>
        {
            public UserCountData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
            {
                JsonObject serializedData = JsonUtils.getJsonObject(json, "users");
                UserCountData playerCountData = new UserCountData(JsonUtils.getInt(serializedData, "max"), JsonUtils.getInt(serializedData, "online"));

                if (JsonUtils.isJsonArray(serializedData, "sample"))
                {
                    JsonArray profiles = JsonUtils.getJsonArray(serializedData, "sample");

                    if (profiles.size() > 0)
                    {
                        UserProfile[] userProfiles = new UserProfile[profiles.size()];

                        for (int i = 0; i < userProfiles.length; ++i)
                        {
                            JsonObject profile = JsonUtils.getJsonObject(profiles.get(i), "user[" + i + "]");
                            String id = JsonUtils.getString(profile, "id");
                            userProfiles[i] = new UserProfile(UUID.fromString(id), JsonUtils.getString(profile, "name"));
                        }

                        playerCountData.setUsers(userProfiles);
                    }
                }

                return playerCountData;
            }

            public JsonElement serialize(UserCountData src, Type typeOfSrc, JsonSerializationContext context)
            {
                JsonObject countDataSerialized = new JsonObject();
                countDataSerialized.addProperty("max", src.getMaxUsers());
                countDataSerialized.addProperty("online", src.getOnlineUserCount());

                if (src.getUsers() != null && src.getUsers().length > 0)
                {
                    JsonArray profiles = new JsonArray();

                    for (int i = 0; i < src.getUsers().length; ++i)
                    {
                        JsonObject profile = new JsonObject();
                        UUID uuid = src.getUsers()[i].getId();
                        profile.addProperty("id", uuid == null ? "" : uuid.toString());
                        profile.addProperty("name", src.getUsers()[i].getName());
                        profiles.add(profile);
                    }

                    countDataSerialized.add("sample", profiles);
                }

                return countDataSerialized;
            }
        }
    }

    public static class Serializer implements JsonDeserializer<ServerStatusResponse>, JsonSerializer<ServerStatusResponse>
    {
        public ServerStatusResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonobject = JsonUtils.getJsonObject(json, "status");
            ServerStatusResponse serverstatusresponse = new ServerStatusResponse();

            if (jsonobject.has("description"))
            {
                serverstatusresponse.setServerDescription(context.deserialize(jsonobject.get("description"), String.class));
            }

            if (jsonobject.has("users"))
            {
                serverstatusresponse.setPlayerCountData(context.deserialize(jsonobject.get("users"), UserCountData.class));
            }

            if (jsonobject.has("favicon"))
            {
                serverstatusresponse.setFavicon(JsonUtils.getString(jsonobject, "favicon"));
            }

            return serverstatusresponse;
        }

        public JsonElement serialize(ServerStatusResponse src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject jsonobject = new JsonObject();

            if (src.getServerDescription() != null)
            {
                jsonobject.add("description", context.serialize(src.getServerDescription()));
            }

            if (src.getPlayerCountData() != null)
            {
                jsonobject.add("users", context.serialize(src.getPlayerCountData()));
            }

            if (src.getFavicon() != null)
            {
                jsonobject.addProperty("favicon", src.getFavicon());
            }

            return jsonobject;
        }
    }

}
