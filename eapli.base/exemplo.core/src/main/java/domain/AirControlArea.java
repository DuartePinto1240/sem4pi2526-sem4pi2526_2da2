package domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Aggregate Root representing an Air Control Area in the AISafe system (US050).
 *
 * <p>An air control area is a defined region of airspace managed by a flight
 * control entity. It serves as the organisational unit to which airports,
 * weather data, and flight simulations are scoped.</p>
 *
 * <p>Domain invariants enforced by this class:</p>
 * <ul>
 *   <li>Area code must not be blank and is unique system-wide.</li>
 *   <li>Name must not be blank.</li>
 *   <li>Geographic boundary must define a valid bounding box.</li>
 * </ul>
 */
@Entity
@Table(name = "AIR_CONTROL_AREA")
public class AirControlArea implements AggregateRoot<AirControlAreaCode> {

    /** Aggregate identity — unique code for this area, e.g. "LPPC". */
    @EmbeddedId
    private AirControlAreaCode areaCode;

    /** Human-readable name of the air control area. */
    @Column(nullable = false)
    private String name;

    /** Geographic bounding box defining the spatial extent of this area. */
    @Embedded
    private GeographicBoundary boundary;

    /**
     * Creates a new Air Control Area.
     *
     * @param areaCode unique code for this area, must not be blank
     * @param name     human-readable name, must not be blank
     * @param boundary geographic bounding box, must be valid
     * @throws IllegalArgumentException if any argument fails validation
     */
    public AirControlArea(final String areaCode,
                          final String name,
                          final GeographicBoundary boundary) {
        Preconditions.nonEmpty(areaCode, "Area code must not be empty");
        Preconditions.nonEmpty(name, "Area name must not be empty");
        Preconditions.nonNull(boundary, "Geographic boundary must not be null");

        this.areaCode = new AirControlAreaCode(areaCode);
        this.name = name.trim();
        this.boundary = boundary;
    }

    /** {@inheritDoc} */
    @Override
    public AirControlAreaCode identity() {
        return areaCode;
    }

    /** {@inheritDoc} */
    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AirControlArea)) return false;
        final AirControlArea that = (AirControlArea) other;
        return this.areaCode.equals(that.areaCode);
    }

    /** @return the human-readable name of this area */
    public String name() {
        return name;
    }

    /** @return the geographic bounding box of this area */
    public GeographicBoundary boundary() {
        return boundary;
    }

    @Override
    public String toString() {
        return String.format("%s — %s | %s", areaCode, name, boundary);
    }

    /** No-arg constructor required by JPA. */
    protected AirControlArea() {
    }
}
