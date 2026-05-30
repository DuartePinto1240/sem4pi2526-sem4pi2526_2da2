package controller;

import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import domain.pilot.Pilot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.AircraftModelRepository;
import repositories.AirTransportCompanyRepository;
import repositories.PilotRepository;
import util.Either;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddPilotControllerTest {

    private PilotRepository               pilotRepo;
    private AirTransportCompanyRepository companyRepo;
    private AircraftModelRepository       modelRepo;
    private AddPilotController            controller;

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

    @BeforeEach
    void setUp() {
        pilotRepo   = mock(PilotRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        modelRepo   = mock(AircraftModelRepository.class);
        controller  = new AddPilotController(pilotRepo, companyRepo, modelRepo);
    }

    // ── availableCompanies ────────────────────────────────────────────────────

    @Test
    void ensureAvailableCompaniesReturnsRepositoryResult() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of(validCompany()));
        // Act
        final Iterable<AirTransportCompany> result = controller.availableCompanies();
        // Assert
        assertNotNull(result);
        verify(companyRepo).findAll();
    }

    // ── availableAircraftModels ───────────────────────────────────────────────

    @Test
    void ensureAvailableAircraftModelsReturnsRepositoryResult() {
        // Arrange
        when(modelRepo.findAll()).thenReturn(List.of(validAircraftModel()));
        // Act
        final Iterable<AircraftModel> result = controller.availableAircraftModels();
        // Assert
        assertNotNull(result);
        verify(modelRepo).findAll();
    }

    // ── addPilot — success ────────────────────────────────────────────────────

    @Test
    void ensureAddPilotSucceedsWithValidData() {
        // Arrange
        when(pilotRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(pilotRepo.save(any(Pilot.class))).thenAnswer(i -> i.getArgument(0));
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        // Assert
        assertTrue(result.isRight());
        assertEquals("joao@tap.pt", result.getRight().email());
        verify(pilotRepo).save(any(Pilot.class));
    }

    // ── addPilot — duplicate email ────────────────────────────────────────────

    @Test
    void ensureAddPilotFailsWhenEmailAlreadyExists() {
        // Arrange
        final Pilot existing = new Pilot("João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        when(pilotRepo.findByEmail("joao@tap.pt")).thenReturn(Optional.of(existing));
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        // Assert
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().contains("joao@tap.pt"));
        verify(pilotRepo, never()).save(any());
    }

    // ── addPilot — empty certified models ─────────────────────────────────────

    @Test
    void ensureAddPilotFailsWhenNoCertifiedModelsProvided() {
        // Arrange
        when(pilotRepo.findByEmail(any())).thenReturn(Optional.empty());
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), Collections.emptyList());
        // Assert
        assertTrue(result.isLeft());
        verify(pilotRepo, never()).save(any());
    }

    @Test
    void ensureAddPilotFailsWhenCertifiedModelsIsNull() {
        // Arrange
        when(pilotRepo.findByEmail(any())).thenReturn(Optional.empty());
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), null);
        // Assert
        assertTrue(result.isLeft());
        verify(pilotRepo, never()).save(any());
    }

    // ── addPilot — saved pilot has correct data ───────────────────────────────

    @Test
    void ensureSavedPilotBelongsToCorrectCompany() {
        // Arrange
        final AirTransportCompany company = validCompany();
        when(pilotRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(pilotRepo.save(any(Pilot.class))).thenAnswer(i -> i.getArgument(0));
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                company, List.of(validAircraftModel()));
        // Assert
        assertTrue(result.isRight());
        assertTrue(result.getRight().company().sameAs(company));
    }

    @Test
    void ensureSavedPilotIsActiveByDefault() {
        // Arrange
        when(pilotRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(pilotRepo.save(any(Pilot.class))).thenAnswer(i -> i.getArgument(0));
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(validAircraftModel()));
        // Assert
        assertTrue(result.isRight());
        assertTrue(result.getRight().isActive());
    }

    @Test
    void ensureSavedPilotHasCorrectCertifiedModels() {
        // Arrange
        final AircraftModel model = validAircraftModel();
        when(pilotRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(pilotRepo.save(any(Pilot.class))).thenAnswer(i -> i.getArgument(0));
        // Act
        final Either<String, Pilot> result = controller.addPilot(
                "João Silva", "joao@tap.pt", "+351910000001",
                validCompany(), List.of(model));
        // Assert
        assertTrue(result.isRight());
        assertTrue(result.getRight().certifiedModels().contains(model));
    }
}