package ui;

import controller.CreateFlightRouteController;
import domain.Airport;
import domain.flightroute.FlightRoute;
import domain.flightroute.ScheduleEntry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CreateFlightRouteUI extends AbstractUI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter T_FMT  =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected boolean doShow() {
        final CreateFlightRouteController ctrl =
                new CreateFlightRouteController();

        System.out.println("\n--- Create Flight Route ---");

        // 1. list airports
        final List<Airport> airports = new ArrayList<>();
        ctrl.availableAirports().forEach(airports::add);

        if (airports.isEmpty()) {
            System.out.println("\n>> ERROR: No airports registered. Create airports first (US052).");
            return false;
        }

        System.out.println("\nAvailable airports:");
        for (int i = 0; i < airports.size(); i++) {
            final Airport a = airports.get(i);
            System.out.printf("  %d. %s / %s — %s%n",
                    i + 1,
                    a.iataCode().code(),
                    a.identity().code(),
                    a.name().toString());
        }

        // 2. collect route data
        final String designator  = Console.readLine("Route designator (e.g. TP123):");
        final String companyIata = Console.readLine("Company IATA code:");

        final int originChoice = Console.readInteger("Origin airport (number):");
        if (originChoice < 1 || originChoice > airports.size()) {
            System.out.println(">> Invalid option.");
            return false;
        }

        final int destChoice = Console.readInteger("Destination airport (number):");
        if (destChoice < 1 || destChoice > airports.size()) {
            System.out.println(">> Invalid option.");
            return false;
        }

        final String originIata = airports.get(originChoice - 1).iataCode().code();
        final String destIata   = airports.get(destChoice   - 1).iataCode().code();

        // 3. route type
        System.out.println("\nRoute type:");
        System.out.println("  1. REGULAR");
        System.out.println("  2. CHARTER");
        final int typeChoice = Console.readInteger("Select type:");

        try {
            FlightRoute route;

            if (typeChoice == 2) {
                // CHARTER
                final String depStr = Console.readLine("Departure (yyyy-MM-dd HH:mm):");
                final String arrStr = Console.readLine("Arrival   (yyyy-MM-dd HH:mm):");
                final LocalDateTime dep = LocalDateTime.parse(depStr, DT_FMT);
                final LocalDateTime arr = LocalDateTime.parse(arrStr, DT_FMT);

                route = ctrl.createCharterRoute(
                        designator, companyIata, originIata, destIata, dep, arr);

                System.out.printf("%n>> Route '%s' created successfully.%n",
                        route.identity().value());
                System.out.printf("   Type      : CHARTER%n");
                System.out.printf("   Route     : %s → %s%n", originIata, destIata);
                System.out.printf("   Departure : %s%n", dep);
                System.out.printf("   Arrival   : %s%n", arr);
                System.out.printf("   Status    : ACTIVE%n");

            } else if (typeChoice == 1) {
                // REGULAR
                final List<ScheduleEntry> entries = new ArrayList<>();
                System.out.println("\nAdd schedule entries (at least one):");

                boolean addMore = true;
                while (addMore) {
                    System.out.println("\n  Day of week: 1=MON 2=TUE 3=WED 4=THU 5=FRI 6=SAT 7=SUN");
                    final int dayNum = Console.readInteger("  Day:");
                    if (dayNum < 1 || dayNum > 7) {
                        System.out.println("  >> Invalid day.");
                        continue;
                    }
                    final DayOfWeek day = DayOfWeek.of(dayNum);
                    final String depStr = Console.readLine("  Departure time (HH:mm):");
                    final String arrStr = Console.readLine("  Arrival time   (HH:mm):");
                    final LocalTime dep = LocalTime.parse(depStr, T_FMT);
                    final LocalTime arr = LocalTime.parse(arrStr, T_FMT);
                    entries.add(new ScheduleEntry(day, dep, arr));

                    final String more = Console.readLine("Add another entry? (y/n):");
                    addMore = more.trim().equalsIgnoreCase("y");
                }

                route = ctrl.createRegularRoute(
                        designator, companyIata, originIata, destIata, entries);

                System.out.printf("%n>> Route '%s' created successfully.%n",
                        route.identity().value());
                System.out.printf("   Type     : REGULAR%n");
                System.out.printf("   Route    : %s → %s%n", originIata, destIata);
                System.out.printf("   Schedule :%n");
                route.recurringSchedule().entries()
                        .forEach(e -> System.out.printf("     %s%n", e));
                System.out.printf("   Status   : ACTIVE%n");

            } else {
                System.out.println(">> Invalid type option.");
                return false;
            }

        } catch (final DateTimeParseException e) {
            System.out.println("\n>> ERROR: Invalid date/time format. Use yyyy-MM-dd HH:mm or HH:mm.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US073 - Create Flight Route";
    }
}