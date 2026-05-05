package domain;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object encapsulating the geographic coordinates of an airport.
 *
 * <p>Distinct from any coordinate type used in {@link GeographicBoundary}
 * to preserve semantic clarity: this represents a single point location,
 * not a boundary vertex.</p>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>Latitude must be in the range [-90, 90].</li>
 *   <li>Longitude must be in the range [-180, 180].</li>
 * </ul>
 */
@Embeddable
public class AirportCoordinates implements ValueObject {

    private double latitude;
    private double longitude;

    /**
     * Creates a new AirportCoordinates.
     *
     * @param latitude  geographic latitude  in decimal degrees [-90, 90]
     * @param longitude geographic longitude in decimal degrees [-180, 180]
     * @throws IllegalArgumentException if any value is out of range
     */
    public AirportCoordinates(final double latitude, final double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90, got: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180, got: " + longitude);
        }
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    /** @return latitude in decimal degrees */
    public double latitude() {
        return latitude;
    }

    /** @return longitude in decimal degrees */
    public double longitude() {
        return longitude;
    }

    /**
     * Returns {@code true} if this point lies within the given bounding box.
     *
     * <p>Used by the UI and the service to warn the operator when the airport
     * coordinates fall outside the selected air control area's boundary.</p>
     *
     * @param boundary the geographic bounding box to test against
     * @return {@code true} if the point is inside (or on the edge of) the boundary
     */
    public boolean isWithinBoundary(final GeographicBoundary boundary) {
        if (boundary == null) return false;
        return latitude  >= boundary.minLat() && latitude  <= boundary.maxLat()
            && longitude >= boundary.minLon() && longitude <= boundary.maxLon();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportCoordinates)) return false;
        final AirportCoordinates that = (AirportCoordinates) o;
        return Double.compare(that.latitude,  latitude)  == 0
            && Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("(%.6f, %.6f)", latitude, longitude);
    }

    /** No-arg constructor required by JPA. */
    protected AirportCoordinates() {
    }
}
