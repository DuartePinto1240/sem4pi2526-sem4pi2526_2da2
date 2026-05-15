package domain;

import domain.GeographicBoundary;
import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing the rectangular sub-area within an Air Control Area
 * to which a weather observation applies.
 *
 * <p>The {@link GeographicBoundary} class is reused to represent the bounding
 * box of the sub-area, ensuring coordinate validation is consistent across
 * the project.</p>
 */
@Embeddable
public class WeatherSubArea implements ValueObject {

    @Embedded
    private GeographicBoundary subBoundary;

    @Embedded
    private AltitudeRange altitudeRange;

    /**
     * Creates a new WeatherSubArea.
     *
     * @param subBoundary the rectangular bounding box of this sub-area; must not be null
     * @param altitudeRange the altitude range; must not be null
     * @throws IllegalArgumentException if subBoundary is null
     */
    public WeatherSubArea(final GeographicBoundary subBoundary, final AltitudeRange altitudeRange) {
        Preconditions.nonNull(subBoundary, "Sub-area boundary must not be null");
        Preconditions.nonNull(altitudeRange,  "Altitude range must not be null");
        this.subBoundary = subBoundary;
        this.altitudeRange = altitudeRange;
    }

    /** @return the geographic bounding box of this sub-area */
    public GeographicBoundary subBoundary() {
        return subBoundary;
    }

    /** @return the altitude range of this sub-area */
    public AltitudeRange altitudeRange() { return altitudeRange; }

    /**
     * Returns {@code true} if this sub-area lies entirely inside the given parent boundary.
     *
     * <p>Used by the controller to validate that the sub-area the user entered
     * is contained within the selected air control area.</p>
     *
     * @param parentBoundary the parent area's boundary
     * @return {@code true} if all four corners of this sub-area are inside the parent
     */
    public boolean isInsideBoundary(final GeographicBoundary parentBoundary) {
        if (parentBoundary == null) return false;
        return subBoundary.minLat() >= parentBoundary.minLat()
                && subBoundary.maxLat() <= parentBoundary.maxLat()
                && subBoundary.minLon() >= parentBoundary.minLon()
                && subBoundary.maxLon() <= parentBoundary.maxLon();
    }

    /**
     * Returns {@code true} if this sub-area overlaps with {@code other}.
     *
     * <p>Delegates to {@link GeographicBoundary#overlapsWith(GeographicBoundary)}.</p>
     *
     * @param other the other sub-area to test
     * @return {@code true} if the two sub-areas share any geographic region
     */
    public boolean overlapsWith(final WeatherSubArea other) {
        if (other == null) return false;
        return this.subBoundary.overlapsWith(other.subBoundary)
                && this.altitudeRange.overlapsWith(other.altitudeRange);
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof WeatherSubArea)) return false;
        final WeatherSubArea that = (WeatherSubArea) o;
        return Objects.equals(subBoundary, that.subBoundary)
                && Objects.equals(altitudeRange, that.altitudeRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subBoundary, altitudeRange);
    }

    @Override
    public String toString() {
        return "SubArea[" + subBoundary + ", " + altitudeRange + "]";
    }

    /** No-arg constructor required by JPA. */
    protected WeatherSubArea() {
    }
}
