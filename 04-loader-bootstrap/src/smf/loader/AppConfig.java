package smf.loader;

import java.io.*;
import java.util.Properties;

/**
 * SMF Library - AppConfig
 *
 * Loads and saves application configuration from a .properties file.
 *
 * In the original smf-lib, connection settings (IP, port, user, password)
 * were hardcoded directly in the source. AppConfig moves them into an
 * external file so they can be changed without recompiling.
 *
 * Default config file: app.properties (in the working directory)
 * If the file doesn't exist, defaults are used and the file is created.
 *
 * Usage:
 *   AppConfig config = AppConfig.getInstance();
 *   config.load();
 *
 *   String ip   = config.get("db.host", "127.0.0.1");
 *   String port = config.get("db.port", "3306");
 *
 *   config.set("db.host", "192.168.1.10");
 *   config.save();
 *
 * Properties file example (app.properties):
 *   db.host=127.0.0.1
 *   db.port=3306
 *   db.name=myshop
 *   db.user=root
 *   db.password=
 *   app.title=My Application
 *   app.version=1.0
 *
 * Original concept by: stark (PropertyFile interface — left empty in smf-lib)
 * Fully implemented for Java 8
 */
public class AppConfig {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static AppConfig instance = null;

    private AppConfig() {
        properties = new Properties();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Properties properties;
    private String configFilePath = "app.properties";
    private boolean loaded = false;

    // -------------------------------------------------------------------------
    // File path
    // -------------------------------------------------------------------------

    /**
     * Override the default config file path.
     * Call this before load() if needed.
     *
     * @param path Path to the .properties file.
     */
    public void setConfigFile(String path) {
        this.configFilePath = path;
    }

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    /**
     * Load properties from the config file.
     * If the file doesn't exist, defaults are applied and the file is created.
     *
     * @return true if loaded successfully (or defaults were applied).
     */
    public boolean load() {
        File file = new File(configFilePath);

        if (!file.exists()) {
            applyDefaults();
            save(); // create the file with defaults
            loaded = true;
            return true;
        }

        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
            loaded = true;
            return true;
        } catch (IOException e) {
            System.err.println("[AppConfig] Failed to load: " + e.getMessage());
            applyDefaults();
            loaded = true;
            return false;
        }
    }

    /**
     * Save current properties to the config file.
     *
     * @return true if saved successfully.
     */
    public boolean save() {
        try (OutputStream out = new FileOutputStream(configFilePath)) {
            properties.store(out, "SMF Application Configuration");
            return true;
        } catch (IOException e) {
            System.err.println("[AppConfig] Failed to save: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Get / Set
    // -------------------------------------------------------------------------

    /**
     * Get a property value.
     *
     * @param key          Property key.
     * @param defaultValue Fallback if key is not found.
     * @return The property value or defaultValue.
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get a property value with no default.
     * Returns null if not found.
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get an integer property.
     *
     * @param key          Property key.
     * @param defaultValue Fallback if key is not found or not a valid integer.
     * @return Integer value.
     */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get a boolean property.
     * Recognizes "true" (case-insensitive) as true.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    /**
     * Set a property value.
     * Call save() to persist changes to disk.
     */
    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public boolean isLoaded() {
        return loaded;
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    /**
     * Apply default values for all known keys.
     * Called when the config file doesn't exist yet.
     */
    private void applyDefaults() {
        // Database
        properties.setProperty("db.host",     "127.0.0.1");
        properties.setProperty("db.port",     "3306");
        properties.setProperty("db.name",     "mydb");
        properties.setProperty("db.user",     "root");
        properties.setProperty("db.password", "");
        properties.setProperty("db.timeout",  "5000");

        // Application
        properties.setProperty("app.title",   "SMF Application");
        properties.setProperty("app.version", "1.0");
    }
}
