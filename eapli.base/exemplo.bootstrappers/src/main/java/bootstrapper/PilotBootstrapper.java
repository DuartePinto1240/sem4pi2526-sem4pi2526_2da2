package bootstrapper;

import controller.AddPilotController;
import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import eapli.framework.actions.Action;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bootstrap action that pre-loads a set of pilots.
 *
 * Executed once at application startup. Pilots that already exist
 * (same email) are silently skipped.
 *
 * Assumes AircraftBootstrapper and AirTransportCompanyBootstrapper
 * have already run (Bootstrap.java execution order).
 */
public class PilotBootstrapper implements Action {

    @Override
    public boolean execute() {
        final AddPilotController ctrl = new AddPilotController();
        final AirTransportCompanyRepository companyRepo =
                PersistenceContext.getInstance().airTransportCompanyRepository();

        final List<AircraftModel> models = new ArrayList<>();
        PersistenceContext.getInstance()
                .aircraftModelRepository()
                .findAll()
                .forEach(models::add);

        if (models.isEmpty()) return true; // no models yet — skip

        final List<AirTransportCompany> companies = new ArrayList<>();
        companyRepo.findAll().forEach(companies::add);

        findCompany(companies, "TP").ifPresent(tap -> {
            register(ctrl, "João Silva",   "joao.silva@tap.pt",   "910000001", "STAFF-P-001", "Pilot1234", tap, models.subList(0, 1));
            register(ctrl, "Maria Santos", "maria.santos@tap.pt", "910000002", "STAFF-P-002", "Pilot1234", tap, models.subList(0, 1));
        });

        findCompany(companies, "FR").ifPresent(ryr ->
                register(ctrl, "James Murphy", "james.murphy@ryanair.com", "910000003", "STAFF-P-003", "Pilot1234", ryr, models.subList(0, 1))
        );

        return true;
    }

    private Optional<AirTransportCompany> findCompany(final List<AirTransportCompany> companies,
                                                      final String iata) {
        return companies.stream()
                .filter(c -> c.companyCode().iata().equalsIgnoreCase(iata))
                .findFirst();
    }

    private void register(final AddPilotController ctrl,
                          final String name,
                          final String email,
                          final String phone,
                          final String staffNumber,
                          final String password,
                          final AirTransportCompany company,
                          final List<AircraftModel> models) {
        ctrl.addPilot(name, email, phone, staffNumber, password, company, models)
                .ifLeft(error -> { /* already exists or validation failed — skip silently */ });
    }
}