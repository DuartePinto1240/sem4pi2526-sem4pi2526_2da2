package domain.aircraft;

import domain.AirTransportCompany;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class CabinConfigurationTest {

    @Test
    void ensureValidCabinConfigurationIsCreated() {
        final CabinConfiguration cabin =
                new CabinConfiguration(200, 20, 8, 6);

        assertEquals(200, cabin.economySeats());
        assertEquals(20,  cabin.businessSeats());
        assertEquals(8,   cabin.firstClassSeats());
        assertEquals(6,   cabin.crewSize());
        assertEquals(228, cabin.totalSeats());
    }

    @Test
    void ensureTotalSeatsIsCorrect() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 50, 10, 4);

        assertEquals(160, cabin.totalSeats());
    }

    @Test
    void ensureEconomyOnlyConfigurationIsAccepted() {
        assertDoesNotThrow(() -> new CabinConfiguration(200, 0, 0, 4));
    }

    @Test
    void ensureNegativeEconomySeatsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(-1, 0, 0, 4));
    }

    @Test
    void ensureNegativeBusinessSeatsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(0, -1, 0, 4));
    }

    @Test
    void ensureNegativeFirstClassSeatsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(0, 0, -1, 4));
    }

    @Test
    void ensureZeroCrewSizeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(100, 0, 0, 0));
    }

    @Test
    void ensureAllZeroSeatsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(0, 0, 0, 4));
    }

    @Test
    void ensureTwoConfigurationsWithSameValuesAreEqual() {
        final CabinConfiguration c1 = new CabinConfiguration(200, 20, 8, 6);
        final CabinConfiguration c2 = new CabinConfiguration(200, 20, 8, 6);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void ensureTwoConfigurationsWithDifferentValuesAreNotEqual() {
        final CabinConfiguration c1 = new CabinConfiguration(200, 20, 8, 6);
        final CabinConfiguration c2 = new CabinConfiguration(150, 20, 8, 6);
        assertNotEquals(c1, c2);
    }
    @Test
    void ensureToStringContainsAllValues() {
        final CabinConfiguration cabin = new CabinConfiguration(200, 20, 8, 6);
        final String result = cabin.toString();
        assertTrue(result.contains("200"));
        assertTrue(result.contains("20"));
        assertTrue(result.contains("8"));
        assertTrue(result.contains("6"));
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<CabinConfiguration> ctor =
                CabinConfiguration.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final CabinConfiguration instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
