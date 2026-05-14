package smf.events;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * SMF Library - AppEventBus
 *
 * An application-wide event bus that any component can publish to
 * and any component can listen to — without them knowing about each other.
 *
 * This extends the original smf-lib EventWatcher pattern from a
 * 1-to-many (DataStore → watchers) into a many-to-many system:
 *   - Multiple sources can fire events (DB, serial port, timers, UI actions)
 *   - Multiple listeners can react independently
 *
 * Singleton — one bus shared across the whole application.
 *
 * Usage (publishing):
 *   AppEventBus.getInstance().publish("--product-saved");
 *   AppEventBus.getInstance().publish(new MyEvent("data", 42));
 *
 * Usage (subscribing with lambda):
 *   AppEventBus.getInstance().subscribe(event -> {
 *       if ("--product-saved".equals(event)) refreshTable();
 *   });
 *
 * Usage (subscribing with class implementing EventWatcher):
 *   AppEventBus.getInstance().subscribe(myPanel);
 *
 * Original concept by: stark (EventWatcher + Event interfaces)
 * Extended and modernized for Java 8
 */
public class AppEventBus {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static AppEventBus instance = null;

    private AppEventBus() {
        watchers = new HashSet<>();
    }

    public static synchronized AppEventBus getInstance() {
        if (instance == null) {
            instance = new AppEventBus();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Set<EventWatcher> watchers;

    // -------------------------------------------------------------------------
    // Subscribe
    // -------------------------------------------------------------------------

    /**
     * Subscribe a class that implements EventWatcher.
     * Its update() method will be called on every event.
     *
     * @param watcher Any class implementing EventWatcher.
     */
    public synchronized void subscribe(EventWatcher watcher) {
        watchers.add(watcher);
    }

    /**
     * Subscribe using a Java 8 lambda.
     * Convenient when you don't want to implement EventWatcher on your class.
     *
     * Returns the EventWatcher wrapping the lambda,
     * so you can unsubscribe later if needed.
     *
     * Example:
     *   EventWatcher w = bus.subscribe(e -> System.out.println(e));
     *   bus.unsubscribe(w); // later
     *
     * @param handler Lambda receiving the event object.
     * @return The EventWatcher wrapping the lambda.
     */
    public synchronized EventWatcher subscribe(Consumer<Object> handler) {
        EventWatcher watcher = handler::accept;
        watchers.add(watcher);
        return watcher;
    }

    // -------------------------------------------------------------------------
    // Unsubscribe
    // -------------------------------------------------------------------------

    /**
     * Remove a previously registered watcher.
     * Important for panels/dialogs that are closed — prevent memory leaks.
     *
     * @param watcher The watcher to remove.
     */
    public synchronized void unsubscribe(EventWatcher watcher) {
        watchers.remove(watcher);
    }

    // -------------------------------------------------------------------------
    // Publish
    // -------------------------------------------------------------------------

    /**
     * Publish an event to all subscribers.
     * All registered watchers receive the event immediately.
     *
     * @param event Any object — typically a String constant or a typed event object.
     */
    public void publish(Object event) {
        // Copy to avoid ConcurrentModificationException if a watcher unsubscribes during notify
        Set<EventWatcher> snapshot;
        synchronized (this) {
            snapshot = new HashSet<>(watchers);
        }
        snapshot.forEach(w -> w.update(event));
    }

    /**
     * How many watchers are currently subscribed.
     * Useful for debugging.
     */
    public synchronized int subscriberCount() {
        return watchers.size();
    }
}
