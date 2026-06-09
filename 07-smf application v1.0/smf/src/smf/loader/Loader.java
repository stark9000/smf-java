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
 * SMF - Loader
 *
 * Application bootstrap sequence.
 *
 * Fix vs tutorial version:
 *   - Removed db.registerWatcher() bridge — DataStore now publishes
 *     to AppEventBus directly. Old code created a double-publish path.
 */
public class Loader implements AppLoader {

    private static Loader instance = null;
    private Loader() { results = new ArrayList<>(); }
    public static synchronized Loader getInstance() {
        if (instance == null) instance = new Loader();
        return instance;
    }

    private final List<LoadResult> results;
    private boolean loaded = false;
    private int currentStep = 0;
    private int totalSteps  = 0;

    private Consumer<Integer>    onProgress = null;
    private Consumer<LoadResult> onStep     = null;

    public void onProgress(Consumer<Integer>    cb) { this.onProgress = cb; }
    public void onStep(Consumer<LoadResult>     cb) { this.onStep     = cb; }

    @Override
    public boolean load() {
        results.clear();
        loaded = false;
        totalSteps  = 5;
        currentStep = 0;

        step("MessageLog",  true, () -> MessageLog.getInstance());
        step("AppEventBus", true, () -> AppEventBus.getInstance());
        step("AppConfig",   true, () -> {
            if (!AppConfig.getInstance().load())
                throw new RuntimeException("Could not load app.properties — using defaults");
        });
        step("DataStore", true, () -> {
            AppConfig config = AppConfig.getInstance();
            DataStore db     = DataStore.getInstance();
            db.setServerIP(        config.get("db.host",     "127.0.0.1"));
            db.setServerPort(      config.get("db.port",     "3306"));
            db.setDatabaseName(    config.get("db.name",     "smf_shop"));
            db.setDatabaseUser(    config.get("db.user",     "root"));
            db.setDatabasePassword(config.get("db.password", ""));
            db.setTimeout(         config.getInt("db.timeout", 5000));
        });
        stepOptional("DB Connection Check", () -> {
            AppConfig config = AppConfig.getInstance();
            DataStore db     = DataStore.getInstance();
            if (!db.isReachable(config.get("db.host", "127.0.0.1"),
                                config.getInt("db.timeout", 5000)))
                throw new RuntimeException("DB not reachable at " + config.get("db.host","127.0.0.1"));
        });

        loaded = results.stream().filter(LoadResult::isRequired).allMatch(LoadResult::isOk);

        MessageLog log = MessageLog.getInstance();
        results.forEach(r -> {
            if (r.isOk())                            log.info(r.toString());
            else if (r.isFailed() && r.isRequired()) log.error(r.toString());
            else                                     log.warn(r.toString());
        });

        AppEventBus.getInstance().publish(loaded ? AppEvent.APP_READY : AppEvent.APP_BUSY);
        log.info(loaded ? "[Loader] Bootstrap complete" : "[Loader] Bootstrap FAILED");
        return loaded;
    }

    private void step(String name, boolean required, Runnable action) {
        currentStep++;
        try { action.run(); record(LoadResult.ok(name)); }
        catch (Exception e) { record(LoadResult.failed(name, required, e.getMessage())); }
    }

    private void stepOptional(String name, Runnable action) {
        currentStep++;
        try { action.run(); record(LoadResult.ok(name)); }
        catch (Exception e) { record(LoadResult.failed(name, false, e.getMessage())); }
    }

    private void record(LoadResult r) {
        results.add(r);
        if (onStep     != null) onStep.accept(r);
        if (onProgress != null) onProgress.accept(getProgressPercent());
    }

    @Override public boolean          isLoaded()          { return loaded; }
    @Override public List<LoadResult> getResults()        { return Collections.unmodifiableList(results); }
    @Override public int              getProgressPercent(){ return totalSteps == 0 ? 0 : (currentStep * 100) / totalSteps; }
}
