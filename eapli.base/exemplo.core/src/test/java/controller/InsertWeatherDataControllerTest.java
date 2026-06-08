package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.AltitudeRange;
import domain.GeographicBoundary;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherSubArea;
import domain.WindCondition;
import dto.AirControlAreaDTO;
import dto.FlightPlanDTO;
import dto.WeatherDataDTO;
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.AirControlAreaRepository;
import repositories.FlightPlanRepository;
import repositories.WeatherDataRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InsertWeatherDataControllerTest {

    private FlightPlanRepository        flightPlanRepo;
    private WeatherDataRepository       weatherRepo;
    private AirControlAreaRepository    areaRepo;
    private InsertWeatherDataController ctrl;

    private static final String PILOT_EMAIL = "joao@tap.pt";

    // helpers

    private FlightPlan draftPlan() {
        final FlightLeg leg = new FlightLeg("OPO", "2026-07-01", "10:00",
                "LIS", "11:00", 5000.0, "kg");
        return new FlightPlan("TP001", "regular", List.of(leg));
    }

    private FlightPlan draftPlanWithId(final Long id) {
        final FlightPlan p = draftPlan();
        setField(p, "id", id);
        return p;
    }

    private AirControlArea area(final String code, final String name) {
        return new AirControlArea(code, name,
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0));
    }

    private WeatherData weatherData(final String areaCode) {
        return new WeatherData(
                new AirControlAreaCode(areaCode),
                new WeatherSubArea(
                        new GeographicBoundary(36.0, 39.0, -10.0, -7.0),
                        new AltitudeRange(0.0, 10000.0)),
                new ObservationPeriod(
                        LocalDateTime.of(2026, 6, 1, 0, 0),
                        LocalDateTime.of(2026, 6, 30, 23, 59)),
                new WindCondition(270.0, 15.0));
    }

    private void setField(final Object target, final String name, final Object value) {
        try {
            final Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (final Exception e) {
            fail("Could not set field '" + name + "': " + e.getMessage());
        }
    }

    private void setStatus(final FlightPlan plan, final FlightPlanStatus status) {
        setField(plan, "status", status);
    }

    @BeforeEach
    void setUp() {
        flightPlanRepo = mock(FlightPlanRepository.class);
        weatherRepo    = mock(WeatherDataRepository.class);
        areaRepo       = mock(AirControlAreaRepository.class);
        ctrl           = new InsertWeatherDataController(
                flightPlanRepo, weatherRepo, areaRepo);
    }

    // getPilotFlightPlans

    @Test
    void ensureModifiablePlansAreReturned() {
        // Arrange
        when(flightPlanRepo.findByPilot(PILOT_EMAIL))
                .thenReturn(List.of(draftPlan()));
        // Act
        final List<FlightPlanDTO> result = ctrl.getPilotFlightPlans(PILOT_EMAIL);
        // Assert — DRAFT is modifiable
        assertEquals(1, result.size());
        verify(flightPlanRepo).findByPilot(PILOT_EMAIL);
    }

    @Test
    void ensureTestedPlansAreIncluded() {
        // Arrange — TESTED plans are also modifiable
        final FlightPlan plan = draftPlan();
        setStatus(plan, FlightPlanStatus.TESTED);
        when(flightPlanRepo.findByPilot(PILOT_EMAIL)).thenReturn(List.of(plan));
        // Act
        final List<FlightPlanDTO> result = ctrl.getPilotFlightPlans(PILOT_EMAIL);
        // Assert — US082.4
        assertEquals(1, result.size());
    }

    @Test
    void ensureEmptyListWhenNoPilotFlightPlans() {
        // Arrange
        when(flightPlanRepo.findByPilot(PILOT_EMAIL)).thenReturn(List.of());
        // Act
        final List<FlightPlanDTO> result = ctrl.getPilotFlightPlans(PILOT_EMAIL);
        // Assert
        assertTrue(result.isEmpty());
    }

    // getAvailableAreas

    @Test
    void ensureAvailableAreasAreReturned() {
        // Arrange
        when(areaRepo.findAll()).thenReturn(List.of(
                area("LPPC", "Lisboa"),
                area("LPPO", "Santa Maria")));
        // Act
        final List<AirControlAreaDTO> result = ctrl.getAvailableAreas();
        // Assert
        assertEquals(2, result.size());
        verify(areaRepo).findAll();
    }

    @Test
    void ensureEmptyListWhenNoAreasExist() {
        // Arrange
        when(areaRepo.findAll()).thenReturn(List.of());
        // Act
        final List<AirControlAreaDTO> result = ctrl.getAvailableAreas();
        // Assert
        assertTrue(result.isEmpty());
    }

    // getAvailableWeatherData

    @Test
    void ensureWeatherDataIsReturnedForArea() {
        // Arrange — use a real WeatherData object instead of mocking (Java 26 limitation)
        when(weatherRepo.findByArea(new AirControlAreaCode("LPPC")))
                .thenReturn(List.of(weatherData("LPPC")));
        // Act
        final List<WeatherDataDTO> result = ctrl.getAvailableWeatherData("LPPC");
        // Assert
        assertEquals(1, result.size());
        verify(weatherRepo).findByArea(new AirControlAreaCode("LPPC"));
    }

    @Test
    void ensureEmptyListWhenNoWeatherDataForArea() {
        // Arrange
        when(weatherRepo.findByArea(any(AirControlAreaCode.class)))
                .thenReturn(List.of());
        // Act
        final List<WeatherDataDTO> result = ctrl.getAvailableWeatherData("LPPC");
        // Assert
        assertTrue(result.isEmpty());
    }

    // insertWeatherData — success

    @Test
    void ensureWeatherDataIsAssociatedSuccessfully() {
        // Arrange
        final FlightPlan plan = draftPlanWithId(1L);
        when(flightPlanRepo.ofIdentity(1L)).thenReturn(Optional.of(plan));
        when(flightPlanRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.insertWeatherData(1L, 42L);
        // Assert — US082.2
        assertEquals(42L, plan.weatherDataId());
        verify(flightPlanRepo).save(plan);
    }

    @Test
    void ensureWeatherDataIdIsStoredOnFlightPlan() {
        // Arrange
        final FlightPlan plan = draftPlanWithId(1L);
        when(flightPlanRepo.ofIdentity(1L)).thenReturn(Optional.of(plan));
        when(flightPlanRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.insertWeatherData(1L, 99L);
        // Assert
        assertEquals(99L, plan.weatherDataId());
    }

    // insertWeatherData — TESTED → DRAFT

    @Test
    void ensureAddingWeatherDataToTestedPlanVoidsTest() {
        // Arrange
        final FlightPlan plan = draftPlanWithId(1L);
        setStatus(plan, FlightPlanStatus.TESTED);
        when(flightPlanRepo.ofIdentity(1L)).thenReturn(Optional.of(plan));
        when(flightPlanRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.insertWeatherData(1L, 42L);
        // Assert — US082.3
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureAddingWeatherDataToDraftKeepsDraftStatus() {
        // Arrange
        final FlightPlan plan = draftPlanWithId(1L);
        when(flightPlanRepo.ofIdentity(1L)).thenReturn(Optional.of(plan));
        when(flightPlanRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        ctrl.insertWeatherData(1L, 42L);
        // Assert
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    // insertWeatherData — error paths

    @Test
    void ensureInsertThrowsWhenFlightPlanNotFound() {
        // Arrange
        when(flightPlanRepo.ofIdentity(999L)).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.insertWeatherData(999L, 42L));
        verify(flightPlanRepo, never()).save(any());
    }
}