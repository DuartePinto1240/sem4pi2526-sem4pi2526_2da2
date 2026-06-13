package controller;

import domain.aircraft.Aircraft;
import domain.pilot.Pilot;
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanBuilderListener.FlightData;
import lprog.FlightPlanBuilderListener.LegData;
import lprog.FlightPlanBuilderListener.SegmentData;
import lprog.FlightSegment;
import lprog.FlightPlanImporter;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import persistence.PersistenceContext;
import repositories.AircraftRepository;
import repositories.FlightPlanRepository;
import repositories.PilotRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImportFlightPlanController {

    private static final String FOLDER_NAME        = "flightplans";
    private static final String SUPPORTED_EXTENSION = ".fp";

    private final FlightPlanImporter   importer;
    private final FlightPlanRepository flightPlanRepo;
    private final AircraftRepository   aircraftRepo;
    private final PilotRepository      pilotRepo;

    public ImportFlightPlanController() {
        this.importer       = new FlightPlanImporter();
        this.flightPlanRepo = PersistenceContext.getInstance().flightPlanRepository();
        this.aircraftRepo   = PersistenceContext.getInstance().aircraftRepository();
        this.pilotRepo      = PersistenceContext.getInstance().pilotRepository();
    }

    // Package-private constructor for unit testing
    ImportFlightPlanController(final FlightPlanImporter importer,
                               final FlightPlanRepository flightPlanRepo) {
        this.importer       = importer;
        this.flightPlanRepo = flightPlanRepo;
        this.aircraftRepo   = null;
        this.pilotRepo      = null;
    }

    /**
     * Returns the active aircraft for the pilot's company, for selection at import time.
     *
     * @param pilotEmail authenticated pilot's email
     * @return list of active aircraft belonging to the pilot's company
     */
    public List<Aircraft> availableAircraftForPilot(final String pilotEmail) {
        final List<Aircraft> result = new ArrayList<>();
        pilotRepo.findByEmail(pilotEmail).ifPresent(pilot -> {
            final String iata = pilot.company().companyCode().iata();
            aircraftRepo.findByCompanyIata(iata).forEach(a -> {
                if (a.operationalStatus().isActive()) result.add(a);
            });
        });
        return result;
    }

    /**
     * Returns the list of available flight plan files in the flightplans/ folder.
     * Searches from the working directory upwards until the folder is found,
     * so the application works regardless of from where it is launched.
     */
    public List<File> availableFlightPlanFiles() {
        final File dir = resolveFlightPlansDir();
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        final File[] files = dir.listFiles(
                f -> f.isFile() && f.getName().endsWith(SUPPORTED_EXTENSION));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        final List<File> result = new ArrayList<>(Arrays.asList(files));
        result.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    /**
     * Imports a flight plan from a raw DSL string (used by the TCP server US86).
     * If valid, persists the flight plan with status DRAFT.
     *
     * @param dsl        raw DSL content
     * @param pilotEmail email of the authenticated pilot (may be null for system imports)
     */
    public FlightPlanImportResult importFromDsl(final String dsl, final String pilotEmail) {
        final FlightPlanImportResult result = importer.importFromString(dsl);
        if (result.isSuccess()) {
            persistFlightPlans(result.getFlights(), result.getRawSource(), null, pilotEmail);
        }
        return result;
    }

    /** Backwards-compatible overload — no pilot context (system/TCP import). */
    public FlightPlanImportResult importFromDsl(final String dsl) {
        return importFromDsl(dsl, null);
    }

    /**
     * Validates the file format and imports the flight plan.
     * Only .fp files (Core Flight DSL) are supported.
     * If valid, persists the flight plan with status DRAFT.
     *
     * @param file                  the .fp file to import
     * @param aircraftRegistration  registration of the assigned aircraft (may be null)
     * @param pilotEmail            email of the authenticated pilot (for ownership tracking)
     */
    public FlightPlanImportResult importFromFile(final File file,
                                                 final String aircraftRegistration,
                                                 final String pilotEmail) {
        if (!file.getName().endsWith(SUPPORTED_EXTENSION)) {
            return unsupportedFormatResult(file.getName());
        }
        final FlightPlanImportResult result = importer.importFromFile(file.getAbsolutePath());
        if (result.isSuccess()) {
            persistFlightPlans(result.getFlights(), result.getRawSource(),
                               aircraftRegistration, pilotEmail);
        }
        return result;
    }

    /** Backwards-compatible overload — no aircraft or pilot assigned. */
    public FlightPlanImportResult importFromFile(final File file,
                                                 final String aircraftRegistration) {
        return importFromFile(file, aircraftRegistration, null);
    }

    /** Backwards-compatible overload — no aircraft assigned. */
    public FlightPlanImportResult importFromFile(final File file) {
        return importFromFile(file, null, null);
    }

    /**
     * Resolves the flightplans/ directory by searching from the working directory
     * upwards through parent directories. This ensures the folder is found
     * regardless of where the application is launched from.
     */
    private File resolveFlightPlansDir() {
        // 1. Tentar primeiro o caminho relativo direto (funciona na maioria das IDEs)
        File candidate = new File(FOLDER_NAME);
        if (candidate.exists() && candidate.isDirectory()) return candidate;

        // 2. Tentar via System Property (User Dir) - Caminho de execução
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            File dir = new File(userDir);
            while (dir != null) {
                // Tenta /flightplans
                candidate = new File(dir, FOLDER_NAME);
                if (candidate.exists() && candidate.isDirectory()) return candidate;

                // Tenta /exemplo.core/flightplans
                candidate = new File(dir, "exemplo.core" + File.separator + FOLDER_NAME);
                if (candidate.exists() && candidate.isDirectory()) return candidate;

                dir = dir.getParentFile();
            }
        }

        // 3. Fallback: Tentar localizar através da localização do código compilado
        try {
            File jarDir = new File(ImportFlightPlanController.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
            while (jarDir != null) {
                candidate = new File(jarDir, FOLDER_NAME);
                if (candidate.exists() && candidate.isDirectory()) return candidate;

                candidate = new File(jarDir, "exemplo.core" + File.separator + FOLDER_NAME);
                if (candidate.exists() && candidate.isDirectory()) return candidate;

                jarDir = jarDir.getParentFile();
            }
        } catch (Exception ignored) {
            // Se falhar, o loop acima ou o return null resolvem
        }

        return null;
    }

    private FlightPlanImportResult unsupportedFormatResult(final String fileName) {
        final lprog.ErrorCollector collector = new lprog.ErrorCollector();
        collector.addSemanticError(0, 0,
                "Unsupported file format '" + fileName
                        + "'. Only .fp (Core Flight DSL) files are supported.");
        return FlightPlanImportResult.failure(collector.getErrors());
    }

    /** Flight IDs skipped on the last import because they already exist in the database. */
    private final List<String> lastSkippedIds = new ArrayList<>();

    /** Returns the flight IDs skipped on the last {@link #importFromFile} or {@link #importFromDsl} call. */
    public List<String> lastSkippedIds() {
        return Collections.unmodifiableList(lastSkippedIds);
    }

    private void persistFlightPlans(final List<FlightData> flights,
                                    final String rawSource,
                                    final String aircraftRegistration,
                                    final String pilotEmail) {
        lastSkippedIds.clear();
        for (final FlightData fd : flights) {
            if (flightPlanRepo.existsByFlightId(fd.id)) {
                lastSkippedIds.add(fd.id);
                continue;
            }
            final List<FlightLeg> legs = new ArrayList<>();
            for (final LegData ld : fd.legs) {
                final FlightLeg leg = new FlightLeg(
                        ld.departureAirport,
                        ld.departureDate,
                        ld.departureTime,
                        ld.arrivalAirport,
                        ld.arrivalTime,
                        ld.fuelQuantity,
                        ld.fuelUnit);
                for (final SegmentData sd : ld.segments) {
                    leg.addSegment(new FlightSegment(
                            sd.fromLat, sd.fromLon,
                            sd.toLat,   sd.toLon,
                            sd.altitudeMin,
                            sd.altitudeMax,
                            sd.phase,
                            sd.width,
                            sd.windAngle,
                            sd.windSpeed));
                }
                legs.add(leg);
            }
            final FlightPlan plan = new FlightPlan(fd.id, fd.type, legs);
            plan.setDslContent(rawSource);
            if (aircraftRegistration != null && !aircraftRegistration.isBlank()) {
                plan.setAircraftRegistration(aircraftRegistration);
            }
            if (pilotEmail != null && !pilotEmail.isBlank()) {
                plan.setPilotEmail(pilotEmail);
            }
            flightPlanRepo.save(plan);
        }
    }
}