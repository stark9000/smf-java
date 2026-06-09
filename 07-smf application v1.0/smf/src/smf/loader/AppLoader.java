package smf.loader;

import java.util.List;

/**
 * SMF Library - AppLoader
 *
 * Contract for the application bootstrap sequence.
 *
 * In the original smf-lib, Loaderx defined:
 *   Load(), getErrors(), isLoaded()
 *
 * Modernized here:
 *   - Raw HashSet → List<LoadResult>
 *   - Progress reporting via a callback
 *   - Separate phases: config → services → ui
 *
 * Original concept by: stark (Loaderx interface)
 * Modernized for Java 8
 */
public interface AppLoader {

    /**
     * Run the full bootstrap sequence.
     * Fires progress callbacks as each step completes.
     *
     * @return true if all required services loaded successfully.
     */
    boolean load();

    /**
     * True if all required services loaded without error.
     */
    boolean isLoaded();

    /**
     * Get the list of load results — one per service attempted.
     */
    List<LoadResult> getResults();

    /**
     * Get load progress as a percentage (0–100).
     * Useful for showing a progress bar during startup.
     */
    int getProgressPercent();
}
