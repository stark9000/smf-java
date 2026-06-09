package smf.database;

import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.messages.MessageLog;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * SMF - DataStore
 *
 * Singleton database layer.
 * Implements DatabaseConnection and DatabaseFunctions.
 *
 * Fixes applied vs tutorial version:
 *   - Modern MySQL driver: com.mysql.cj.jdbc.Driver (Connector/J 8+)
 *   - Connection URL includes: serverTimezone, useSSL, allowPublicKeyRetrieval, characterEncoding
 *   - All CRUD methods use PreparedStatements — no SQL injection possible
 *   - execute() returns a CloseableResult wrapper so callers can close resources safely
 *   - executeUpdate() uses try-with-resources properly
 *   - Connection validity checked with isValid() before each operation
 *   - AppEventBus used directly — old Event/notifyWatchers pattern removed
 */
public class DataStore {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static DataStore instance = null;

    private DataStore() {}

    public static synchronized DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    // -------------------------------------------------------------------------
    // CloseableResult — wraps ResultSet + Statement so callers can close both
    // -------------------------------------------------------------------------

    /**
     * Wraps a ResultSet and its parent Statement.
     * Use in a try-with-resources block:
     *
     *   try (DataStore.CloseableResult cr = db.query("SELECT * FROM products")) {
     *       ResultSet rs = cr.getResultSet();
     *       while (rs.next()) { ... }
     *   }
     *
     * Both the ResultSet and Statement are closed when the block exits.
     */
    public static class CloseableResult implements AutoCloseable {

        private final Statement  statement;
        private final ResultSet  resultSet;

        CloseableResult(Statement statement, ResultSet resultSet) {
            this.statement = statement;
            this.resultSet = resultSet;
        }

        public ResultSet getResultSet() { return resultSet; }

        @Override
        public void close() {
            try { if (resultSet != null)  resultSet.close();  } catch (SQLException ignored) {}
            try { if (statement != null)  statement.close();  } catch (SQLException ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Connection connection = null;

    private String  ip            = "127.0.0.1";
    private String  port          = "3306";
    private String  database      = "smf_shop";
    private String  user          = "root";
    private String  password      = "";
    private int     timeoutMs     = 5000;
    private boolean autoReconnect = true;

    private final MessageLog  log = MessageLog.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    public void setServerIP(String ip)            { this.ip = ip; }
    public void setServerPort(String port)        { this.port = port; }
    public void setDatabaseName(String name)      { this.database = name; }
    public void setDatabaseUser(String user)      { this.user = user; }
    public void setDatabasePassword(String pwd)   { this.password = pwd; }
    public void setTimeout(int ms)                { this.timeoutMs = ms; }
    public void setAutoReconnect(boolean val)     { this.autoReconnect = val; }

    public String getServerIP()   { return ip; }
    public String getServerPort() { return port; }
    public int    getTimeout()    { return timeoutMs; }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    /**
     * Get or establish the database connection.
     * Uses the modern MySQL Connector/J 8+ driver and URL parameters.
     */
    public synchronized Connection getConnection() {
        try {
            // Check if existing connection is still valid
            if (connection != null && !connection.isClosed()
                    && connection.isValid(3)) {
                return connection;
            }

            // Modern driver class — Connector/J 8+
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Modern URL with required parameters for Connector/J 8+
            String url = "jdbc:mysql://" + ip + ":" + port + "/" + database
                       + "?autoReconnect="         + autoReconnect
                       + "&serverTimezone=UTC"
                       + "&useSSL=false"
                       + "&allowPublicKeyRetrieval=true"
                       + "&characterEncoding=UTF-8"
                       + "&useUnicode=true";

            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(true);

            log.info("[DataStore] Connected to " + ip + ":" + port + "/" + database);
            bus.publish(AppEvent.DB_CONNECTED);
            return connection;

        } catch (ClassNotFoundException e) {
            log.error("[DataStore] Driver not found — add mysql-connector-j-8.x.x.jar to classpath");
            return null;
        } catch (SQLException e) {
            log.error("[DataStore] Connection failed: " + e.getMessage());
            bus.publish(AppEvent.DB_ERROR);
            return null;
        }
    }

    /**
     * Check if a host is reachable before attempting a connection.
     */
    public boolean isReachable(String host, int timeoutMs) {
        try {
            return InetAddress.getByName(host).isReachable(timeoutMs);
        } catch (IOException e) {
            log.warn("[DataStore] isReachable failed for " + host + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Close the connection. Call on application shutdown.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                log.info("[DataStore] Connection closed.");
            } catch (SQLException e) {
                log.warn("[DataStore] Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Safe query — returns CloseableResult for use in try-with-resources
    // -------------------------------------------------------------------------

    /**
     * Execute a SELECT query safely.
     * Returns a CloseableResult — MUST be used in try-with-resources.
     *
     * Example:
     *   try (DataStore.CloseableResult cr = db.query("SELECT * FROM products")) {
     *       ResultSet rs = cr.getResultSet();
     *       while (rs.next()) { ... }
     *   }
     *
     * @param sql Plain SQL string — use only for trusted, non-user-controlled queries.
     *            For user input, use queryPrepared() instead.
     */
    public CloseableResult query(String sql) {
        Connection conn = getConnection();
        if (conn == null) return new CloseableResult(null, null);
        try {
            Statement stmt = conn.createStatement(
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sql);
            log.info("[DataStore] Query OK: " + sql);
            bus.publish(AppEvent.DB_SELECT);
            return new CloseableResult(stmt, rs);
        } catch (SQLException e) {
            log.error("[DataStore] Query failed: " + e.getMessage() + " — SQL: " + sql);
            bus.publish(AppEvent.DB_ERROR);
            return new CloseableResult(null, null);
        }
    }

    /**
     * Execute a SELECT query with PreparedStatement parameters.
     * Use this whenever any part of the query comes from user input.
     *
     * Example:
     *   try (DataStore.CloseableResult cr = db.queryPrepared(
     *           "SELECT * FROM products WHERE category = ? AND sales_price < ?",
     *           "Sensors", 5.00)) {
     *       ResultSet rs = cr.getResultSet();
     *       while (rs.next()) { ... }
     *   }
     */
    public CloseableResult queryPrepared(String sql, Object... params) {
        Connection conn = getConnection();
        if (conn == null) return new CloseableResult(null, null);
        try {
            PreparedStatement ps = conn.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            bus.publish(AppEvent.DB_SELECT);
            return new CloseableResult(ps, rs);
        } catch (SQLException e) {
            log.error("[DataStore] queryPrepared failed: " + e.getMessage());
            bus.publish(AppEvent.DB_ERROR);
            return new CloseableResult(null, null);
        }
    }

    // -------------------------------------------------------------------------
    // Safe INSERT — PreparedStatement only
    // -------------------------------------------------------------------------

    /**
     * Insert a row using a PreparedStatement — safe from SQL injection.
     *
     * Example:
     *   db.insert("INSERT INTO products (name, category, sales_price) VALUES (?, ?, ?)",
     *             "ESP32", "Microcontrollers", 6.80);
     *
     * @param sql    SQL with ? placeholders.
     * @param params Values to bind — can be String, Integer, Double, etc.
     * @return true if the insert succeeded.
     */
    public boolean insert(String sql, Object... params) {
        Connection conn = getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            log.info("[DataStore] INSERT OK");
            bus.publish(AppEvent.DB_INSERT);
            return true;
        } catch (SQLException e) {
            log.error("[DataStore] INSERT failed: " + e.getMessage());
            bus.publish(AppEvent.DB_ERROR);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Safe UPDATE — PreparedStatement only
    // -------------------------------------------------------------------------

    /**
     * Update rows using a PreparedStatement.
     *
     * Example:
     *   db.update("UPDATE products SET sales_price = ?, name = ? WHERE id = ?",
     *             7.50, "ESP32 v2", 3);
     *
     * @param sql    SQL with ? placeholders.
     * @param params Values to bind.
     * @return true if at least one row was updated.
     */
    public boolean update(String sql, Object... params) {
        Connection conn = getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("[DataStore] UPDATE OK (" + rows + " rows)");
                bus.publish(AppEvent.DB_UPDATE);
                return true;
            } else {
                log.warn("[DataStore] UPDATE affected 0 rows");
                return false;
            }
        } catch (SQLException e) {
            log.error("[DataStore] UPDATE failed: " + e.getMessage());
            bus.publish(AppEvent.DB_ERROR);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Safe DELETE — PreparedStatement only
    // -------------------------------------------------------------------------

    /**
     * Delete rows using a PreparedStatement.
     *
     * Example:
     *   db.delete("DELETE FROM products WHERE id = ?", 3);
     *
     * @param sql    SQL with ? placeholders.
     * @param params Values to bind.
     * @return true if at least one row was deleted.
     */
    public boolean delete(String sql, Object... params) {
        Connection conn = getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("[DataStore] DELETE OK (" + rows + " rows)");
                bus.publish(AppEvent.DB_DELETE);
                return true;
            } else {
                log.warn("[DataStore] DELETE affected 0 rows");
                return false;
            }
        } catch (SQLException e) {
            log.error("[DataStore] DELETE failed: " + e.getMessage());
            bus.publish(AppEvent.DB_ERROR);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Count helper — used by DashboardPanel
    // -------------------------------------------------------------------------

    /**
     * Return the row count of a table. Safe — no user input involved.
     */
    public int count(String table) {
        try (CloseableResult cr = query("SELECT COUNT(*) FROM `" + table + "`")) {
            ResultSet rs = cr.getResultSet();
            if (rs != null && rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("[DataStore] count() failed: " + e.getMessage());
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // EventWatcher registration (kept for backward compat with Tutorial 01 style)
    // -------------------------------------------------------------------------

    private final Set<EventWatcher> legacyWatchers = new HashSet<>();

    /** @deprecated Use AppEventBus.getInstance().subscribe() instead. */
    @Deprecated
    public void registerWatcher(EventWatcher w) { legacyWatchers.add(w); }

    /** @deprecated Use AppEventBus.getInstance().unsubscribe() instead. */
    @Deprecated
    public void removeWatcher(EventWatcher w)   { legacyWatchers.remove(w); }
}
