package domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing the vertical extent (altitude layer) of a
 * WeatherSubArea, in metres above sea level.
 *
 * minAltitude must be strictly less than maxAltitude.
 */
@Embeddable
public class AltitudeRange implements ValueObject {

    private static final double MIN_ALLOWED = 0.0;
    private static final double MAX_ALLOWED = 14000.0;

    private double minAltitude;
    private double maxAltitude;

    public AltitudeRange(final double minAltitude, final double maxAltitude) {
        if (minAltitude < MIN_ALLOWED || minAltitude > MAX_ALLOWED) {
            throw new IllegalArgumentException(
                    "Min altitude must be in [0, 14000] (got " + minAltitude + ")");
        }
        if (maxAltitude <= minAltitude || maxAltitude > MAX_ALLOWED) {
            throw new IllegalArgumentException(
                    "Max altitude must be in (minAltitude, 14000] (got " + maxAltitude + ")");
        }
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
    }

    public double minAltitude() { return minAltitude; }
    public double maxAltitude() { return maxAltitude; }

    /**
     * Returns true if this altitude range overlaps with other.
     * Ranges that merely touch (max == other.min) do NOT overlap.
     */
    public boolean overlapsWith(final AltitudeRange other) {
        if (other == null) return false;
        return this.minAltitude < other.maxAltitude
                && other.minAltitude < this.maxAltitude;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof AltitudeRange)) return false;
        final AltitudeRange that = (AltitudeRange) o;
        return Double.compare(minAltitude, that.minAltitude) == 0
                && Double.compare(maxAltitude, that.maxAltitude) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(minAltitude, maxAltitude); }

    @Override
    public String toString() {
        return String.format("Alt[%.0f, %.0f]m", minAltitude, maxAltitude);
    }

    protected AltitudeRange() { }
}
