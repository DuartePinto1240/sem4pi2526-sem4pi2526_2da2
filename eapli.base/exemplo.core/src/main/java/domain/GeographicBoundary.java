package domain;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing the geographic bounding box of an Air Control Area.
 *
 * <p>Validation rules enforced by this class:</p>
 * <ul>
 *   <li>Latitude values must be in the range [-90, 90].</li>
 *   <li>Longitude values must be in the range [-180, 180].</li>
 *   <li>{@code minLat} must be strictly less than {@code maxLat}.</li>
 *   <li>{@code minLon} must be strictly less than {@code maxLon}.</li>
 *   <li>{@code minAltitude} must be &gt;= 0.</li>
 *   <li>{@code maxAltitude} must be &lt;= {@value #MAX_ALTITUDE_METRES} m.</li>
 *   <li>{@code minAltitude} must be strictly less than {@code maxAltitude}.</li>
 * </ul>
 *
 * <p>Overlap detection between two bounding boxes is provided via
 * {@link #overlapsWith(GeographicBoundary)}, used by the repository
 * uniqueness check to ensure no two areas share airspace.</p>
 */
@Embeddable
public class GeographicBoundary implements ValueObject {

     // Minimum altitude (metres) for any air control area — always sea level.
    public static final double MIN_ALTITUDE_METRES = 0.0;

     //Maximum altitude (metres) for any air control area
    public static final double MAX_ALTITUDE_METRES = 14000;

    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private double minAltitude;
    private double maxAltitude;

    /**
     * @param minLat      minimum latitude  [-90, 90]
     * @param maxLat      maximum latitude  [-90, 90], must be &gt; minLat
     * @param minLon      minimum longitude [-180, 180]
     * @param maxLon      maximum longitude [-180, 180], must be &gt; minLon
     * @param minAltitude minimum altitude in metres, must be &gt;= 0
     * @param maxAltitude maximum altitude in metres, must be &gt; minAltitude
     *                    and &lt;= {@value #MAX_ALTITUDE_METRES}
     * @throws IllegalArgumentException if any argument fails validation
     */
    public GeographicBoundary(final double minLat, final double maxLat,
                              final double minLon, final double maxLon,
                              final double minAltitude, final double maxAltitude) {

        if (minLat < -90 || minLat > 90 || maxLat < -90 || maxLat > 90) {
            throw new IllegalArgumentException(
                    "Latitude values must be between -90 and 90.");
        }
        if (minLat >= maxLat) {
            throw new IllegalArgumentException(
                    "minLat must be strictly less than maxLat.");
        }

        if (minLon < -180 || minLon > 180 || maxLon < -180 || maxLon > 180) {
            throw new IllegalArgumentException(
                    "Longitude values must be between -180 and 180.");
        }
        if (minLon >= maxLon) {
            throw new IllegalArgumentException(
                    "minLon must be strictly less than maxLon.");
        }

        if (minAltitude < 0) {
            throw new IllegalArgumentException(
                    "Minimum altitude cannot be negative.");
        }
        if (maxAltitude > MAX_ALTITUDE_METRES) {
            throw new IllegalArgumentException(
                    "Maximum altitude cannot exceed " + MAX_ALTITUDE_METRES + " m.");
        }
        if (minAltitude >= maxAltitude) {
            throw new IllegalArgumentException(
                    "minAltitude must be strictly less than maxAltitude.");
        }

        this.minLat      = minLat;
        this.maxLat      = maxLat;
        this.minLon      = minLon;
        this.maxLon      = maxLon;
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
    }

    /**
     * Convenience constructor — uses the system-wide altitude defaults
     * ({@value #MIN_ALTITUDE_METRES} m to {@value #MAX_ALTITUDE_METRES} m).
     *
     * @param minLat minimum latitude  [-90, 90]
     * @param maxLat maximum latitude  [-90, 90], must be &gt; minLat
     * @param minLon minimum longitude [-180, 180]
     * @param maxLon maximum longitude [-180, 180], must be &gt; minLon
     * @throws IllegalArgumentException if any argument fails validation
     */
    public GeographicBoundary(final double minLat, final double maxLat,
                              final double minLon, final double maxLon) {
        this(minLat, maxLat, minLon, maxLon,
                MIN_ALTITUDE_METRES, MAX_ALTITUDE_METRES);
    }

    /** @return minimum latitude of the bounding box */
    public double minLat() { return minLat; }

    /** @return maximum latitude of the bounding box */
    public double maxLat() { return maxLat; }

    /** @return minimum longitude of the bounding box */
    public double minLon() { return minLon; }

    /** @return maximum longitude of the bounding box */
    public double maxLon() { return maxLon; }

    /** @return minimum altitude of the bounding box (metres) */
    public double minAltitude() { return minAltitude; }

    /** @return maximum altitude of the bounding box (metres) */
    public double maxAltitude() { return maxAltitude; }

    /**
     * Returns {@code true} if this bounding box overlaps with {@code other}.
     *
     * <p>Two rectangles overlap when they intersect on both axes.
     * They do NOT overlap if one is entirely above, below, left, or right
     * of the other. Boundaries that merely touch (share an edge) are
     *
     * @param other the other boundary to check against
     * @return {@code true} if the two areas share any airspace
     */
    public boolean overlapsWith(final GeographicBoundary other) {
        if (other == null) return false;
        final boolean noLatOverlap = this.maxLat <= other.minLat || other.maxLat <= this.minLat;
        final boolean noLonOverlap = this.maxLon <= other.minLon || other.maxLon <= this.minLon;
        return !noLatOverlap && !noLonOverlap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof GeographicBoundary)) return false;
        final GeographicBoundary that = (GeographicBoundary) o;
        return Double.compare(that.minLat,      minLat)      == 0
                && Double.compare(that.maxLat,      maxLat)      == 0
                && Double.compare(that.minLon,      minLon)      == 0
                && Double.compare(that.maxLon,      maxLon)      == 0
                && Double.compare(that.minAltitude, minAltitude) == 0
                && Double.compare(that.maxAltitude, maxAltitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLat, maxLat, minLon, maxLon, minAltitude, maxAltitude);
    }

    @Override
    public String toString() {
        return String.format(
                "Lat[%.4f, %.4f] Lon[%.4f, %.4f] Alt[%.0f, %.0f]m",
                minLat, maxLat, minLon, maxLon, minAltitude, maxAltitude);
    }

    /** No-arg constructor required by JPA. */
    protected GeographicBoundary() {
    }
}
