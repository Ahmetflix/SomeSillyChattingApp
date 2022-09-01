package xyz.ahmetflix.chattingserver;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserCache {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private final Map<String, UserCacheEntry> nameMap = Maps.newHashMap();
    private final Map<UUID, UserCache.UserCacheEntry> uuidMap = Maps.newHashMap();
    private final java.util.Deque<UserProfile> profiles = new java.util.concurrent.LinkedBlockingDeque<>();
    private final Server server;
    private final File cacheFile;
    protected final Gson gson;
    private static final ParameterizedType TYPE = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[] { UserCache.UserCacheEntry.class};
        }

        public Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };

    public UserCache(Server server, File cacheFile) {
        this.server = server;
        this.cacheFile = cacheFile;

        GsonBuilder gsonbuilder = new GsonBuilder();

        gsonbuilder.registerTypeHierarchyAdapter(UserCache.UserCacheEntry.class, new UserCache.BanEntrySerializer());
        this.gson = gsonbuilder.create();
        this.load();
    }

    public void addNewProfile(UserProfile userProfile) {
        this.addNewProfile(userProfile, (Date) null);
    }

    private void addNewProfile(UserProfile userProfile, Date date) {
        UUID uuid = userProfile.getId();

        if (date == null) {
            Calendar calendar = Calendar.getInstance();

            calendar.setTime(new Date());
            calendar.add(Calendar.MONTH, 1);
            date = calendar.getTime();
        }

        UserCache.UserCacheEntry entry = new UserCache.UserCacheEntry(userProfile, date);

        if (this.uuidMap.containsKey(uuid)) {
            UserCache.UserCacheEntry entry1 = (UserCache.UserCacheEntry) this.uuidMap.get(uuid);

            this.nameMap.remove(entry1.getUserProfile().getName().toLowerCase(Locale.ROOT));
            this.profiles.remove(userProfile);
        }

        this.nameMap.put(userProfile.getName().toLowerCase(Locale.ROOT), entry);
        this.uuidMap.put(uuid, entry);
        this.profiles.addFirst(userProfile);
        this.save();
    }

    public void save() {
        BufferedWriter bufferedwriter = null;
        try {
            String json = this.gson.toJson(this.getEntries(1000));
            bufferedwriter = Files.newBufferedWriter(this.cacheFile.toPath(), Charsets.UTF_8, StandardOpenOption.WRITE);
            bufferedwriter.write(json);
        } catch (IOException ignored) {
        } finally {
            IOUtils.closeQuietly(bufferedwriter);
        }
    }

    public UserProfile getProfile(String name) {
        String lowerCasedName = name.toLowerCase(Locale.ROOT);
        UserCacheEntry entry = this.nameMap.get(lowerCasedName);

        if (entry != null && (new Date()).getTime() >= entry.getDate().getTime()) {
            this.uuidMap.remove(entry.getUserProfile().getId());
            this.nameMap.remove(entry.getUserProfile().getName().toLowerCase(Locale.ROOT));
            this.profiles.remove(entry.getUserProfile());
            entry = null;
        }

        UserProfile userProfile;

        if (entry != null) {
            userProfile = entry.getUserProfile();
            this.profiles.remove(userProfile);
            this.profiles.addFirst(userProfile);
        } else {
            userProfile = UserProfile.makeUUIDIfNotExists(new UserProfile(null, name)); // Spigot - use correct case for offline players
            this.addNewProfile(userProfile);
            entry = (UserCacheEntry) this.nameMap.get(lowerCasedName);
        }

        this.save();
        return entry == null ? null : entry.getUserProfile();
    }

    private List<UserCacheEntry> getEntries(int size) {
        List<UserCacheEntry> entries = Lists.newArrayList();
        List<UserProfile> profiles = Lists.newArrayList(Iterators.limit(this.profiles.iterator(), size));

        for (UserProfile profile : profiles) {
            UserCacheEntry entry = this.getEntryByUUID(profile.getId());

            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private UserCacheEntry getEntryByUUID(UUID uuid) {
        UserCacheEntry entry = this.uuidMap.get(uuid);

        if (entry != null) {
            UserProfile profile = entry.getUserProfile();

            this.profiles.remove(profile);
            this.profiles.addFirst(profile);
        }

        return entry;
    }

    public UserProfile getProfileByUUID(UUID uuid) {
        UserCacheEntry entry = this.uuidMap.get(uuid);

        return entry == null ? null : entry.getUserProfile();
    }

    public void load() {
        BufferedReader bufferedreader = null;

        try {
            bufferedreader = Files.newBufferedReader(this.cacheFile.toPath(), Charsets.UTF_8);
            List<UserCacheEntry> list = this.gson.fromJson(bufferedreader, UserCache.TYPE);

            this.nameMap.clear();
            this.uuidMap.clear();
            this.profiles.clear();

            for (UserCacheEntry entry : Lists.reverse(list)) {
                if (entry != null) {
                    this.addNewProfile(entry.getUserProfile(), entry.getDate());
                }
            }
        } catch (com.google.gson.JsonSyntaxException ex) {
            LOGGER.warn( "Usercache.json is corrupted or has bad formatting. Deleting it to prevent further issues." );
            this.cacheFile.delete();
        } catch (IOException ignored) {

        } finally {
            IOUtils.closeQuietly(bufferedreader);
        }

    }


    static class UserCacheEntry {

        private final UserProfile userProfile;
        private final Date date;

        private UserCacheEntry(UserProfile userProfile, Date date) {
            this.userProfile = userProfile;
            this.date = date;
        }

        public UserProfile getUserProfile() {
            return this.userProfile;
        }

        public Date getDate() {
            return this.date;
        }
    }

    static class BanEntrySerializer implements JsonDeserializer<UserCacheEntry>, JsonSerializer<UserCacheEntry> {

        private BanEntrySerializer() {}


        @Override
        public UserCacheEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                JsonElement nameElement = object.get("name");
                JsonElement uuidElement = object.get("uuid");
                JsonElement expiresOnElement = object.get("expiresOn");

                if (nameElement != null && uuidElement != null) {
                    String uuidStr = uuidElement.getAsString();
                    String name = nameElement.getAsString();

                    Date date = null;

                    if (expiresOnElement != null) {
                        try {
                            date = UserCache.dateFormat.parse(expiresOnElement.getAsString());
                        } catch (ParseException e) {
                            date = null;
                        }
                    }

                    if (name != null && uuidStr != null) {
                        UUID uuid;

                        try {
                            uuid = UUID.fromString(uuidStr);
                        } catch (Throwable throwable) {
                            return null;
                        }

                        return new UserCacheEntry(new UserProfile(uuid, name), date);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public JsonElement serialize(UserCacheEntry src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            object.addProperty("name", src.getUserProfile().getName());
            UUID uuid = src.getUserProfile().getId();

            object.addProperty("uuid", uuid == null ? "" : uuid.toString());
            object.addProperty("expiresOn", dateFormat.format(src.getDate()));
            return object;
        }
    }

}
