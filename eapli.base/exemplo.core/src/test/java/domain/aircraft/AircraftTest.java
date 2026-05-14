package domain.aircraft;

import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AircraftTest {

    private AircraftModel model;

    @BeforeEach
    void setUp() {
        final EngineModel engine = new EngineModel(
                "GE90", "GE", MotorizationType.TURBOFAN,
                new Thrust(300000, 400000),
                new TSFC(1.6e-4));

        final FlightCharacteristics chars = new FlightCharacteristics(
                150000, 250000, 300000, 100000,
                13000, 250, 12000, 440, 0.02, 0.5);

        model = new AircraftModel(
                "777-200ER", "Boeing", AircraftType.PASSENGER,
                chars, List.of(engine));
    }

    @Test
    void ensureValidAircraftIsCreated() {
        final CabinConfiguration cabin =
                new CabinConfiguration(200, 20, 0, 8);
        final Aircraft aircraft =
                new Aircraft("CS-TUA", "Portugal", "TP", model, cabin);

        assertNotNull(aircraft);
        assertEquals("CS-TUA", aircraft.registration());
        assertEquals("Portugal", aircraft.countryOfRegistration());
        assertEquals("TP", aircraft.companyIata());
        assertTrue(aircraft.operationalStatus().isActive());
    }

    @Test
    void ensureRegistrationIsStoredUppercase() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("cs-tua", "Portugal", "TP", model, cabin);

        assertEquals("CS-TUA", aircraft.registration());
    }
    @Test
    void ensureRegistrationDateIsSetToToday() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUH", "Portugal", "TP", model, cabin);

        assertEquals(LocalDate.now(), aircraft.registrationDate());
    }

    @Test
    void ensureAgeInYearsIsZeroForNewAircraft() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUI", "Portugal", "TP", model, cabin);

        assertEquals(0, aircraft.ageInYears());
    }

    @Test
    void ensureAircraftModelIdIsSet() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUJ", "Portugal", "TP", model, cabin);

        assertEquals(model.identity(), aircraft.aircraftModelId());
    }

    @Test
    void ensureToStringContainsRegistration() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUK", "Portugal", "TP", model, cabin);

        assertTrue(aircraft.toString().contains("CS-TUK"));
    }

    @Test
    void ensureAircraftIsActiveUponCreation() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUB", "Portugal", "TP", model, cabin);

        assertTrue(aircraft.operationalStatus().isActive());
        assertFalse(aircraft.operationalStatus().isDecommissioned());
    }

    @Test
    void ensureEmptyRegistrationIsRejected() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("", "Portugal", "TP", model, cabin));
    }

    @Test
    void ensureEmptyCountryIsRejected() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "", "TP", model, cabin));
    }

    @Test
    void ensureEmptyCompanyIataIsRejected() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", "", model, cabin));
    }

    @Test
    void ensureNullModelIsRejected() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", "TP", null, cabin));
    }

    @Test
    void ensureNullCabinConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", "TP", model, null));
    }

    @Test
    void ensureDecommissionMakesAircraftInactive() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUC", "Portugal", "TP", model, cabin);

        aircraft.decommission();

        assertFalse(aircraft.operationalStatus().isActive());
        assertTrue(aircraft.operationalStatus().isDecommissioned());
    }

    @Test
    void ensureDecommissioningAlreadyDecommissionedAircraftThrows() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUD", "Portugal", "TP", model, cabin);

        aircraft.decommission();

        assertThrows(IllegalArgumentException.class, aircraft::decommission);
    }

    @Test
    void ensureIdentityIsRegistration() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft aircraft =
                new Aircraft("CS-TUE", "Portugal", "TP", model, cabin);

        assertEquals("CS-TUE", aircraft.identity());
    }

    @Test
    void ensureSameAsReturnsTrueForSameRegistration() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft a1 =
                new Aircraft("CS-TUF", "Portugal", "TP", model, cabin);
        final Aircraft a2 =
                new Aircraft("CS-TUF", "Spain", "FR", model, cabin);

        assertTrue(a1.sameAs(a2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentRegistration() {
        final CabinConfiguration cabin =
                new CabinConfiguration(100, 0, 0, 4);
        final Aircraft a1 =
                new Aircraft("CS-TUG", "Portugal", "TP", model, cabin);
        final Aircraft a2 =
                new Aircraft("EI-ABC", "Ireland", "FR", model, cabin);

        assertFalse(a1.sameAs(a2));
    }
    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<Aircraft> ctor =
                Aircraft.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final Aircraft instance = ctor.newInstance();
        assertNotNull(instance);
    }
}