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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImportFlightPlanController {

    private static final String FLIGHT_PLANS_DIR    = "exemplo.core/flightplans";
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
     * Returns the list of available flight plan files in the flightplans/ folder,
     * sorted alphabetically.
     */
    public List<File> availableFlightPlanFiles() {
        final File dir = new File(FLIGHT_PLANS_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
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