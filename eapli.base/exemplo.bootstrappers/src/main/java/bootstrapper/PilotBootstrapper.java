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
            register(ctrl, "Joao Silva",   "joao.silva@tap.pt",   "+351910000001", tap, models.subList(0, 1));
            register(ctrl, "Maria Santos", "maria.santos@tap.pt", "+351910000002", tap, models.subList(0, 1));
            // Demo pilot account — same email as the PILOT system user for US080 auto-association
            register(ctrl, "AlSafe Pilot", "pilot@alsafe.com",    "+351910000099", tap, models.subList(0, 1));
        });

        findCompany(companies, "FR").ifPresent(ryr ->
                register(ctrl, "James Murphy", "james.murphy@ryanair.com", "+353870000001", ryr, models.subList(0, 1))
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
                          final AirTransportCompany company,
                          final List<AircraftModel> models) {
        ctrl.addPilot(name, email, phone, company, models)
                .ifLeft(error -> { /* already exists or validation failed — skip silently */ });
    }
}