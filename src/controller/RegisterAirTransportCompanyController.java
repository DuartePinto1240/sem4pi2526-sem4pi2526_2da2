package controller;

import domain.AirTransportCompany;
import repositories.AirTransportCompanyRepository;
import persistence.impl.jpa.JpaAirTransportCompanyRepository;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import domain.BaseRoles;

@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AuthorizationService authz;
    private AirTransportCompanyRepository repository = null;

    private AirTransportCompanyRepository repository() {
        if (this.repository == null) {
            this.repository = new JpaAirTransportCompanyRepository(
                    PersistenceContext.repositories().transactionalContext());
        }
        return this.repository;
    }

    public RegisterAirTransportCompanyController() {
        this.authz = AuthzRegistry.authorizationService();
    }

    public AirTransportCompany registerCompany(final String name,
                                               final String iata,
                                               final String icao) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        if (repository().existsByIata(iata)) {
            throw new IllegalArgumentException(
                    "A company with IATA code '" + iata + "' already exists.");
        }

        if (repository().existsByIcao(icao)) {
            throw new IllegalArgumentException(
                    "A company with ICAO code '" + icao + "' already exists.");
        }

        final AirTransportCompany company =
                new AirTransportCompany(name, iata, icao);

        return repository().save(company);
    }
}