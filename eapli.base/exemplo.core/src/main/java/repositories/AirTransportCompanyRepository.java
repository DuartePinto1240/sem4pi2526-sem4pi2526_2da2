package repositories;

import domain.AirTransportCompany;
import domain.CompanyCode;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface AirTransportCompanyRepository
        extends DomainRepository<CompanyCode, AirTransportCompany> {

    boolean existsByIata(String iata);

    boolean existsByIcao(String icao);

    Iterable<AirTransportCompany> findAll();

    Optional<AirTransportCompany> findByCollaborator(String collaboratorEmail);
}
