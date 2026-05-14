package smf.events;

/**
 * SMF Library - Event
 *
 * Implemented by any class that fires events to registered watchers.
 * DataStore implements this so UI components can react to DB operations
 * without tight coupling.
 *
 * Original concept by: stark
 * Modernized for Java 8
 */
public interface Event {

    /**
     * Register a watcher to receive event notifications.
     *
     * @param watcher The EventWatcher to register.
     */
    void registerWatcher(EventWatcher watcher);

    /**
     * Remove a previously registered watcher.
     *
     * @param watcher The EventWatcher to remove.
     */
    void removeWatcher(EventWatcher watcher);

    /**
     * Notify all registered watchers of an event.
     *
     * @param event Object describing the event. Typically a String.
     */
    void notifyWatchers(Object event);
}
