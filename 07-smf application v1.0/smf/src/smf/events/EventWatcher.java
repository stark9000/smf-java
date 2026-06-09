package smf.events;

/**
 * SMF Library - EventWatcher
 *
 * Any class that wants to listen to events (e.g. database operations)
 * must implement this interface and register itself with an Event source.
 *
 * Original concept by: stark
 * Modernized for Java 8
 */
public interface EventWatcher {

    /**
     * Called by the event source when something happens.
     *
     * @param event A string or object describing what happened.
     *              Example: "--insert", "--select", "--error"
     */
    void update(Object event);
}
