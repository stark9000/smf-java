package smf.database;

import smf.events.Event;
import smf.events.EventWatcher;
import smf.messages.SystemMessages;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * SMF Library - DataStore
 *
 * The heart of the SMF database layer.
 * A singleton that implements DatabaseConnection, DatabaseFunctions, and Event.
 *
 * This means DataStore:
 *   1. Manages the MySQL connection (DatabaseConnection)
 *   2. Provides CRUD operations (DatabaseFunctions)
 *   3. Notifies listeners when operations occur (Event)
 *
 * Usage:
 *   DataStore db = DataStore.getInstance();
 *   db.setServerIP("192.168.1.10");
 *   db.setDatabaseName("myshop");
 *   db.setDatabaseUser("root");
 *   db.setDatabasePassword("secret");
 *
 *   ResultSet rs = db.Select("products", "*", null, false);
 *   while (rs.next()) {
 *       System.out.println(rs.getString("name"));
 *   }
 *
 * Listening to events:
 *   db.registerWatcher(event -> System.out.println("DB event: " + event));
 *
 * Original concept by: stark
 * Modernized for Java 8 - try-with-resources, lambdas, generics, fixed typos
 */
public class DataStore implements DatabaseConnection, DatabaseFunctions, Event {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static DataStore instance = null;

    private DataStore() {
        watchers = new HashSet<>();
    }

    /**
     * Get the single application-wide DataStore instance.
     * Thread-safe.
     */
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Connection connection = null;

    // Default connection settings - override with setters before calling getConnection()
    private String ip          = "127.0.0.1";
    private String port        = "3306";
    private String database    = "mydb";
    private String user        = "root";
    private String password    = "";
    private int    timeoutMs   = 5000;
    private boolean autoReconnect = true;
    private boolean autoCommit    = false;

    private DatabaseMetaData metaData = null;

    // Operation status flags
    private boolean lastInsertOk = false;
    private boolean lastSelectOk = false;
    private boolean lastUpdateOk = false;
    private boolean lastDeleteOk = false;

    // Message log
    private final SystemMessages log = SystemMessages.getInstance();

    // Event watchers
    private final Set<EventWatcher> watchers;

    // -------------------------------------------------------------------------
    // DatabaseConnection implementation
    // -------------------------------------------------------------------------

    @Override
    public Connection getConnection() {
        try {
            if (!isReachable(ip, timeoutMs)) {
                log.addMessage("[DataStore] Server not reachable: " + ip);
                return null;
            }

            if (connection == null || connection.isClosed()) {
                // Java 8 compatible driver loading
                Class.forName("com.mysql.jdbc.Driver");

                String url = "jdbc:mysql://" + ip + ":" + port + "/" + database
                           + "?autoReconnect=" + autoReconnect;

                connection = DriverManager.getConnection(url, user, password);
                connection.setAutoCommit(autoCommit);
                metaData = connection.getMetaData();

                log.addMessage("[DataStore] Connected to " + ip + ":" + port + "/" + database);
            }

            return connection;

        } catch (ClassNotFoundException e) {
            log.addMessage("[DataStore] MySQL driver not found: " + e.getMessage());
            log.addMessage("[DataStore] Make sure mysql-connector-java.jar is in your classpath");
            return null;
        } catch (SQLException e) {
            log.addMessage("[DataStore] Connection failed: " + e.getMessage());
            return null;
        }
    }

    @Override public void setServerIP(String ip)           { this.ip = ip; }
    @Override public void setServerPort(String port)       { this.port = port; }
    @Override public void setDatabaseName(String name)     { this.database = name; }
    @Override public void setDatabaseUser(String user)     { this.user = user; }
    @Override public void setDatabasePassword(String pwd)  { this.password = pwd; }
    @Override public void setAutoReconnect(boolean val)    { this.autoReconnect = val; }
    @Override public void setAutoCommit(boolean val)       { this.autoCommit = val; }
    @Override public void setTimeout(int ms)               { this.timeoutMs = ms; }

    @Override public String  getServerIP()    { return ip; }
    @Override public String  getServerPort()  { return port; }
    @Override public int     getTimeout()     { return timeoutMs; }
    @Override public boolean isAutoReconnect(){ return autoReconnect; }
    @Override public boolean isAutoCommit()   { return autoCommit; }

    @Override
    public boolean isReachable(String ip, int timeoutMs) {
        try {
            return InetAddress.getByName(ip).isReachable(timeoutMs);
        } catch (IOException e) {
            log.addMessage("[DataStore] isReachable failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public DatabaseMetaData getMetaData() {
        return metaData;
    }

    // -------------------------------------------------------------------------
    // DatabaseFunctions implementation
    // -------------------------------------------------------------------------

    @Override
    public ResultSet execute(String sql) throws Exception {
        Connection conn = getConnection();
        if (conn == null) return null;

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            lastSelectOk = true;
            log.addMessage("[DataStore] SELECT OK: " + sql);
            notifyWatchers("--select");
            return rs;
        } catch (SQLException e) {
            lastSelectOk = false;
            log.addMessage("[DataStore] SELECT failed: " + e.getMessage());
            notifyWatchers("--select-error");
            return null;
        }
    }

    @Override
    public boolean executeUpdate(String sql) throws Exception {
        Connection conn = getConnection();
        if (conn == null) return false;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            log.addMessage("[DataStore] executeUpdate OK: " + sql);
            notifyWatchers("--update");
            return true;
        } catch (SQLException e) {
            log.addMessage("[DataStore] executeUpdate failed: " + e.getMessage());
            notifyWatchers("--update-error");
            return false;
        }
    }

    @Override
    public boolean Insert(String sql, String[] params) {
        Connection conn = getConnection();
        if (conn == null) {
            lastInsertOk = false;
            return false;
        }

        // Build: INSERT INTO table (col1, col2) VALUES (?,?)
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            placeholders.append(i < params.length - 1 ? "?," : "?");
        }
        String fullSql = sql + " VALUES(" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(fullSql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
            lastInsertOk = true;
            log.addMessage("[DataStore] INSERT OK: " + fullSql);
            notifyWatchers("--insert");
            return true;

        } catch (SQLException e) {
            lastInsertOk = false;
            log.addMessage("[DataStore] INSERT failed: " + e.getMessage());
            notifyWatchers("--insert-error");
            return false;
        }
    }

    @Override
    public ResultSet Select(String tableName, String columns, String condition, boolean where) {
        String sql = "SELECT " + columns + " FROM " + tableName;
        if (where && condition != null && !condition.isEmpty()) {
            sql += " WHERE " + condition;
        }
        try {
            return execute(sql);
        } catch (Exception e) {
            log.addMessage("[DataStore] Select failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean Delete(String tableName, String column, String value) {
        String sql = "DELETE FROM " + tableName + " WHERE " + column + " = '" + value + "'";
        try {
            boolean ok = executeUpdate(sql);
            lastDeleteOk = ok;
            notifyWatchers(ok ? "--delete" : "--delete-error");
            return ok;
        } catch (Exception e) {
            log.addMessage("[DataStore] Delete failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean Update(String tableName, String setClause, String condition) {
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + condition;
        try {
            boolean ok = executeUpdate(sql);
            lastUpdateOk = ok;
            notifyWatchers(ok ? "--update" : "--update-error");
            return ok;
        } catch (Exception e) {
            log.addMessage("[DataStore] Update failed: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Event implementation
    // -------------------------------------------------------------------------

    @Override
    public void registerWatcher(EventWatcher watcher) {
        watchers.add(watcher);
    }

    @Override
    public void removeWatcher(EventWatcher watcher) {
        watchers.remove(watcher);
    }

    @Override
    public void notifyWatchers(Object event) {
        // Java 8 lambda-friendly iteration
        watchers.forEach(w -> w.update(event));
    }

    // -------------------------------------------------------------------------
    // Status helpers
    // -------------------------------------------------------------------------

    public boolean wasLastInsertOk() { return lastInsertOk; }
    public boolean wasLastSelectOk() { return lastSelectOk; }
    public boolean wasLastUpdateOk() { return lastUpdateOk; }
    public boolean wasLastDeleteOk() { return lastDeleteOk; }

    /**
     * Close the connection manually when shutting down.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                log.addMessage("[DataStore] Connection closed.");
            } catch (SQLException e) {
                log.addMessage("[DataStore] Error closing connection: " + e.getMessage());
            }
        }
    }
}
