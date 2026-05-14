package smf.loader;

/**
 * SMF Library - LoadResult
 *
 * Describes the outcome of loading one service during bootstrap.
 *
 * In the original smf-lib, Loader collected errors into a raw HashSet —
 * no label, no status, no distinction between a warning and a failure.
 *
 * LoadResult replaces that with a structured record:
 *   - service name
 *   - whether it succeeded
 *   - whether it is required (failure = abort) or optional (failure = warn)
 *   - error message if it failed
 *
 * Original concept by: stark (HashSet errors in Loader)
 * Modernized for Java 8
 */
public class LoadResult {

    public enum Status { OK, FAILED, SKIPPED }

    private final String  serviceName;
    private final Status  status;
    private final boolean required;
    private final String  message;

    public LoadResult(String serviceName, Status status, boolean required, String message) {
        this.serviceName = serviceName;
        this.status      = status;
        this.required    = required;
        this.message     = message;
    }

    // --- Factory helpers ---

    public static LoadResult ok(String name) {
        return new LoadResult(name, Status.OK, true, "OK");
    }

    public static LoadResult failed(String name, boolean required, String error) {
        return new LoadResult(name, Status.FAILED, required, error);
    }

    public static LoadResult skipped(String name, String reason) {
        return new LoadResult(name, Status.SKIPPED, false, reason);
    }

    // --- Getters ---

    public String  getServiceName() { return serviceName; }
    public Status  getStatus()      { return status; }
    public boolean isRequired()     { return required; }
    public String  getMessage()     { return message; }
    public boolean isOk()           { return status == Status.OK; }
    public boolean isFailed()       { return status == Status.FAILED; }

    @Override
    public String toString() {
        return String.format("[%-8s] %-30s %s",
            status, serviceName, message);
    }
}
