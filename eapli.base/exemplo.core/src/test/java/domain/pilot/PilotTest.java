package domain.pilot;

import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PilotTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private AirTransportCompany validCompany() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    private AircraftModel validAircraftModel() {
        final EngineModel engine = new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));

        final FlightCharacteristics characteristics = new FlightCharacteristics(
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45);

        return new AircraftModel("737 MAX", "Boeing",
                AircraftType.PASSENGER, characteristics, List.of(engine));
    }

    private Pilot validPilot() {
        return new Pilot(
                "João Silva",
                "joao.silva@tap.pt",
                "+351910000001",
                validCompany(),
                List.of(validAircraftModel()));
    }

    // ── name ──────────────────────────────────────────────────────────────────

    @Test
    void ensureNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot(null, "joao@tap.pt", "+351910000001",
                        validCompany(), List.of(validAircraftModel())));
    }

    @Test
    void ensureNameCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("", "joao@tap.pt", "+351910000001",
                        validCompany(), List.of(validAircraftModel())));
    }

    // ── email ─────────────────────────────────────────────────────────────────

    @Test
    void ensureEmailCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", null, "+351910000001",
                        validCompany(), List.of(validAircraftModel())));
    }

    @Test
    void ensureEmailCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", "", "+351910000001",
                        validCompany(), List.of(validAircraftModel())));
    }

    // ── phoneNumber ───────────────────────────────────────────────────────────

    @Test
    void ensurePhoneNumberCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", "joao@tap.pt", null,
                        validCompany(), List.of(validAircraftModel())));
    }

    @Test
    void ensurePhoneNumberCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", "joao@tap.pt", "",
                        validCompany(), List.of(validAircraftModel())));
    }

    // ── company ───────────────────────────────────────────────────────────────

    @Test
    void ensureCompanyCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", "joao@tap.pt", "+351910000001",
                        null, List.of(validAircraftModel())));
    }

    // ── certifiedModels ───────────────────────────────────────────────────────

    @Test
    void ensureCertifiedModelsCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", "joao@tap.pt", "+351910000001",
                        validCompany(), null));
    }

    @Test
    void ensureCertifiedModelsCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot("João Silva", "joao@tap.pt", "+351910000001",
                        validCompany(), Collections.emptyList()));
    }

    // ── valid creation ────────────────────────────────────────────────────────

    @Test
    void ensureValidPilotIsCreatedSuccessfully() {
        // Arrange + Act
        final Pilot pilot = validPilot();
        // Assert
        assertEquals("João Silva",       pilot.name());
        assertEquals("joao.silva@tap.pt", pilot.email());
        assertEquals("+351910000001",    pilot.phoneNumber());
        assertNotNull(pilot.company());
        assertEquals(1, pilot.certifiedModels().size());
    }

    @Test
    void ensurePilotIsActiveOnCreation() {
        // Arrange + Act
        final Pilot pilot = validPilot();
        // Assert
        assertTrue(pilot.isActive());
    }

    @Test
    void ensureEmailIsNormalizedToLowerCase() {
        // Arrange + Act
        final Pilot pilot = new Pilot("João Silva", "Joao.Silva@TAP.PT", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        // Assert
        assertEquals("joao.silva@tap.pt", pilot.email());
    }

    @Test
    void ensurePilotCanBeCertifiedInMultipleModels() {
        // Arrange
        final AircraftModel model1 = validAircraftModel();
        final AircraftModel model2 = new AircraftModel("A320", "Airbus",
                AircraftType.PASSENGER,
                new FlightCharacteristics(42400.0, 64300.0, 78000.0, 26730.0,
                        12131.0, 228.0, 6150.0, 122.6, 0.0260, 1.52),
                List.of(new EngineModel("CFM56-5B", "CFM International",
                        MotorizationType.TURBOFAN,
                        new Thrust(120.0, 138.0), new TSFC(0.0160))));
        // Act
        final Pilot pilot = new Pilot("João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(model1, model2));
        // Assert
        assertEquals(2, pilot.certifiedModels().size());
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    void ensurePilotCanBeDeactivated() {
        // Arrange
        final Pilot pilot = validPilot();
        // Act
        pilot.deactivate();
        // Assert
        assertFalse(pilot.isActive());
    }

    @Test
    void ensureDeactivateIsIdempotent() {
        // Arrange
        final Pilot pilot = validPilot();
        // Act
        pilot.deactivate();
        pilot.deactivate();
        // Assert
        assertFalse(pilot.isActive());
    }

    // ── certifiedModels immutability ──────────────────────────────────────────

    @Test
    void ensureCertifiedModelsListIsUnmodifiable() {
        // Arrange
        final Pilot pilot = validPilot();
        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> pilot.certifiedModels().add(validAircraftModel()));
    }

    // ── identity ─────────────────────────────────────────────────────────────

    @Test
    void ensureIdentityIsNullBeforePersistence() {
        assertNull(validPilot().identity());
    }

    // ── sameAs ───────────────────────────────────────────────────────────────

    @Test
    void ensureSameAsReturnsTrueForSameEmail() {
        final Pilot p1 = validPilot();
        final Pilot p2 = validPilot();
        assertTrue(p1.sameAs(p2));
    }

    @Test
    void ensureSameAsIsCaseInsensitive() {
        final Pilot p1 = new Pilot("João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        final Pilot p2 = new Pilot("João Silva", "JOAO@TAP.PT", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        assertTrue(p1.sameAs(p2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentEmail() {
        final Pilot p1 = validPilot();
        final Pilot p2 = new Pilot("Maria Santos", "maria@tap.pt", "+351910000002",
                validCompany(), List.of(validAircraftModel()));
        assertFalse(p1.sameAs(p2));
    }

    @Test
    void ensureSameAsReturnsFalseForNull() {
        assertFalse(validPilot().sameAs(null));
    }

    @Test
    void ensureSameAsReturnsFalseForOtherType() {
        assertFalse(validPilot().sameAs("not a pilot"));
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsNameEmailAndStatus() {
        final String result = validPilot().toString();
        assertTrue(result.contains("João Silva"));
        assertTrue(result.contains("joao.silva@tap.pt"));
        assertTrue(result.contains("ACTIVE"));
    }

    @Test
    void ensureToStringShowsInactiveAfterDeactivation() {
        // Arrange
        final Pilot pilot = validPilot();
        // Act
        pilot.deactivate();
        // Assert
        assertTrue(pilot.toString().contains("INACTIVE"));
    }
}