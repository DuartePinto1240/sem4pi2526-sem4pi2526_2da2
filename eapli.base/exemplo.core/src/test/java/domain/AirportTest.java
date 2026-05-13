package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Airport} aggregate root (US052).
 *
 * <p>Follows the AAA (Arrange-Act-Assert) convention as required by NFR03.</p>
 */
public class AirportTest {

    private static final ICAOCode           VALID_ICAO    = new ICAOCode("LPPT");
    private static final IATACode           VALID_IATA    = new IATACode("LIS");
    private static final AirportName        VALID_NAME    = new AirportName("Humberto Delgado Airport");
    private static final String             VALID_TOWN    = "Lisbon";
    private static final Country            VALID_COUNTRY = new Country("Portugal");
    private static final AirportCoordinates VALID_COORDS  = new AirportCoordinates(38.7756, -9.1354);
    private static final Elevation          VALID_ELEV    = new Elevation(113.0);
    private static final AirControlAreaCode VALID_AREA    = new AirControlAreaCode("LPPC");

    private Airport validAirport() {
        return new Airport(VALID_ICAO, VALID_IATA, VALID_NAME,
                VALID_TOWN, VALID_COUNTRY, VALID_COORDS, VALID_ELEV, VALID_AREA);
    }

    // Valid construction

    @Test
    public void ensureValidAirportIsCreatedWithCorrectICAO() {
        assertEquals("LPPT", validAirport().identity().code());
    }

    @Test
    public void ensureValidAirportIsCreatedWithCorrectIATA() {
        assertEquals("LIS", validAirport().iataCode().code());
    }

    @Test
    public void ensureValidAirportIsCreatedWithCorrectName() {
        assertEquals("Humberto Delgado Airport", validAirport().name().value());
    }

    @Test
    public void ensureValidAirportIsCreatedWithCorrectTown() {
        assertEquals("Lisbon", validAirport().town());
    }

    @Test
    public void ensureValidAirportIsCreatedWithCorrectCountry() {
        assertEquals("Portugal", validAirport().country().name());
    }

    @Test
    public void ensureValidAirportIsCreatedWithCorrectElevation() {
        assertEquals(113.0, validAirport().elevation().metres());
    }

    @Test
    public void ensureValidAirportIsCreatedWithCorrectAreaCode() {
        assertEquals("LPPC", validAirport().airControlAreaCode().code());
    }

    // Negative elevation is allowed

    @Test
    public void ensureNegativeElevationIsAllowed() {
        final Airport airport = new Airport(new ICAOCode("EHAM"), new IATACode("AMS"),
                new AirportName("Amsterdam Schiphol"), "Amsterdam", new Country("Netherlands"),
                new AirportCoordinates(52.3086, 4.7639), new Elevation(-3.35), VALID_AREA);

        assertEquals(-3.35, airport.elevation().metres());
    }

    // Null / blank inputs rejected

    @Test
    public void ensureNullICAOIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(null, VALID_IATA, VALID_NAME, VALID_TOWN,
                        VALID_COUNTRY, VALID_COORDS, VALID_ELEV, VALID_AREA));
    }

    @Test
    public void ensureNullIATAIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(VALID_ICAO, null, VALID_NAME, VALID_TOWN,
                        VALID_COUNTRY, VALID_COORDS, VALID_ELEV, VALID_AREA));
    }

    @Test
    public void ensureNullCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(VALID_ICAO, VALID_IATA, VALID_NAME, VALID_TOWN,
                        null, VALID_COORDS, VALID_ELEV, VALID_AREA));
    }

    @Test
    public void ensureBlankTownIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(VALID_ICAO, VALID_IATA, VALID_NAME, "   ",
                        VALID_COUNTRY, VALID_COORDS, VALID_ELEV, VALID_AREA));
    }

    @Test
    public void ensureNullCoordinatesIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(VALID_ICAO, VALID_IATA, VALID_NAME, VALID_TOWN,
                        VALID_COUNTRY, null, VALID_ELEV, VALID_AREA));
    }

    @Test
    public void ensureNullElevationIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(VALID_ICAO, VALID_IATA, VALID_NAME, VALID_TOWN,
                        VALID_COUNTRY, VALID_COORDS, null, VALID_AREA));
    }

    // Identity and sameAs

    @Test
    public void ensureTwoAirportsWithSameICAOAreSame() {
        final Airport a1 = validAirport();
        final Airport a2 = new Airport(VALID_ICAO, new IATACode("XXX"),
                new AirportName("Other"), "Other", VALID_COUNTRY,
                VALID_COORDS, VALID_ELEV, VALID_AREA);
        assertTrue(a1.sameAs(a2));
    }

    @Test
    public void ensureTwoAirportsWithDifferentICAOAreNotSame() {
        final Airport a1 = validAirport();
        final Airport a2 = new Airport(new ICAOCode("LPPR"), new IATACode("OPO"),
                new AirportName("Porto Airport"), "Porto", VALID_COUNTRY,
                new AirportCoordinates(41.2481, -8.6814), new Elevation(69.0), VALID_AREA);
        assertFalse(a1.sameAs(a2));
    }

    @Test
    public void ensureSameAsReturnsFalseForNull() {
        assertFalse(validAirport().sameAs(null));
    }

    @Test
    public void ensureToStringContainsICAOAndIATA() {
        final String result = validAirport().toString();
        assertTrue(result.contains("LPPT"));
        assertTrue(result.contains("LIS"));
    }
}
