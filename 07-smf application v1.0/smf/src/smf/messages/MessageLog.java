package smf.messages;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SMF Library - MessageLog
 *
 * An enhanced version of SystemMessages.
 *
 * The original SystemMessages used a plain HashSet<String> — simple and effective,
 * but it had limitations:
 *   - No ordering (HashSet has no guaranteed order)
 *   - No severity levels (errors and info looked the same)
 *   - No timestamps
 *   - Duplicate messages were silently dropped (HashSet behavior)
 *
 * MessageLog fixes all of these:
 *   - Messages are stored in insertion order (ArrayList)
 *   - Each message has a Level: INFO, WARN, ERROR
 *   - Each message has a timestamp
 *   - Duplicates are allowed (every event is recorded)
 *   - Can filter by level
 *
 * Singleton — one log shared across the whole application.
 *
 * Usage:
 *   MessageLog log = MessageLog.getInstance();
 *   log.info("Connection established");
 *   log.warn("Retrying connection...");
 *   log.error("Insert failed: " + e.getMessage());
 *
 *   log.getAll().forEach(System.out::println);
 *   log.getByLevel(Level.ERROR).forEach(System.out::println);
 *
 * Original concept by: stark (SystemMesseges / MessegeCenter)
 * Modernized for Java 8 — levels, timestamps, ordered storage
 */
public class MessageLog {

    // -------------------------------------------------------------------------
    // Level enum
    // -------------------------------------------------------------------------

    public enum Level {
        INFO, WARN, ERROR
    }

    // -------------------------------------------------------------------------
    // Entry - one log record
    // -------------------------------------------------------------------------

    public static class Entry {

        private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

        private final Level         level;
        private final String        message;
        private final LocalDateTime timestamp;

        Entry(Level level, String message) {
            this.level     = level;
            this.message   = message;
            this.timestamp = LocalDateTime.now();
        }

        public Level         getLevel()     { return level; }
        public String        getMessage()   { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "[" + timestamp.format(FMT) + "] "
                 + "[" + level + "] "
                 + message;
        }
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static MessageLog instance = null;

    private MessageLog() {
        entries = new ArrayList<>();
    }

    public static synchronized MessageLog getInstance() {
        if (instance == null) {
            instance = new MessageLog();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final List<Entry> entries;

    // -------------------------------------------------------------------------
    // Logging methods
    // -------------------------------------------------------------------------

    /** Log an informational message. */
    public synchronized void info(String message) {
        entries.add(new Entry(Level.INFO, message));
    }

    /** Log a warning. */
    public synchronized void warn(String message) {
        entries.add(new Entry(Level.WARN, message));
    }

    /** Log an error. */
    public synchronized void error(String message) {
        entries.add(new Entry(Level.ERROR, message));
    }

    // -------------------------------------------------------------------------
    // Retrieval
    // -------------------------------------------------------------------------

    /**
     * Get all log entries in insertion order.
     */
    public synchronized List<Entry> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Get only entries matching a specific level.
     *
     * Example:
     *   log.getByLevel(Level.ERROR).forEach(System.out::println);
     */
    public synchronized List<Entry> getByLevel(Level level) {
        List<Entry> filtered = new ArrayList<>();
        for (Entry e : entries) {
            if (e.getLevel() == level) filtered.add(e);
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * True if there are any ERROR level entries.
     * Useful for showing an error badge in the UI.
     */
    public synchronized boolean hasErrors() {
        return entries.stream().anyMatch(e -> e.getLevel() == Level.ERROR);
    }

    /**
     * Total number of entries.
     */
    public synchronized int size() {
        return entries.size();
    }

    /**
     * Clear all entries.
     */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * Print all entries to System.out.
     * Useful for debugging.
     */
    public void printAll() {
        getAll().forEach(System.out::println);
    }
}
