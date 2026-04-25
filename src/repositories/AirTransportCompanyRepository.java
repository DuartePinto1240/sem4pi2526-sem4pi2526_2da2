package repositories;

import domain.AirTransportCompany;
import domain.CompanyCode;
import eapli.framework.domain.repositories.DomainRepository;

public interface AirTransportCompanyRepository
        extends DomainRepository<CompanyCode, AirTransportCompany> {

    boolean existsByIata(String iata);

    boolean existsByIcao(String icao);

    Iterable<AirTransportCompany> findAll();
}
