package controller;

import domain.AirTransportCompany;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;

public class RegisterAirTransportCompanyController {

    private final AirTransportCompanyRepository repo;

    public RegisterAirTransportCompanyController() {
        this.repo = PersistenceContext.getInstance().airTransportCompanyRepository();
    }

    public AirTransportCompany registerCompany(final String name,
                                               final String iata,
                                               final String icao) {
        if (repo.existsByIata(iata)) {
            throw new IllegalArgumentException(
                    "A company with IATA code '" + iata + "' already exists.");
        }

        if (repo.existsByIcao(icao)) {
            throw new IllegalArgumentException(
                    "A company with ICAO code '" + icao + "' already exists.");
        }

        final AirTransportCompany company =
                new AirTransportCompany(name, iata, icao);

        return repo.save(company);
    }
}