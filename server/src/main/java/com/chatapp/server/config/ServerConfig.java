package com.chatapp.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads server configuration from {@code config.properties} on the classpath,
 * optionally overridden by a file path passed via the {@code chatapp.config}
 * system property (useful for deployment without rebuilding the jar).
 */
public final class ServerConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        load();
    }

    private ServerConfig() {
    }

    private static void load() {
        // 1) defaults bundled in the jar
        try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPERTIES.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load bundled config.properties", e);
        }

        // 2) optional external override
        String externalPath = System.getProperty("chatapp.config");
        if (externalPath != null) {
            Path path = Path.of(externalPath);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    PROPERTIES.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load external config: " + externalPath, e);
                }
            }
        }
    }

    public static String dbUrl() {
        return PROPERTIES.getProperty("db.url");
    }

    public static String dbUser() {
        return PROPERTIES.getProperty("db.user");
    }

    public static String dbPassword() {
        return PROPERTIES.getProperty("db.password");
    }

    public static int dbPoolSize() {
        return Integer.parseInt(PROPERTIES.getProperty("db.pool.size", "10"));
    }

    public static int serverPort() {
        return Integer.parseInt(PROPERTIES.getProperty("server.port", "5555"));
    }
}
