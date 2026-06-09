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
    private static final DateTimeFormatter T_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected boolean doShow() {
        final CreateFlightRouteController ctrl =
                new CreateFlightRouteController();

        System.out.println("\n--- Create Flight Route ---");

        // ── 1. Company IATA from session — not asked to the user ──────────
        final String companyIata;
        try {
            companyIata = ctrl.currentUserCompanyIata();
            System.out.println("  Company: " + companyIata.toUpperCase());
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println(">> ERROR: " + e.getMessage());
            return false;
        }

        // ── 2. List airports ──────────────────────────────────────────────
        final List<Airport> airports = new ArrayList<>();
        ctrl.availableAirports().forEach(airports::add);

        if (airports.isEmpty()) {
            System.out.println(
                    "\n>> ERROR: No airports registered. Create airports first.");
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

        // ── 3. Designator — validated prefix against company IATA ─────────
        String designator;
        while (true) {
            designator = Console.readLine(
                    "Route designator (must start with '" +
                            companyIata.toUpperCase() + "', e.g. " +
                            companyIata.toUpperCase() + "123):");
            if (designator == null || designator.isBlank()) {
                System.out.println("  >> ERROR: Designator cannot be empty.");
                continue;
            }
            try {
                ctrl.validateDesignatorPrefix(designator, companyIata);
                break;
            } catch (final IllegalArgumentException e) {
                System.out.println("  >> ERROR: " + e.getMessage());
            }
        }

        // ── 4. Origin airport — repeat until valid ────────────────────────
        int originChoice;
        while (true) {
            originChoice = Console.readInteger("Origin airport (number):");
            if (originChoice >= 1 && originChoice <= airports.size()) break;
            System.out.println("  >> ERROR: Enter a number between 1 and "
                    + airports.size() + ".");
        }

        // ── 5. Destination airport — repeat until valid and != origin ─────
        int destChoice;
        while (true) {
            destChoice = Console.readInteger("Destination airport (number):");
            if (destChoice < 1 || destChoice > airports.size()) {
                System.out.println("  >> ERROR: Enter a number between 1 and "
                        + airports.size() + ".");
                continue;
            }
            if (destChoice == originChoice) {
                System.out.println(
                        "  >> ERROR: Destination must differ from origin.");
                continue;
            }
            break;
        }

        final String originIata =
                airports.get(originChoice - 1).iataCode().code();
        final String destIata =
                airports.get(destChoice - 1).iataCode().code();

        // ── 6. Route type — repeat until valid ────────────────────────────
        int typeChoice;
        while (true) {
            System.out.println("\nRoute type:");
            System.out.println("  1. REGULAR (recurring weekly schedule)");
            System.out.println("  2. CHARTER (specific dates)");
            typeChoice = Console.readInteger("Select type (1 or 2):");
            if (typeChoice == 1 || typeChoice == 2) break;
            System.out.println("  >> ERROR: Please enter 1 or 2.");
        }

        try {
            FlightRoute route;

            if (typeChoice == 2) {
                // ── CHARTER ───────────────────────────────────────────────
                LocalDateTime dep = null;
                while (dep == null) {
                    final String s =
                            Console.readLine("Departure (yyyy-MM-dd HH:mm):");
                    try {
                        dep = LocalDateTime.parse(s, DT_FMT);
                    } catch (final DateTimeParseException e) {
                        System.out.println(
                                "  >> ERROR: Invalid format. Use yyyy-MM-dd HH:mm.");
                    }
                }

                LocalDateTime arr = null;
                while (arr == null) {
                    final String s =
                            Console.readLine("Arrival   (yyyy-MM-dd HH:mm):");
                    try {
                        arr = LocalDateTime.parse(s, DT_FMT);
                        if (!arr.isAfter(dep)) {
                            System.out.println(
                                    "  >> ERROR: Arrival must be after departure.");
                            arr = null;
                        }
                    } catch (final DateTimeParseException e) {
                        System.out.println(
                                "  >> ERROR: Invalid format. Use yyyy-MM-dd HH:mm.");
                    }
                }

                route = ctrl.createCharterRoute(
                        designator, companyIata, originIata, destIata, dep, arr);

                System.out.printf("%n>> Route '%s' created successfully.%n",
                        route.identity().value());
                System.out.printf("   Type      : CHARTER%n");
                System.out.printf("   Company   : %s%n",
                        companyIata.toUpperCase());
                System.out.printf("   Route     : %s → %s%n",
                        originIata, destIata);
                System.out.printf("   Departure : %s%n", dep);
                System.out.printf("   Arrival   : %s%n", arr);
                System.out.printf("   Status    : ACTIVE%n");

            } else {
                // ── REGULAR ───────────────────────────────────────────────
                final List<ScheduleEntry> entries = new ArrayList<>();
                System.out.println(
                        "\nAdd weekly schedule entries (at least one):");

                boolean addMore = true;
                while (addMore) {
                    System.out.println();
                    System.out.println("  Days of week:");
                    System.out.println(
                            "    1=MONDAY  2=TUESDAY  3=WEDNESDAY  4=THURSDAY");
                    System.out.println(
                            "    5=FRIDAY  6=SATURDAY  7=SUNDAY");

                    int dayNum;
                    while (true) {
                        dayNum = Console.readInteger("  Day (1-7):");
                        if (dayNum >= 1 && dayNum <= 7) break;
                        System.out.println(
                                "  >> ERROR: Enter a number between 1 and 7.");
                    }
                    final DayOfWeek day = DayOfWeek.of(dayNum);

                    LocalTime dep = null;
                    while (dep == null) {
                        final String s =
                                Console.readLine("  Departure time (HH:mm):");
                        try {
                            dep = LocalTime.parse(s, T_FMT);
                        } catch (final DateTimeParseException e) {
                            System.out.println(
                                    "  >> ERROR: Invalid format. Use HH:mm.");
                        }
                    }

                    LocalTime arr = null;
                    while (arr == null) {
                        final String s =
                                Console.readLine("  Arrival time   (HH:mm):");
                        try {
                            arr = LocalTime.parse(s, T_FMT);
                            if (!arr.isAfter(dep)) {
                                System.out.println(
                                        "  >> ERROR: Arrival must be after departure.");
                                arr = null;
                            }
                        } catch (final DateTimeParseException e) {
                            System.out.println(
                                    "  >> ERROR: Invalid format. Use HH:mm.");
                        }
                    }

                    entries.add(new ScheduleEntry(day, dep, arr));
                    System.out.printf("  >> Entry added: %s %s → %s%n",
                            day.name(), dep, arr);

                    final String more =
                            Console.readLine("Add another entry? (y/n):");
                    addMore = more != null &&
                            more.trim().equalsIgnoreCase("y");
                }

                route = ctrl.createRegularRoute(
                        designator, companyIata, originIata, destIata, entries);

                System.out.printf("%n>> Route '%s' created successfully.%n",
                        route.identity().value());
                System.out.printf("   Type     : REGULAR%n");
                System.out.printf("   Company  : %s%n",
                        companyIata.toUpperCase());
                System.out.printf("   Route    : %s → %s%n",
                        originIata, destIata);
                System.out.printf("   Schedule :%n");
                route.recurringSchedule().entries()
                        .forEach(e -> System.out.printf(
                                "     %s %s → %s%n",
                                e.dayOfWeek().name(),
                                e.departureTime(),
                                e.scheduledArrivalTime()));
                System.out.printf("   Status   : ACTIVE%n");
            }

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