package smf.events;

/**
 * SMF Library - AppEvent
 *
 * Typed event constants for the AppEventBus.
 *
 * In the original smf-lib, events were raw strings like "--insert", "--select".
 * This works but is fragile — a typo in a string never causes a compile error.
 *
 * AppEvent defines constants for all standard events so the compiler
 * catches mistakes, and your IDE can autocomplete them.
 *
 * Usage:
 *   bus.publish(AppEvent.DB_INSERT);
 *
 *   bus.subscribe(event -> {
 *       if (AppEvent.DB_INSERT.equals(event))   refreshTable();
 *       if (AppEvent.DB_DELETE.equals(event))   refreshTable();
 *       if (AppEvent.DB_ERROR.equals(event))    showErrorBadge();
 *       if (AppEvent.APP_BUSY.equals(event))    showSpinner();
 *       if (AppEvent.APP_READY.equals(event))   hideSpinner();
 *   });
 *
 * You can also extend this class and add your own constants:
 *   public class ShopEvent extends AppEvent {
 *       public static final String CART_UPDATED = "cart-updated";
 *       public static final String ORDER_PLACED  = "order-placed";
 *   }
 *
 * Original concept by: stark (the "--insert", "--select" string events in DataStore)
 * Extended and modernized for Java 8
 */
public class AppEvent {

    // Prevent instantiation — this is a constants class
    private AppEvent() {}

    // -------------------------------------------------------------------------
    // Database events — fired by DataStore
    // -------------------------------------------------------------------------

    /** A row was successfully inserted. */
    public static final String DB_INSERT     = "--db-insert";

    /** A SELECT query completed successfully. */
    public static final String DB_SELECT     = "--db-select";

    /** A row was successfully updated. */
    public static final String DB_UPDATE     = "--db-update";

    /** A row was successfully deleted. */
    public static final String DB_DELETE     = "--db-delete";

    /** Any database operation failed. */
    public static final String DB_ERROR      = "--db-error";

    /** Database connection was established. */
    public static final String DB_CONNECTED  = "--db-connected";

    /** Database connection was lost or closed. */
    public static final String DB_DISCONNECTED = "--db-disconnected";

    // -------------------------------------------------------------------------
    // Application lifecycle events
    // -------------------------------------------------------------------------

    /** Application is busy — show a loading indicator. */
    public static final String APP_BUSY      = "--app-busy";

    /** Application is ready — hide loading indicator. */
    public static final String APP_READY     = "--app-ready";

    /** User logged in. */
    public static final String USER_LOGIN    = "--user-login";

    /** User logged out. */
    public static final String USER_LOGOUT   = "--user-logout";

    // -------------------------------------------------------------------------
    // Data events — fired when domain data changes
    // -------------------------------------------------------------------------

    /** Product data changed — any table showing products should refresh. */
    public static final String DATA_CHANGED  = "--data-changed";

    /** A new item was added to the cart. */
    public static final String CART_UPDATED  = "--cart-updated";
}
