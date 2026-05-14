package smf.messages;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SMF Library - SystemMessages
 *
 * Singleton implementation of MessageCenter.
 * Collects messages from anywhere in the application.
 * Useful for logging DB errors, connection status, operation results.
 *
 * Usage:
 *   SystemMessages log = SystemMessages.getInstance();
 *   log.addMessage("Connection established");
 *   log.getMessages().forEach(System.out::println);
 *
 * Original concept by: stark (originally named SystemMesseges)
 * Modernized for Java 8
 */
public class SystemMessages implements MessageCenter {

    // --- Singleton ---
    private static SystemMessages instance = null;

    private SystemMessages() {
        // private constructor - use getInstance()
    }

    /**
     * Get the single application-wide instance.
     * Thread-safe via synchronized.
     */
    public static synchronized SystemMessages getInstance() {
        if (instance == null) {
            instance = new SystemMessages();
        }
        return instance;
    }

    // --- State ---
    private final Set<String> messages = new HashSet<>();

    // --- MessageCenter implementation ---

    @Override
    public void addMessage(String message) {
        messages.add(message);
    }

    @Override
    public Set<String> getMessages() {
        // Return unmodifiable view - callers should not modify directly
        return Collections.unmodifiableSet(messages);
    }

    @Override
    public void clearMessages() {
        messages.clear();
    }

    /**
     * Convenience: print all messages to System.out.
     * Useful for debugging.
     */
    public void printAll() {
        messages.forEach(System.out::println);
    }
}
