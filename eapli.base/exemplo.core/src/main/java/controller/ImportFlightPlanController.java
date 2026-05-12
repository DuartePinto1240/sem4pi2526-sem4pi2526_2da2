package controller;

import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanBuilderListener.FlightData;
import lprog.FlightPlanBuilderListener.LegData;
import lprog.FlightPlanImporter;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import persistence.PersistenceContext;
import repositories.FlightPlanRepository;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImportFlightPlanController {

    private static final String FOLDER_NAME        = "flightplans";
    private static final String SUPPORTED_EXTENSION = ".fp";

    private final FlightPlanImporter   importer;
    private final FlightPlanRepository flightPlanRepo;

    public ImportFlightPlanController() {
        this.importer       = new FlightPlanImporter();
        this.flightPlanRepo = PersistenceContext.getInstance().flightPlanRepository();
    }

    // Package-private constructor for unit testing
    ImportFlightPlanController(final FlightPlanImporter importer,
                               final FlightPlanRepository flightPlanRepo) {
        this.importer       = importer;
        this.flightPlanRepo = flightPlanRepo;
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
     * Validates the file format and imports the flight plan.
     * Only .fp files (Core Flight DSL) are supported.
     * If valid, persists the flight plan with status DRAFT.
     */
    public FlightPlanImportResult importFromFile(final File file) {
        if (!file.getName().endsWith(SUPPORTED_EXTENSION)) {
            return unsupportedFormatResult(file.getName());
        }
        final FlightPlanImportResult result = importer.importFromFile(file.getAbsolutePath());
        if (result.isSuccess()) {
            persistFlightPlans(result.getFlights());
        }
        return result;
    }

    /**
     * Resolves the flightplans/ directory by searching from the working directory
     * upwards through parent directories. This ensures the folder is found
     * regardless of where the application is launched from.
     */
    private File resolveFlightPlansDir() {
        // 1. Try working directory directly
        File candidate = new File(FOLDER_NAME);
        if (candidate.exists()) return candidate;

        // 2. Try exemplo.core/flightplans relative to working directory
        candidate = new File("exemplo.core" + File.separator + FOLDER_NAME);
        if (candidate.exists()) return candidate;

        // 3. Walk up from working directory looking for the folder
        File dir = new File(".").getAbsoluteFile();
        while (dir != null) {
            candidate = new File(dir, FOLDER_NAME);
            if (candidate.exists() && candidate.isDirectory()) return candidate;
            candidate = new File(dir, "exemplo.core" + File.separator + FOLDER_NAME);
            if (candidate.exists() && candidate.isDirectory()) return candidate;
            dir = dir.getParentFile();
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

    private void persistFlightPlans(final List<FlightData> flights) {
        for (final FlightData fd : flights) {
            if (flightPlanRepo.existsByFlightId(fd.id)) {
                continue;
            }
            final List<FlightLeg> legs = new ArrayList<>();
            for (final LegData ld : fd.legs) {
                legs.add(new FlightLeg(
                        ld.departureAirport,
                        ld.departureDate,
                        ld.departureTime,
                        ld.arrivalAirport,
                        ld.arrivalTime,
                        ld.fuelQuantity,
                        ld.fuelUnit));
            }
            flightPlanRepo.save(new FlightPlan(fd.id, fd.type, legs));
        }
    }
}