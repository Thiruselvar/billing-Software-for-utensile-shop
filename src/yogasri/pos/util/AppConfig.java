package yogasri.pos.util;

import java.io.FileInputStream;
import java.util.Properties;

public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();
    private final Properties props = new Properties();

    private AppConfig() {}

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public void load() {
        try (FileInputStream in = new FileInputStream("app.properties")) {
            props.load(in);
        } catch (Exception e) {
            System.err.println("Warning: Could not load app.properties. " + e.getMessage());
        }
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

