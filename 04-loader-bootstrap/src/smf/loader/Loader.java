package smf.loader;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.messages.MessageLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * SMF Library - Loader
 *
 * The application bootstrap sequence.
 * Initializes all singletons and services in the correct order
 * before the main UI window is shown.
 *
 * Boot sequence:
 *   1. MessageLog       — logging must be first so everything else can use it
 *   2. AppEventBus      — event bus must be ready before any subscribers register
 *   3. AppConfig        — load settings from app.properties
 *   4. DataStore        — configure DB connection from loaded config
 *   5. [optional steps] — any additional services your app needs
 *
 * The original smf-lib used Class.forName() reflection to load services
 * by class name string. This modernized version uses direct singleton
 * calls — cleaner, type-safe, and the IDE can follow references.
 *
 * Usage:
 *   Loader loader = Loader.getInstance();
 *   loader.onProgress(pct -> progressBar.setValue(pct));
 *   loader.onStep(result -> statusLabel.setText(result.getServiceName()));
 *
 *   if (loader.load()) {
 *       showMainWindow();
 *   } else {
 *       showErrorDialog(loader.getResults());
 *   }
 *
 * Original concept by: stark (Loader / Loaderx)
 * Modernized for Java 8 — no reflection, typed results, progress callbacks
 */
public class Loader implements AppLoader {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static Loader instance = null;

    private Loader() {
        results = new ArrayList<>();
    }

    public static synchronized Loader getInstance() {
        if (instance == null) {
            instance = new Loader();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final List<LoadResult> results;
    private boolean loaded = false;
    private int     currentStep = 0;
    private int     totalSteps  = 0;

    // Optional callbacks
    private Consumer<Integer>    onProgress = null;  // receives 0-100
    private Consumer<LoadResult> onStep     = null;  // receives each result

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    /**
     * Set a callback that receives progress percentage (0–100).
     * Called after each step completes.
     * Use this to drive a JProgressBar during splash screen.
     *
     * @param callback Consumer receiving progress (0-100).
     */
    public void onProgress(Consumer<Integer> callback) {
        this.onProgress = callback;
    }

    /**
     * Set a callback that receives each LoadResult as it happens.
     * Use this to update a status label showing what is loading.
     *
     * @param callback Consumer receiving each LoadResult.
     */
    public void onStep(Consumer<LoadResult> callback) {
        this.onStep = callback;
    }

    // -------------------------------------------------------------------------
    // AppLoader implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean load() {
        results.clear();
        loaded  = false;
        totalSteps  = 5; // update this if you add more steps
        currentStep = 0;

        // Step 1 — MessageLog (logging must be first)
        step("MessageLog", true, () -> {
            MessageLog.getInstance(); // initialize singleton
        });

        // Step 2 — AppEventBus
        step("AppEventBus", true, () -> {
            AppEventBus.getInstance();
        });

        // Step 3 — AppConfig (load app.properties)
        step("AppConfig", true, () -> {
            AppConfig config = AppConfig.getInstance();
            if (!config.load()) {
                throw new RuntimeException("Could not load app.properties — using defaults");
            }
        });

        // Step 4 — DataStore (configure from loaded config)
        step("DataStore", true, () -> {
            AppConfig config = AppConfig.getInstance();
            DataStore db = DataStore.getInstance();
            db.setServerIP(      config.get("db.host",     "127.0.0.1"));
            db.setServerPort(    config.get("db.port",     "3306"));
            db.setDatabaseName(  config.get("db.name",     "mydb"));
            db.setDatabaseUser(  config.get("db.user",     "root"));
            db.setDatabasePassword(config.get("db.password", ""));
            db.setTimeout(       config.getInt("db.timeout", 5000));

            // Wire DataStore events into the AppEventBus
            // So any subscriber to the bus automatically hears DB events
            AppEventBus bus = AppEventBus.getInstance();
            db.registerWatcher(event -> bus.publish(event));
        });

        // Step 5 — Verify DB connection (optional but recommended)
        stepOptional("DB Connection Check", () -> {
            DataStore db = DataStore.getInstance();
            AppConfig config = AppConfig.getInstance();
            String ip = config.get("db.host", "127.0.0.1");
            int timeout = config.getInt("db.timeout", 5000);
            if (!db.isReachable(ip, timeout)) {
                throw new RuntimeException("Database server not reachable at " + ip);
            }
        });

        // All required steps loaded?
        loaded = results.stream()
            .filter(LoadResult::isRequired)
            .allMatch(LoadResult::isOk);

        // Log summary
        MessageLog log = MessageLog.getInstance();
        results.forEach(r -> {
            if (r.isOk())     log.info(r.toString());
            else if (r.isFailed() && r.isRequired()) log.error(r.toString());
            else              log.warn(r.toString());
        });

        if (loaded) {
            AppEventBus.getInstance().publish(AppEvent.APP_READY);
            log.info("[Loader] Bootstrap complete — all required services OK");
        } else {
            log.error("[Loader] Bootstrap FAILED — check results for details");
        }

        return loaded;
    }

    // -------------------------------------------------------------------------
    // Step helpers
    // -------------------------------------------------------------------------

    /**
     * Run a required bootstrap step.
     * If it throws, the result is FAILED and load() will return false.
     */
    private void step(String name, boolean required, Runnable action) {
        currentStep++;
        try {
            action.run();
            record(LoadResult.ok(name), required);
        } catch (Exception e) {
            record(LoadResult.failed(name, required, e.getMessage()), required);
        }
    }

    /**
     * Run an optional bootstrap step.
     * If it throws, the result is FAILED but load() can still return true.
     */
    private void stepOptional(String name, Runnable action) {
        currentStep++;
        try {
            action.run();
            record(LoadResult.ok(name), false);
        } catch (Exception e) {
            record(LoadResult.failed(name, false, e.getMessage()), false);
        }
    }

    private void record(LoadResult result, boolean required) {
        results.add(result);

        // Fire callbacks
        if (onStep != null)     onStep.accept(result);
        if (onProgress != null) onProgress.accept(getProgressPercent());
    }

    // -------------------------------------------------------------------------
    // AppLoader getters
    // -------------------------------------------------------------------------

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public List<LoadResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    @Override
    public int getProgressPercent() {
        if (totalSteps == 0) return 0;
        return (currentStep * 100) / totalSteps;
    }
}
