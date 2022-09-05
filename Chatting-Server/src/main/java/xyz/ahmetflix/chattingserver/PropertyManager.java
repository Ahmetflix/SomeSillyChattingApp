package xyz.ahmetflix.chattingserver;

import joptsimple.OptionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyManager {

    private static final Logger LOGGER = LogManager.getLogger();
    public final Properties properties = new Properties();
    private final File file;

    public PropertyManager(File file) {
        this.file = file;
        if (file.exists()) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                this.properties.load(inputStream);
            } catch (Exception exception) {
                PropertyManager.LOGGER.warn("Failed to load " + file, exception);
                this.generateProperties();
            }
        } else {
            PropertyManager.LOGGER.warn(file + " does not exist");
            this.generateProperties();
        }

    }

    private OptionSet options = null;

    public PropertyManager(final OptionSet options) {
        this((File) options.valueOf("config"));

        this.options = options;
    }

    private <T> T getOverride(String name, T value) {
        if ((this.options != null) && (this.options.has(name))) {
            return (T) this.options.valueOf(name);
        }

        return value;
    }

    public void generateProperties() {
        PropertyManager.LOGGER.info("Generating new properties file");
        this.savePropertiesFile();
    }

    public void savePropertiesFile() {
        FileOutputStream outputStream = null;

        try {
            if (this.file.exists() && !this.file.canWrite()) {
                return;
            }

            outputStream = new FileOutputStream(this.file);
            this.properties.store(outputStream, "Server properties");
        } catch (Exception exception) {
            PropertyManager.LOGGER.warn("Failed to save " + this.file, exception);
            this.generateProperties();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {

                }
            }

        }

    }

    public File getFile() {
        return file;
    }

    public String getString(String s, String s1) {
        if (!this.properties.containsKey(s)) {
            this.properties.setProperty(s, s1);
            this.savePropertiesFile();
            this.savePropertiesFile();
        }

        return getOverride(s, this.properties.getProperty(s, s1));
    }

    public int getInt(String s, int i) {
        try {
            return getOverride(s, Integer.parseInt(this.getString(s, "" + i)));
        } catch (Exception exception) {
            this.properties.setProperty(s, "" + i);
            this.savePropertiesFile();
            return getOverride(s, i);
        }
    }

    public long getLong(String s, long i) {
        try {
            return getOverride(s, Long.parseLong(this.getString(s, "" + i)));
        } catch (Exception exception) {
            this.properties.setProperty(s, "" + i);
            this.savePropertiesFile();
            return getOverride(s, i);
        }
    }

    public boolean getBoolean(String s, boolean flag) {
        try {
            return getOverride(s, Boolean.parseBoolean(this.getString(s, "" + flag)));
        } catch (Exception exception) {
            this.properties.setProperty(s, "" + flag);
            this.savePropertiesFile();
            return getOverride(s, flag);
        }
    }

    public void setProperty(String s, Object object) {
        this.properties.setProperty(s, "" + object);
    }
}