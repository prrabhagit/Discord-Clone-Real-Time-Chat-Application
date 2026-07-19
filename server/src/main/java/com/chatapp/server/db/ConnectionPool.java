package com.chatapp.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.chatapp.server.config.ServerConfig;

/**
 * A minimal fixed-size JDBC connection pool backed by a blocking queue.
 * Deliberately dependency-free (no HikariCP) to keep the server's footprint
 * small; sufficient for a chat app's concurrency needs.
 */
public final class ConnectionPool {

    private static volatile ConnectionPool instance;

    private final BlockingQueue<Connection> pool;

    private ConnectionPool(int size) {
        this.pool = new ArrayBlockingQueue<>(size);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found on classpath", e);
        }
        for (int i = 0; i < size; i++) {
            pool.add(createConnection());
        }
    }

    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool(ServerConfig.dbPoolSize());
                }
            }
        }
        return instance;
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(
                    ServerConfig.dbUrl(), ServerConfig.dbUser(), ServerConfig.dbPassword());
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to MySQL at " + ServerConfig.dbUrl(), e);
        }
    }

    /** Borrow a connection from the pool, waiting if none are currently available. */
    public Connection borrow() {
        try {
            Connection connection = pool.poll(10, TimeUnit.SECONDS);
            if (connection == null) {
                throw new IllegalStateException("Timed out waiting for a database connection");
            }
            if (connection.isClosed() || !connection.isValid(2)) {
                connection = createConnection();
            }
            return connection;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a database connection", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Database connection became invalid", e);
        }
    }

    /** Return a connection to the pool for reuse. */
    public void release(Connection connection) {
        if (connection != null) {
            pool.offer(connection);
        }
    }
}
