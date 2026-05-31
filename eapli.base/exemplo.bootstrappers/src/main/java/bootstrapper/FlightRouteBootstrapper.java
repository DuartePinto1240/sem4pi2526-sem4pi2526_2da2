package bootstrapper;

import controller.CreateFlightRouteController;
import domain.flightroute.FlightRoute;
import domain.flightroute.ScheduleEntry;
import eapli.framework.actions.Action;
import persistence.PersistenceContext;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Bootstrap action that pre-loads demo flight routes.
 *
 * Executed once at application startup. Routes that already exist
 * (same designator) are silently skipped.
 *
 * Assumes AirTransportCompanyBootstrapper and AirportBootstrapper
 * have already run (Bootstrap.java execution order).
 *
 * Pre-loaded routes:
 *   TP001  TAP  OPO -> LIS  regular  Mon/Wed/Fri 08:00-09:00
 *   TP002  TAP  LIS -> OPO  regular  Tue/Thu/Sat 10:00-11:00
 *   FR001  Ryanair  OPO -> MAD  regular  Mon/Thu 07:00-08:30
 */
public class FlightRouteBootstrapper implements Action {

    @Override
    public boolean execute() {
        final CreateFlightRouteController ctrl = new CreateFlightRouteController();

        register(ctrl, "TP001", "TP", "OPO", "LIS", List.of(
                new ScheduleEntry(DayOfWeek.MONDAY,    LocalTime.of(8, 0),  LocalTime.of(9, 0)),
                new ScheduleEntry(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0),  LocalTime.of(9, 0)),
                new ScheduleEntry(DayOfWeek.FRIDAY,    LocalTime.of(8, 0),  LocalTime.of(9, 0))));

        register(ctrl, "TP002", "TP", "LIS", "OPO", List.of(
                new ScheduleEntry(DayOfWeek.TUESDAY,   LocalTime.of(10, 0), LocalTime.of(11, 0)),
                new ScheduleEntry(DayOfWeek.THURSDAY,  LocalTime.of(10, 0), LocalTime.of(11, 0)),
                new ScheduleEntry(DayOfWeek.SATURDAY,  LocalTime.of(10, 0), LocalTime.of(11, 0))));

        register(ctrl, "FR001", "FR", "OPO", "MAD", List.of(
                new ScheduleEntry(DayOfWeek.MONDAY,    LocalTime.of(7, 0),  LocalTime.of(8, 30)),
                new ScheduleEntry(DayOfWeek.THURSDAY,  LocalTime.of(7, 0),  LocalTime.of(8, 30))));

        return true;
    }

    private void register(final CreateFlightRouteController ctrl,
                          final String designator,
                          final String companyIata,
                          final String origin,
                          final String destination,
                          final List<ScheduleEntry> entries) {
        try {
            final FlightRoute route =
                    ctrl.createRegularRoute(designator, companyIata, origin, destination, entries);
        } catch (final Exception e) {
            // Route already exists or airports not found — skip silently
        }
    }
}
