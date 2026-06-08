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
import dto.PilotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.AirTransportCompanyRepository;
import repositories.FlightPlanRepository;
import repositories.PilotRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RemovePilotControllerTest {

    private AirTransportCompanyRepository companyRepo;
    private PilotRepository               pilotRepo;
    private FlightPlanRepository          flightPlanRepo;
    private RemovePilotController         ctrl;

    private static final String EMAIL = "atcc@tap.pt";

    // helpers

    private AirTransportCompany company() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    private AircraftModel model() {
        final EngineModel e = new EngineModel(
                "LEAP-1B", "CFM International", MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));
        final FlightCharacteristics c = new FlightCharacteristics(
                41413, 65317, 79016, 26020, 12500, 230, 5500, 125.08, 0.0265, 1.45);
        return new AircraftModel("737 MAX", "Boeing",
                AircraftType.PASSENGER, c, List.of(e));
    }

    private Pilot pilot(final String email) {
        return new Pilot("João Silva", email, "+351910000001",
                company(), List.of(model()));
    }

    @BeforeEach
    void setUp() {
        companyRepo    = mock(AirTransportCompanyRepository.class);
        pilotRepo      = mock(PilotRepository.class);
        flightPlanRepo = mock(FlightPlanRepository.class);
        ctrl           = new RemovePilotController(companyRepo, pilotRepo, flightPlanRepo);
    }

    // getActivePilots

    @Test
    void ensureActivePilotsAreReturned() {
        // Arrange
        final AirTransportCompany c = company();
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.of(c));
        when(pilotRepo.findActiveByCompany(c))
                .thenReturn(List.of(pilot("joao@tap.pt")));
        // Act
        final List<PilotDTO> result = ctrl.getActivePilots(EMAIL);
        // Assert
        assertEquals(1, result.size());
        assertEquals("joao@tap.pt", result.get(0).email);
    }

    @Test
    void ensureEmptyListWhenNoPilotsRegistered() {
        // Arrange
        final AirTransportCompany c = company();
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.of(c));
        when(pilotRepo.findActiveByCompany(c)).thenReturn(List.of());
        // Act
        final List<PilotDTO> result = ctrl.getActivePilots(EMAIL);
        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureThrowsWhenCollaboratorHasNoCompany() {
        // Arrange
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.getActivePilots(EMAIL));
    }

    @Test
    void ensureReturnedPilotsAreDTOs() {
        // Arrange — verifies domain objects are converted to DTOs
        final AirTransportCompany c = company();
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.of(c));
        when(pilotRepo.findActiveByCompany(c))
                .thenReturn(List.of(pilot("joao@tap.pt"), pilot("maria@tap.pt")));
        // Act
        final List<PilotDTO> result = ctrl.getActivePilots(EMAIL);
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.active));
    }

    // removePilot — success

    @Test
    void ensurePilotIsDeactivatedSuccessfully() {
        // Arrange
        final Pilot p = pilot("joao@tap.pt");
        when(pilotRepo.findByEmail("joao@tap.pt")).thenReturn(Optional.of(p));
        when(flightPlanRepo.existsAssignedFlightPlansForPilot("joao@tap.pt"))
                .thenReturn(false);
        when(pilotRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.removePilot("joao@tap.pt");
        // Assert — US077.3
        assertFalse(p.isActive());
        verify(pilotRepo).save(p);
    }

    @Test
    void ensureDeactivationIsIdempotent() {
        // Arrange — pilot already inactive
        final Pilot p = pilot("joao@tap.pt");
        p.deactivate();
        when(pilotRepo.findByEmail("joao@tap.pt")).thenReturn(Optional.of(p));
        when(flightPlanRepo.existsAssignedFlightPlansForPilot("joao@tap.pt"))
                .thenReturn(false);
        when(pilotRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act & Assert — idempotent: no exception thrown
        assertDoesNotThrow(() -> ctrl.removePilot("joao@tap.pt"));
        assertFalse(p.isActive());
    }

    @Test
    void ensureOnlyTargetPilotIsDeactivated() {
        // Arrange
        final Pilot joao  = pilot("joao@tap.pt");
        final Pilot maria = pilot("maria@tap.pt");
        when(pilotRepo.findByEmail("joao@tap.pt")).thenReturn(Optional.of(joao));
        when(flightPlanRepo.existsAssignedFlightPlansForPilot("joao@tap.pt"))
                .thenReturn(false);
        when(pilotRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.removePilot("joao@tap.pt");
        // Assert — maria untouched
        assertFalse(joao.isActive());
        assertTrue(maria.isActive());
        verify(pilotRepo, never()).save(maria);
    }

    // removePilot — blocking condition

    @Test
    void ensurePilotWithAssignedFlightPlansCannotBeDeactivated() {
        // Arrange
        final Pilot p = pilot("joao@tap.pt");
        when(pilotRepo.findByEmail("joao@tap.pt")).thenReturn(Optional.of(p));
        when(flightPlanRepo.existsAssignedFlightPlansForPilot("joao@tap.pt"))
                .thenReturn(true);
        // Act & Assert — US077.2
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.removePilot("joao@tap.pt"));
        verify(pilotRepo, never()).save(any());
    }

    // removePilot — not found

    @Test
    void ensureRemovingNonExistentPilotThrows() {
        // Arrange
        when(pilotRepo.findByEmail("ghost@tap.pt")).thenReturn(Optional.empty());
        when(flightPlanRepo.existsAssignedFlightPlansForPilot("ghost@tap.pt"))
                .thenReturn(false);
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.removePilot("ghost@tap.pt"));
        verify(pilotRepo, never()).save(any());
    }
}