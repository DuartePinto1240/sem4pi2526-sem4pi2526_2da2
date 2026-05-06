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
 * <p>Following the professor's clarification, weather data applies to rectangular
 * sub-areas of an air control area (not to the whole area). Each sub-area is
 * defined by a geographic bounding box that <strong>must fit inside</strong>
 * the parent air control area's boundary.</p>
 *
 * <p>The {@link GeographicBoundary} class is reused to represent the bounding
 * box of the sub-area, ensuring coordinate validation is consistent across
 * the project.</p>
 *
 * <p><b>Simplification note (MVP):</b> altitude layering (the "paralelepípedo"
 * the professor described) is not modelled in this sprint. Weather data is
 * treated as applying uniformly throughout the vertical extent of the sub-area.
 * This simplification is safe for MVP purposes and can be extended by adding
 * {@code minAltitude}/{@code maxAltitude} fields to this class in a future sprint.</p>
 */
@Embeddable
public class WeatherSubArea implements ValueObject {

    @Embedded
    private GeographicBoundary subBoundary;

    /**
     * Creates a new WeatherSubArea.
     *
     * @param subBoundary the rectangular bounding box of this sub-area; must not be null
     * @throws IllegalArgumentException if subBoundary is null
     */
    public WeatherSubArea(final GeographicBoundary subBoundary) {
        Preconditions.nonNull(subBoundary, "Sub-area boundary must not be null");
        this.subBoundary = subBoundary;
    }

    /** @return the geographic bounding box of this sub-area */
    public GeographicBoundary subBoundary() {
        return subBoundary;
    }

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
        return this.subBoundary.overlapsWith(other.subBoundary);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WeatherSubArea)) return false;
        return Objects.equals(subBoundary, ((WeatherSubArea) o).subBoundary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subBoundary);
    }

    @Override
    public String toString() {
        return "SubArea[" + subBoundary + "]";
    }

    /** No-arg constructor required by JPA. */
    protected WeatherSubArea() {
    }
}
