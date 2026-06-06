package controller;

import domain.AirTransportCompany;
import dto.PilotDTO;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;
import repositories.PilotRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
     * Returns all registered air transport companies for selection.
     */
    public Iterable<AirTransportCompany> availableCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Returns DTOs for all active pilots belonging to the given company.
     */
    public List<PilotDTO> activePilotsOfMyCompany(final AirTransportCompany company) {
        return StreamSupport
                .stream(pilotRepository.findActiveByCompany(company).spliterator(), false)
                .map(PilotDTO::from)
                .collect(Collectors.toList());
    }
}