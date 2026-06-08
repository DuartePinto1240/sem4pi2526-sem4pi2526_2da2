package controller;

import domain.AirTransportCompany;
import domain.pilot.Pilot;
import dto.PilotDTO;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;
import repositories.FlightPlanRepository;
import repositories.PilotRepository;

import java.util.ArrayList;
import java.util.List;

public class RemovePilotController {

    private final AirTransportCompanyRepository atcRepo;
    private final PilotRepository               pilotRepo;
    private final FlightPlanRepository          flightPlanRepo;

    public RemovePilotController() {
        this.atcRepo        = PersistenceContext.getInstance().airTransportCompanyRepository();
        this.pilotRepo      = PersistenceContext.getInstance().pilotRepository();
        this.flightPlanRepo = PersistenceContext.getInstance().flightPlanRepository();
    }

    // Package-private constructor for unit testing
    RemovePilotController(final AirTransportCompanyRepository atcRepo,
                          final PilotRepository pilotRepo,
                          final FlightPlanRepository flightPlanRepo) {
        this.atcRepo        = atcRepo;
        this.pilotRepo      = pilotRepo;
        this.flightPlanRepo = flightPlanRepo;
    }

    public List<PilotDTO> getActivePilots (String collaboratorEmail) {
        final AirTransportCompany company = findByCollaborator(collaboratorEmail);
        List<PilotDTO> activePilots = new ArrayList<>();
        for (Pilot fr : listActivePilots(company)) {
            activePilots.add(PilotDTO.from(fr));
        }
        return activePilots;
    }

    private AirTransportCompany findByCollaborator (String collaboratorEmail) {
        return atcRepo.findByCollaborator(collaboratorEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No company found for collaborator " + collaboratorEmail));
    }

    private List<Pilot> listActivePilots (AirTransportCompany airTransportCompany) {
        final List<Pilot> pilots = new ArrayList<>();
        pilotRepo.findActiveByCompany(airTransportCompany).forEach(pilots::add);
        return pilots;
    }

    public void removePilot (String pilotEmail) {
        final Pilot pilot = findPilotByEmail(pilotEmail);

        if (existsAssignedFlightPlanForPilot(pilotEmail)) {
            throw new IllegalArgumentException(
                    "Cannot deactivate pilot '" + pilotEmail
                            + "': has assigned flight plans.");
        }

        deactivatePilot(pilot);
    }

    private Pilot findPilotByEmail(final String pilotEmail) {
        return pilotRepo.findByEmail(pilotEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pilot not found: " + pilotEmail));
    }

    private boolean existsAssignedFlightPlanForPilot (String pilotEmail) {
        return flightPlanRepo.existsAssignedFlightPlansForPilot(pilotEmail);
    }

    private void deactivatePilot (Pilot pilot) {
        pilot.deactivate();
        pilotRepo.save(pilot);
    }


}