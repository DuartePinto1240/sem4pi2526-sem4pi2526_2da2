package domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Aggregate Root representing an Airport in the AISafe system (US052).
 *
 * <p>An airport is a physical location where aircraft take off and land.
 * It is identified by an internationally standardised ICAO code (4 letters),
 * carries a secondary IATA code (3 letters), and belongs to exactly one
 * {@link AirControlArea}.</p>
 *
 * <p>Domain invariants enforced by this class:</p>
 * <ul>
 *   <li>ICAO code must be exactly 4 uppercase letters — used as aggregate identity.</li>
 *   <li>IATA code must be exactly 3 uppercase letters — unique system-wide.</li>
 *   <li>Name must not be blank.</li>
 *   <li>Town must not be blank (free text, entered by operator).</li>
 *   <li>Country must not be null (selected from bootstrap list).</li>
 *   <li>Coordinates must be within valid geographic ranges AND within the
 *       bounding box of the associated air control area.</li>
 *   <li>Elevation is in metres above sea level; negative values are allowed
 *       (some airports exist below sea level).</li>
 *   <li>Air control area reference must not be null.</li>
 * </ul>
 */
@Entity
@Table(name = "AIRPORT")
public class Airport implements AggregateRoot<ICAOCode> {

    /** Aggregate identity — unique ICAO code, e.g. "LPPT". */
    @EmbeddedId
    private ICAOCode icaoCode;

    /** Secondary unique identifier — 3-letter IATA code, e.g. "LIS". */
    @Embedded
    private IATACode iataCode;

    /** Full name of the airport. */
    @Embedded
    private AirportName name;

    /**
     * City or town where the airport is located.
     * Entered as free text by the operator (professor confirmed this approach).
     */
    @Column(name = "town", nullable = false)
    private String town;

    /**
     * Country where the airport is located.
     * Selected from the bootstrap-loaded country list.
     */
    @Embedded
    private Country country;

    /** Geographic location of the airport (latitude and longitude). */
    @Embedded
    private AirportCoordinates coordinates;

    /**
     * Height in metres above (or below) sea level.
     * The specification states "elevation in meters above sea level" as the
     * unit of reference — negative values are valid for airports below sea level.
     */
    @Embedded
    private Elevation elevation;

    /**
     * Reference to the air control area this airport belongs to.
     * Stored by identity (code) — not a direct object reference —
     * to preserve aggregate boundary independence.
     */
    @Embedded
    private AirControlAreaCode airControlAreaCode;

    /**
     * Creates a new Airport with all mandatory attributes.
     *
     * @param icaoCode           unique 4-letter ICAO code
     * @param iataCode           unique 3-letter IATA code
     * @param name               full name of the airport
     * @param town               city/town (free text)
     * @param country            country (from bootstrap list)
     * @param coordinates        geographic location
     * @param elevation          height in metres (may be negative)
     * @param airControlAreaCode reference to the controlling area
     * @throws IllegalArgumentException if any argument fails validation
     */
    public Airport(final ICAOCode icaoCode,
                   final IATACode iataCode,
                   final AirportName name,
                   final String town,
                   final Country country,
                   final AirportCoordinates coordinates,
                   final Elevation elevation,
                   final AirControlAreaCode airControlAreaCode) {

        Preconditions.nonNull(icaoCode,           "ICAO code must not be null");
        Preconditions.nonNull(iataCode,           "IATA code must not be null");
        Preconditions.nonNull(name,               "Airport name must not be null");
        Preconditions.nonEmpty(town,              "Town must not be blank");
        Preconditions.nonNull(country,            "Country must not be null");
        Preconditions.nonNull(coordinates,        "Coordinates must not be null");
        Preconditions.nonNull(elevation,          "Elevation must not be null");
        Preconditions.nonNull(airControlAreaCode, "Air control area code must not be null");

        this.icaoCode           = icaoCode;
        this.iataCode           = iataCode;
        this.name               = name;
        this.town               = town.trim();
        this.country            = country;
        this.coordinates        = coordinates;
        this.elevation          = elevation;
        this.airControlAreaCode = airControlAreaCode;
    }

    /** {@inheritDoc} */
    @Override
    public ICAOCode identity() {
        return icaoCode;
    }

    /** {@inheritDoc} */
    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof Airport)) return false;
        return this.icaoCode.equals(((Airport) other).icaoCode);
    }

    /** @return the IATA code */
    public IATACode iataCode() { return iataCode; }

    /** @return the full name */
    public AirportName name() { return name; }

    /** @return the town/city (free text) */
    public String town() { return town; }

    /** @return the country */
    public Country country() { return country; }

    /** @return the geographic coordinates */
    public AirportCoordinates coordinates() { return coordinates; }

    /** @return the elevation in metres */
    public Elevation elevation() { return elevation; }

    /** @return the air control area code reference */
    public AirControlAreaCode airControlAreaCode() { return airControlAreaCode; }

    @Override
    public String toString() {
        return String.format("%s (%s) — %s, %s | %s | elev: %s | area: %s",
                icaoCode, iataCode, town, country, name, elevation, airControlAreaCode);
    }

    /** No-arg constructor required by JPA. */
    protected Airport() {
    }
}
