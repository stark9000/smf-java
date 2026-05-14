package smf.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * SMF Library - DatabaseConnection
 *
 * Defines the contract for any database connection implementation.
 * By programming to this interface rather than a concrete class,
 * you can swap MySQL for SQLite or any other DB with minimal code changes.
 *
 * Original concept by: stark (originally named DataBaseConnection)
 * Modernized for Java 8
 */
public interface DatabaseConnection {

    /**
     * Get the active JDBC Connection.
     * Establishes the connection if not already connected.
     *
     * @return Connection object, or null if connection failed.
     */
    Connection getConnection();

    // --- Server / Connection config ---

    void setServerIP(String ip);
    void setServerPort(String port);
    void setDatabaseName(String name);
    void setDatabaseUser(String user);
    void setDatabasePassword(String password);
    void setAutoReconnect(boolean autoReconnect);
    void setAutoCommit(boolean autoCommit);
    void setTimeout(int timeoutMs);

    // --- Getters ---

    String getServerIP();
    String getServerPort();
    int getTimeout();
    boolean isAutoReconnect();
    boolean isAutoCommit();

    /**
     * Check if a host is reachable before attempting connection.
     *
     * @param ip        IP address to check.
     * @param timeoutMs Timeout in milliseconds.
     * @return true if reachable, false otherwise.
     */
    boolean isReachable(String ip, int timeoutMs);

    /**
     * Get JDBC metadata for the connected database.
     *
     * @return DatabaseMetaData or null if not connected.
     */
    DatabaseMetaData getMetaData();
}
