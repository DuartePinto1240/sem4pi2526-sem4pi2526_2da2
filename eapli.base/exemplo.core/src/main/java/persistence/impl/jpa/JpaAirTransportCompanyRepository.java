package persistence.impl.jpa;

import domain.AirTransportCompany;
import domain.CompanyCode;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.AirTransportCompanyRepository;

import java.util.Map;



public class JpaAirTransportCompanyRepository
        extends JpaAutoTxRepository<AirTransportCompany, CompanyCode, CompanyCode>
        implements AirTransportCompanyRepository {

    public JpaAirTransportCompanyRepository(final String persistenceUnitName,
                                            final Map<String, Object> props) {
        super(persistenceUnitName, props, "companyCode");
    }

    @Override
    public boolean existsByIata(final String iata) {
        return !createQuery(
                "SELECT a FROM AirTransportCompany a " +
                        "WHERE a.companyCode.iata = :iata",
                AirTransportCompany.class)
                .setParameter("iata", iata.toUpperCase().trim())
                .getResultList()
                .isEmpty();
    }

    @Override
    public boolean existsByIcao(final String icao) {
        return !createQuery(
                "SELECT a FROM AirTransportCompany a " +
                        "WHERE a.companyCode.icao = :icao",
                AirTransportCompany.class)
                .setParameter("icao", icao.toUpperCase().trim())
                .getResultList()
                .isEmpty();
    }

    @Override
    public Iterable<AirTransportCompany> findAll() {
        return createQuery(
                "SELECT a FROM AirTransportCompany a ORDER BY a.name",
                AirTransportCompany.class)
                .getResultList();
    }
}