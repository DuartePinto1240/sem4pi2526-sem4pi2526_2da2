package controller;

import domain.AirTransportCompany;
import domain.CompanyCode;
import domain.pilot.Pilot;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;
import repositories.PilotRepository;

import java.util.Collections;

public class ListPilotRosterController {

    private final PilotRepository               pilotRepository;
    private final AirTransportCompanyRepository companyRepository;

    public ListPilotRosterController() {
        this.pilotRepository   = PersistenceContext.getInstance().pilotRepository();
        this.companyRepository = PersistenceContext.getInstance().airTransportCompanyRepository();
    }

    // Package-private constructor for unit testing
    ListPilotRosterController(final PilotRepository pilotRepository,
                              final AirTransportCompanyRepository companyRepository) {
        this.pilotRepository   = pilotRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Returns all active pilots belonging to the company identified by the given codes.
     * Returns an empty list if the company is not found.
     */
    public Iterable<Pilot> activePilotsOfMyCompany(final String iata, final String icao) {
        return companyRepository.ofIdentity(new CompanyCode(iata, icao))
                .map(pilotRepository::findActiveByCompany)
                .orElse(Collections.emptyList());
    }
}