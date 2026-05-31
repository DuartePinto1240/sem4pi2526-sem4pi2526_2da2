package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plain value carrier that accumulates the outcome of a bulk weather data
 * import operation.
 *
 * <p>Mutable during the import loop, then read-only by the UI.</p>
 */
public class ImportWeatherResult {

    private int importedCount;
    private int rejectedCount;
    private final List<String> errors = new ArrayList<>();

    public void recordSuccess() {
        importedCount++;
    }

    /**
     * Records one rejected record with its reason.
     *
     * @param reason human-readable description of why the record was rejected
     */
    public void recordFailure(final String reason) {
        rejectedCount++;
        errors.add(reason);
    }

    /** @return number of records successfully imported */
    public int importedCount() {
        return importedCount;
    }

    /** @return number of records rejected */
    public int rejectedCount() {
        return rejectedCount;
    }

    /** @return total records read (imported + rejected) */
    public int totalRead() {
        return importedCount + rejectedCount;
    }

    /** @return unmodifiable list of per-record error messages */
    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }
}
