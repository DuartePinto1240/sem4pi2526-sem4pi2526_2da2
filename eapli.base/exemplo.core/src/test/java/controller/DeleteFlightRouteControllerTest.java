package controller;

import domain.AirTransportCompany;
import domain.flightroute.FlightRoute;
import domain.flightroute.RecurringSchedule;
import domain.flightroute.RouteDesignator;
import domain.flightroute.ScheduleEntry;
import domain.flightroute.ScheduledDates;
import dto.FlightRouteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.AirTransportCompanyRepository;
import repositories.FlightPlanRepository;
import repositories.FlightRouteRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteFlightRouteControllerTest {

    private AirTransportCompanyRepository companyRepo;
    private FlightRouteRepository         routeRepo;
    private FlightPlanRepository          flightPlanRepo;
    private DeleteFlightRouteController   ctrl;

    private static final String EMAIL = "atcc@tap.pt";

    // helpers

    private AirTransportCompany company() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    private FlightRoute charterRoute(final String designator) {
        return new FlightRoute(designator, "TP", "OPO", "LIS",
                new ScheduledDates(
                        LocalDateTime.of(2026, 12, 12, 10, 0),
                        LocalDateTime.of(2026, 12, 12, 11, 0)));
    }

    private FlightRoute regularRoute(final String designator) {
        return new FlightRoute(designator, "TP", "OPO", "LIS",
                new RecurringSchedule(List.of(new ScheduleEntry(
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)))));
    }

    @BeforeEach
    void setUp() {
        companyRepo    = mock(AirTransportCompanyRepository.class);
        routeRepo      = mock(FlightRouteRepository.class);
        flightPlanRepo = mock(FlightPlanRepository.class);
        ctrl           = new DeleteFlightRouteController(
                routeRepo, flightPlanRepo, companyRepo);
    }

    // getCompanyRoutes tests

    @Test
    void ensureActiveCharterRoutesAreReturned() {
        // Arrange
        when(companyRepo.findByCollaborator(EMAIL))
                .thenReturn(Optional.of(company()));
        when(routeRepo.findByCompanyIata("TP"))
                .thenReturn(List.of(charterRoute("TP001")));
        // Act
        final List<FlightRouteDTO> result = ctrl.getCompanyRoutes(EMAIL);
        // Assert
        assertEquals(1, result.size());
        assertEquals("TP001", result.get(0).designator);
    }

    @Test
    void ensureRegularRoutesAreExcludedFromListing() {
        // Arrange — only regular routes returned
        when(companyRepo.findByCollaborator(EMAIL))
                .thenReturn(Optional.of(company()));
        when(routeRepo.findByCompanyIata("TP"))
                .thenReturn(List.of(regularRoute("TP001")));
        // Act
        final List<FlightRouteDTO> result = ctrl.getCompanyRoutes(EMAIL);
        // Assert — regular routes must not appear
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureInactiveCharterRoutesAreExcluded() {
        // Arrange
        final FlightRoute inactive = charterRoute("TP001");
        inactive.deactivate(LocalDate.now().minusDays(1));
        when(companyRepo.findByCollaborator(EMAIL))
                .thenReturn(Optional.of(company()));
        when(routeRepo.findByCompanyIata("TP"))
                .thenReturn(List.of(inactive));
        // Act
        final List<FlightRouteDTO> result = ctrl.getCompanyRoutes(EMAIL);
        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureEmptyListWhenNoRoutesExist() {
        // Arrange
        when(companyRepo.findByCollaborator(EMAIL))
                .thenReturn(Optional.of(company()));
        when(routeRepo.findByCompanyIata("TP")).thenReturn(List.of());
        // Act
        final List<FlightRouteDTO> result = ctrl.getCompanyRoutes(EMAIL);
        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureThrowsWhenCollaboratorHasNoCompany() {
        // Arrange
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.getCompanyRoutes(EMAIL));
    }

    // deleteRoute — success

    @Test
    void ensureCharterRouteIsDeactivatedSuccessfully() {
        // Arrange
        final FlightRoute route = charterRoute("TP001");
        when(routeRepo.ofIdentity(new RouteDesignator("TP001")))
                .thenReturn(Optional.of(route));
        when(flightPlanRepo.existsPlannedFlightsAfter(any(), any()))
                .thenReturn(false);
        when(routeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.deleteRoute("TP001", LocalDate.now());
        // Assert — US074.5
        assertFalse(route.isActive());
        verify(routeRepo).save(route);
    }

    @Test
    void ensureDeactivationDateIsStoredOnRoute() {
        // Arrange
        final FlightRoute route = charterRoute("TP001");
        final LocalDate date = LocalDate.of(2026, 11, 11);
        when(routeRepo.ofIdentity(new RouteDesignator("TP001")))
                .thenReturn(Optional.of(route));
        when(flightPlanRepo.existsPlannedFlightsAfter(any(), any()))
                .thenReturn(false);
        when(routeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.deleteRoute("TP001", date);
        // Assert
        assertEquals(date, route.deactivationDate());
    }

    // deleteRoute — REGULAR blocked

    @Test
    void ensureRegularRouteCannotBeDeactivated() {
        // Arrange
        final FlightRoute route = regularRoute("TP001");
        when(routeRepo.ofIdentity(new RouteDesignator("TP001")))
                .thenReturn(Optional.of(route));
        // Act & Assert — US074.1
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.deleteRoute("TP001", LocalDate.now()));
        verify(routeRepo, never()).save(any());
    }

    // deleteRoute — blocking condition

    @Test
    void ensureRouteWithPlannedFlightsCannotBeDeactivated() {
        // Arrange
        final FlightRoute route = charterRoute("TP001");
        when(routeRepo.ofIdentity(new RouteDesignator("TP001")))
                .thenReturn(Optional.of(route));
        when(flightPlanRepo.existsPlannedFlightsAfter(any(), any()))
                .thenReturn(true);
        // Act & Assert — US074.2
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.deleteRoute("TP001", LocalDate.now()));
        verify(routeRepo, never()).save(any());
    }

    // deleteRoute — not found

    @Test
    void ensureDeactivatingNonExistentRouteThrows() {
        // Arrange
        when(routeRepo.ofIdentity(any())).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.deleteRoute("TP999", LocalDate.now()));
        verify(routeRepo, never()).save(any());
    }
}