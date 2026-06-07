package controller;

import domain.AirTransportCompany;
import domain.flightroute.FlightRoute;
import domain.flightroute.RecurringSchedule;
import domain.flightroute.RouteDesignator;
import domain.flightroute.ScheduleEntry;
import dto.FlightRouteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.AirTransportCompanyRepository;
import repositories.FlightPlanRepository;
import repositories.FlightRouteRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    private static final String           EMAIL    = "atcc@tap.pt";
    private static final RecurringSchedule SCHEDULE = new RecurringSchedule(List.of(
            new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))));

    private AirTransportCompany company() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    private FlightRoute activeRoute(final String designator, final String iata) {
        return new FlightRoute(designator, iata, "OPO", "LIS", SCHEDULE);
    }

    @BeforeEach
    void setUp() {
        companyRepo    = mock(AirTransportCompanyRepository.class);
        routeRepo      = mock(FlightRouteRepository.class);
        flightPlanRepo = mock(FlightPlanRepository.class);
        ctrl           = new DeleteFlightRouteController(routeRepo, flightPlanRepo, companyRepo);
    }

    // getCompanyRoutes

    @Test
    void ensureActiveRoutesAreReturned() {
        // Arrange
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.of(company()));
        when(routeRepo.findByCompanyIata("TP"))
                .thenReturn(List.of(activeRoute("TP001", "TP")));
        // Act
        final List<FlightRouteDTO> result = ctrl.getCompanyRoutes(EMAIL);
        // Assert
        assertEquals(1, result.size());
        assertEquals("TP001", result.get(0).designator);
    }

    @Test
    void ensureInactiveRoutesAreExcluded() {
        // Arrange
        final FlightRoute inactive = activeRoute("TP001", "TP");
        inactive.deactivate(LocalDate.now().minusDays(1));
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.of(company()));
        when(routeRepo.findByCompanyIata("TP")).thenReturn(List.of(inactive));
        // Act
        final List<FlightRouteDTO> result = ctrl.getCompanyRoutes(EMAIL);
        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureEmptyListWhenNoRoutesExist() {
        // Arrange
        when(companyRepo.findByCollaborator(EMAIL)).thenReturn(Optional.of(company()));
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
    void ensureRouteIsDeactivatedSuccessfully() {
        // Arrange
        final FlightRoute route = activeRoute("TP001", "TP");
        when(routeRepo.ofIdentity(new RouteDesignator("TP001")))
                .thenReturn(Optional.of(route));
        when(flightPlanRepo.existsPlannedFlightsAfter(
                new RouteDesignator("TP001"), LocalDate.now()))
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
        final FlightRoute route = activeRoute("TP001", "TP");
        final LocalDate date = LocalDate.of(2026, 7, 1);
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

    // deleteRoute — blocking condition

    @Test
    void ensureRouteWithPlannedFlightsCannotBeDeactivated() {
        // Arrange
        final FlightRoute route = activeRoute("TP001", "TP");
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