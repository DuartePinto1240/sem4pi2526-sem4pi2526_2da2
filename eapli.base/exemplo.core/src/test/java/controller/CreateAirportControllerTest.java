package controller;

import domain.AirControlArea;
import domain.Airport;
import domain.Country;
import domain.GeographicBoundary;
import domain.ICAOCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import persistence.impl.inmemory.InMemoryAirportRepository;
import persistence.impl.inmemory.InMemoryCountryRepository;
import repositories.AirControlAreaRepository;
import repositories.AirportRepository;
import repositories.CountryRepository;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CreateAirportController} (US052).
 */
public class CreateAirportControllerTest {

    private CreateAirportController controller;
    private AirportRepository airportRepo;
    private AirControlAreaRepository areaRepo;
    private CountryRepository countryRepo;

    private static final String AREA_CODE = "LPPC";
    private static final String COUNTRY_NAME = "Portugal";

    @BeforeEach
    public void setUp() {
        airportRepo = new InMemoryAirportRepository();
        areaRepo = new InMemoryAirControlAreaRepository();
        countryRepo = new InMemoryCountryRepository();

        areaRepo.save(new AirControlArea(AREA_CODE, "Lisbon ACC",
                new GeographicBoundary(36.0, 42.2, -10.0, -6.0)));
        countryRepo.save(new Country(COUNTRY_NAME));

        controller = new CreateAirportController(airportRepo, areaRepo, countryRepo);
    }

    @Test
    public void ensureAllAreasReturnsRegisteredAreas() {
        final Iterator<AirControlArea> areas = controller.allAreas().iterator();

        assertTrue(areas.hasNext());
        assertEquals(AREA_CODE, areas.next().identity().code());
    }

    @Test
    public void ensureAllCountriesReturnsRegisteredCountries() {
        final Iterator<Country> countries = controller.allCountries().iterator();

        assertTrue(countries.hasNext());
        assertEquals(COUNTRY_NAME, countries.next().name());
    }

    @Test
    public void ensureValidAirportIsRegisteredAndReturned() {
        final Airport airport = createLisbonAirport();

        assertNotNull(airport);
        assertEquals("LPPT", airport.identity().code());
        assertEquals("LIS", airport.iataCode().code());
        assertEquals("Humberto Delgado Airport", airport.name().toString());
        assertEquals("Lisbon", airport.town());
        assertEquals(COUNTRY_NAME, airport.country().name());
        assertEquals(38.7756, airport.coordinates().latitude());
        assertEquals(-9.1354, airport.coordinates().longitude());
        assertEquals(113.0, airport.elevation().metres());
        assertEquals(AREA_CODE, airport.airControlAreaCode().code());
    }

    @Test
    public void ensureRegisteredAirportIsPersisted() {
        createLisbonAirport();

        assertTrue(airportRepo.containsOfIdentity(new ICAOCode("LPPT")));
    }

    @Test
    public void ensureNegativeElevationIsAccepted() {
        areaRepo.save(new AirControlArea("EHAA", "Amsterdam FIR",
                new GeographicBoundary(50.0, 56.0, 2.0, 8.0)));
        countryRepo.save(new Country("Netherlands"));

        final Airport airport = controller.createAirport(
                "EHAM", "AMS", "Amsterdam Schiphol",
                "Amsterdam", "Netherlands",
                52.3086, 4.7639, -3.35, "EHAA");

        assertEquals(-3.35, airport.elevation().metres());
    }

    @Test
    public void ensureDuplicateICAOCodeIsRejected() {
        createLisbonAirport();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "OPO", "Some Airport",
                        "Porto", COUNTRY_NAME, 41.2, -8.6, 69.0, AREA_CODE));

        assertTrue(ex.getMessage().contains("ICAO"));
    }

    @Test
    public void ensureDuplicateIATACodeIsRejected() {
        createLisbonAirport();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPR", "LIS", "Porto Airport",
                        "Porto", COUNTRY_NAME, 41.2, -8.6, 69.0, AREA_CODE));

        assertTrue(ex.getMessage().contains("IATA"));
    }

    @Test
    public void ensureNonExistentAreaCodeIsRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "Lisbon", COUNTRY_NAME, 38.7756, -9.1354, 113.0, "XXXX"));

        assertTrue(ex.getMessage().contains("XXXX"));
    }

    @Test
    public void ensureCoordinatesOutsideAreaBoundaryAreRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "Lisbon", COUNTRY_NAME, 48.8566, 2.3522, 113.0, AREA_CODE));

        assertTrue(ex.getMessage().contains("outside"));
        assertTrue(ex.getMessage().contains(AREA_CODE));
    }

    @Test
    public void ensureCoordinatesOnBoundaryEdgeAreAccepted() {
        assertDoesNotThrow(() ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "Lisbon", COUNTRY_NAME, 36.0, -10.0, 10.0, AREA_CODE));
    }

    @Test
    public void ensureUnknownCountryIsRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "Lisbon", "Narnia", 38.7756, -9.1354, 113.0, AREA_CODE));

        assertTrue(ex.getMessage().contains("Narnia"));
    }

    @Test
    public void ensureICAOCodeWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPP", "LIS", "Airport",
                        "Lisbon", COUNTRY_NAME, 38.7756, -9.1354, 113.0, AREA_CODE));
    }

    @Test
    public void ensureIATACodeWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LISB", "Airport",
                        "Lisbon", COUNTRY_NAME, 38.7756, -9.1354, 113.0, AREA_CODE));
    }

    @Test
    public void ensureInvalidLatitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "Lisbon", COUNTRY_NAME, 95.0, -9.1354, 113.0, AREA_CODE));
    }

    @Test
    public void ensureInvalidLongitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "Lisbon", COUNTRY_NAME, 38.7756, -190.0, 113.0, AREA_CODE));
    }

    @Test
    public void ensureBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "   ",
                        "Lisbon", COUNTRY_NAME, 38.7756, -9.1354, 113.0, AREA_CODE));
    }

    @Test
    public void ensureBlankTownIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAirport("LPPT", "LIS", "Airport",
                        "  ", COUNTRY_NAME, 38.7756, -9.1354, 113.0, AREA_CODE));
    }

    private Airport createLisbonAirport() {
        return controller.createAirport(
                "LPPT", "LIS", "Humberto Delgado Airport",
                "Lisbon", COUNTRY_NAME,
                38.7756, -9.1354, 113.0, AREA_CODE);
    }
}
