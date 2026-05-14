package smf.messages;

import java.util.Set;

/**
 * SMF Library - MessageCenter
 *
 * A simple application-wide message bus.
 * Used to collect system messages, errors, and status updates
 * from any part of the application without direct coupling.
 *
 * Original concept by: stark (originally named MessegeCenter)
 * Modernized for Java 8 - typo corrected, raw HashSet replaced with Set<String>
 */
public interface MessageCenter {

    /**
     * Add a message to the message store.
     *
     * @param message The message string to store.
     */
    void addMessage(String message);

    /**
     * Get all stored messages.
     *
     * @return A Set of all message strings.
     */
    Set<String> getMessages();

    /**
     * Clear all stored messages.
     */
    void clearMessages();
}
