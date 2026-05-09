package controller;

import domain.aircraft.Aircraft;
import persistence.PersistenceContext;
import repositories.AircraftRepository;

import java.util.Optional;

public class DecommissionAircraftController {

    private final AircraftRepository aircraftRepo;

    public DecommissionAircraftController() {
        this.aircraftRepo = PersistenceContext.getInstance().aircraftRepository();
    }

    // package-private para testes
    DecommissionAircraftController(final AircraftRepository aircraftRepo) {
        this.aircraftRepo = aircraftRepo;
    }

    public Iterable<Aircraft> activeAircraftOfCompany(final String companyIata) {
        final java.util.List<Aircraft> active = new java.util.ArrayList<>();
        for (final Aircraft a : aircraftRepo.findByCompanyIata(companyIata)) {
            if (a.operationalStatus().isActive()) {
                active.add(a);
            }
        }
        return active;
    }

    /**

     * A verificação de "pending flights" não é feita aqui porque o domínio
     * FlightPlan (US080) ainda não está implementado. Quando o for, deve-se
     * adicionar hasPendingFlights ao AircraftRepository.
     */
    public Aircraft decommissionAircraft(final String registration,
                                         final String companyIata) {
        final Optional<Aircraft> opt = aircraftRepo.ofIdentity(registration);

        if (opt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aircraft with registration '" + registration + "' not found.");
        }

        final Aircraft aircraft = opt.get();

        if (!aircraft.companyIata().equalsIgnoreCase(companyIata.trim())) {
            throw new IllegalArgumentException(
                    "Aircraft '" + registration
                            + "' does not belong to company '" + companyIata + "'.");
        }

        aircraft.decommission(); // o aggregate lança IllegalArgumentException se já decommissioned
        return aircraftRepo.save(aircraft);
    }
}
