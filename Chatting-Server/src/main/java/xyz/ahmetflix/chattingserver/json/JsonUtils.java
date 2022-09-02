package xyz.ahmetflix.chattingserver.json;

import com.google.gson.*;

public class JsonUtils
{
    public static boolean isString(JsonObject object, String property)
    {
        return isJsonPrimitive(object, property) && object.getAsJsonPrimitive(property).isString();
    }

    /**
     * Is the given JsonElement a string?
     */
    public static boolean isString(JsonElement jsonElement)
    {
        return jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString();
    }

    public static boolean isBoolean(JsonObject object, String property)
    {
        return isJsonPrimitive(object, property) && object.getAsJsonPrimitive(property).isBoolean();
    }

    public static boolean isJsonArray(JsonObject object, String property)
    {
        return hasField(object, property) && object.get(property).isJsonArray();
    }

    public static boolean isJsonPrimitive(JsonObject object, String property)
    {
        return hasField(object, property) && object.get(property).isJsonPrimitive();
    }

    public static boolean hasField(JsonObject object, String property)
    {
        return object != null && object.get(property) != null;
    }

    public static String getString(JsonElement element, String logName)
    {
        if (element.isJsonPrimitive())
        {
            return element.getAsString();
        }
        else
        {
            throw new JsonSyntaxException("Expected " + logName + " to be a string, was " + toString(element));
        }
    }

    public static String getString(JsonObject object, String property)
    {
        if (object.has(property))
        {
            return getString(object.get(property), property);
        }
        else
        {
            throw new JsonSyntaxException("Missing " + property + ", expected to find a string");
        }
    }

    public static String getString(JsonObject object, String property, String defaultValue)
    {
        return object.has(property) ? getString(object.get(property), property) : defaultValue;
    }

    public static boolean getBoolean(JsonElement element, String logName)
    {
        if (element.isJsonPrimitive())
        {
            return element.getAsBoolean();
        }
        else
        {
            throw new JsonSyntaxException("Expected " + logName + " to be a Boolean, was " + toString(element));
        }
    }

    public static boolean getBoolean(JsonObject object, String property)
    {
        if (object.has(property))
        {
            return getBoolean(object.get(property), property);
        }
        else
        {
            throw new JsonSyntaxException("Missing " + property + ", expected to find a Boolean");
        }
    }

    public static boolean getBoolean(JsonObject object, String property, boolean defaultValue)
    {
        return object.has(property) ? getBoolean(object.get(property), property) : defaultValue;
    }

    public static float getFloat(JsonElement element, String logName)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            return element.getAsFloat();
        }
        else
        {
            throw new JsonSyntaxException("Expected " + logName + " to be a Float, was " + toString(element));
        }
    }

    public static float getFloat(JsonObject object, String property)
    {
        if (object.has(property))
        {
            return getFloat(object.get(property), property);
        }
        else
        {
            throw new JsonSyntaxException("Missing " + property + ", expected to find a Float");
        }
    }

    public static float getFloat(JsonObject object, String property, float defaultValue)
    {
        return object.has(property) ? getFloat(object.get(property), property) : defaultValue;
    }

    public static int getInt(JsonElement element, String logName)
    {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
        {
            return element.getAsInt();
        }
        else
        {
            throw new JsonSyntaxException("Expected " + logName + " to be a Int, was " + toString(element));
        }
    }

    public static int getInt(JsonObject object, String property)
    {
        if (object.has(property))
        {
            return getInt(object.get(property), property);
        }
        else
        {
            throw new JsonSyntaxException("Missing " + property + ", expected to find a Int");
        }
    }

    public static int getInt(JsonObject object, String property, int defaultValue)
    {
        return object.has(property) ? getInt(object.get(property), property) : defaultValue;
    }

    public static JsonObject getJsonObject(JsonElement element, String logName)
    {
        if (element.isJsonObject())
        {
            return element.getAsJsonObject();
        }
        else
        {
            throw new JsonSyntaxException("Expected " + logName + " to be a JsonObject, was " + toString(element));
        }
    }

    public static JsonObject getJsonObject(JsonObject base, String key)
    {
        if (base.has(key))
        {
            return getJsonObject(base.get(key), key);
        }
        else
        {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a JsonObject");
        }
    }

    public static JsonObject getJsonObject(JsonObject object, String property, JsonObject defaultValue)
    {
        return object.has(property) ? getJsonObject(object.get(property), property) : defaultValue;
    }

    public static JsonArray getJsonArray(JsonElement element, String logName)
    {
        if (element.isJsonArray())
        {
            return element.getAsJsonArray();
        }
        else
        {
            throw new JsonSyntaxException("Expected " + logName + " to be a JsonArray, was " + toString(element));
        }
    }

    public static JsonArray getJsonArray(JsonObject object, String property)
    {
        if (object.has(property))
        {
            return getJsonArray(object.get(property), property);
        }
        else
        {
            throw new JsonSyntaxException("Missing " + property + ", expected to find a JsonArray");
        }
    }

    public static JsonArray getJsonArray(JsonObject object, String property, JsonArray defaultValue)
    {
        return object.has(property) ? getJsonArray(object.get(property), property) : defaultValue;
    }

    public static String toString(JsonElement element)
    {
        String s = org.apache.commons.lang3.StringUtils.abbreviateMiddle(String.valueOf(element), "...", 10);

        if (element == null)
        {
            return "null (missing)";
        }
        else if (element.isJsonNull())
        {
            return "null (json)";
        }
        else if (element.isJsonArray())
        {
            return "an array (" + s + ")";
        }
        else if (element.isJsonObject())
        {
            return "an object (" + s + ")";
        }
        else
        {
            if (element.isJsonPrimitive())
            {
                JsonPrimitive jsonprimitive = element.getAsJsonPrimitive();

                if (jsonprimitive.isNumber())
                {
                    return "a number (" + s + ")";
                }

                if (jsonprimitive.isBoolean())
                {
                    return "a boolean (" + s + ")";
                }
            }

            return s;
        }
    }
}
