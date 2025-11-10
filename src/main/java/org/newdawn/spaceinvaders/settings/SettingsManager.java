package org.newdawn.spaceinvaders.settings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SettingsManager {
    private static final String DIR_NAME = ".spaceinvaders";
    private static final String FILE_NAME = "config.properties";
    private static final String KEY_TWO_PLAYER = "twoPlayerEnabled";

    private static Properties props = new Properties();
    private static Path configPath;

    static {
        Path dir = Path.of(System.getProperty("user.home"), DIR_NAME);
        configPath = dir.resolve(FILE_NAME);
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    props.load(in);
                }
            } else {
                // 기본값
                props.setProperty(KEY_TWO_PLAYER, "false");
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() throws IOException {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "Space Invaders Settings");
        }
    }

    public static boolean isTwoPlayerEnabled() {
        return Boolean.parseBoolean(props.getProperty(KEY_TWO_PLAYER, "false"));
    }

    public static void setTwoPlayerEnabled(boolean enabled) {
        props.setProperty(KEY_TWO_PLAYER, Boolean.toString(enabled));
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
