package com.chatapp.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ClientConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPERTIES.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load client config.properties", e);
        }
    }

    private ClientConfig() {
    }

    public static String serverHost() {
        return PROPERTIES.getProperty("server.host", "localhost");
    }

    public static int serverPort() {
        return Integer.parseInt(PROPERTIES.getProperty("server.port", "5555"));
    }
}
