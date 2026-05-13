package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.Airport;
import domain.AirportCoordinates;
import domain.AirportName;
import domain.Country;
import domain.Elevation;
import domain.IATACode;
import domain.ICAOCode;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;
import repositories.AirportRepository;
import repositories.CountryRepository;

/**
 * Application-layer controller for US052 — Create an Airport.
 *
 * <p>Business rules enforced:</p>
 * <ul>
 *   <li>ICAO code must be unique system-wide.</li>
 *   <li>IATA code must be unique system-wide.</li>
 *   <li>The referenced air control area must exist.</li>
 *   <li>Airport coordinates must lie within the bounding box of the selected area.</li>
 *   <li>Country must exist in the bootstrap-loaded country list.</li>
 *   <li>All domain-level validations (code formats, coordinate ranges, etc.)
 *       are enforced by the Value Objects themselves.</li>
 * </ul>
 */
public class CreateAirportController {

    private final AirportRepository        airportRepo;
    private final AirControlAreaRepository areaRepo;
    private final CountryRepository        countryRepo;

    /**
     * Creates the controller, obtaining repositories from {@link PersistenceContext}.
     */
    public CreateAirportController() {
        this.airportRepo = PersistenceContext.getInstance().airportRepository();
        this.areaRepo    = PersistenceContext.getInstance().airControlAreaRepository();
        this.countryRepo = PersistenceContext.getInstance().countryRepository();
    }

    /**
     * Package-private constructor for testing — allows injection of repositories.
     */
    CreateAirportController(final AirportRepository airportRepo,
                            final AirControlAreaRepository areaRepo,
                            final CountryRepository countryRepo) {
        this.airportRepo = airportRepo;
        this.areaRepo    = areaRepo;
        this.countryRepo = countryRepo;
    }

    /**
     * Returns all registered air control areas for display in the UI.
     *
     * @return iterable of all existing {@link AirControlArea} records
     */
    public Iterable<AirControlArea> allAreas() {
        return areaRepo.findAll();
    }

    /**
     * Returns all available countries for display in the UI selection list.
     *
     * @return iterable of all bootstrap-loaded {@link Country} records
     */
    public Iterable<Country> allCountries() {
        return countryRepo.findAll();
    }

    /**
     * Registers a new airport.
     *
     * @param icao        4-letter ICAO code (e.g. "LPPT")
     * @param iata        3-letter IATA code (e.g. "LIS")
     * @param name        full name of the airport
     * @param town        city/town (free text, entered by operator)
     * @param countryName country name (must exist in bootstrap list)
     * @param latitude    geographic latitude  [-90, 90]
     * @param longitude   geographic longitude [-180, 180]
     * @param elevationM  elevation in metres (may be negative)
     * @param areaCode    code of an existing {@link AirControlArea}
     * @return the persisted {@link Airport}
     * @throws IllegalArgumentException if any business rule or domain rule is violated
     */
    public Airport createAirport(final String icao,
                                 final String iata,
                                 final String name,
                                 final String town,
                                 final String countryName,
                                 final double latitude,
                                 final double longitude,
                                 final double elevationM,
                                 final String areaCode) {

        //Build Value Objects — format/range validation inside each VO
        final ICAOCode           icaoCode    = new ICAOCode(icao);
        final IATACode           iataCode    = new IATACode(iata);
        final AirportName        airportName = new AirportName(name);
        final AirportCoordinates coords      = new AirportCoordinates(latitude, longitude);
        final Elevation          elevation   = new Elevation(elevationM);
        final AirControlAreaCode areaRef     = new AirControlAreaCode(areaCode);

        // Country must exist in the bootstrap list
        final Country country = countryRepo.findByName(countryName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country '" + countryName + "' is not in the system. "
                                + "Please select a country from the available list."));

        // ICAO uniqueness
        if (airportRepo.containsOfIdentity(icaoCode)) {
            throw new IllegalArgumentException(
                    "An airport with ICAO code '" + icao + "' already exists.");
        }

        // IATA uniqueness
        if (airportRepo.existsByIATACode(iataCode)) {
            throw new IllegalArgumentException(
                    "An airport with IATA code '" + iata + "' already exists.");
        }

        // Air control area must exist
        final AirControlArea area = areaRepo.ofIdentity(areaRef)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No air control area found with code '" + areaCode + "'."));

        // Coordinates must be within the area boundary — hard rejection
        if (!coords.isWithinBoundary(area.boundary())) {
            throw new IllegalArgumentException(String.format(
                    "Coordinates (%.2f, %.2f) are outside the boundary of area '%s' "
                            + "[Lat %.2f–%.2f, Lon %.2f–%.2f]. "
                            + "An airport must be located within its air control area.",
                    latitude, longitude, areaCode,
                    area.boundary().minLat(), area.boundary().maxLat(),
                    area.boundary().minLon(), area.boundary().maxLon()));
        }

        // Create and persist
        final Airport airport = new Airport(
                icaoCode, iataCode, airportName,
                town, country,
                coords, elevation, areaRef);

        return airportRepo.save(airport);
    }
}