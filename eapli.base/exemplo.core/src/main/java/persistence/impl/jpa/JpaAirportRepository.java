package persistence.impl.jpa;

import domain.Airport;
import domain.IATACode;
import domain.ICAOCode;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.AirportRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA implementation of {@link AirportRepository} backed by H2 (US052).
 *
 * <p>Extends {@link JpaAutoTxRepository} — the same base class used throughout
 * this project — which manages its own transactions per operation.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaAirportRepository
        extends JpaAutoTxRepository<Airport, ICAOCode, ICAOCode>
        implements AirportRepository {

    /**
     * Standalone constructor — manages its own transactions.
     *
     * @param persistenceUnitName the JPA persistence unit name
     * @param extendedProperties  additional JPA properties
     */
    public JpaAirportRepository(final String persistenceUnitName,
                                 final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "icaoCode");
    }

    /**
     * Shared-transaction constructor.
     *
     * @param autoTx the shared transactional context
     */
    public JpaAirportRepository(final TransactionalContext autoTx) {
        super(autoTx, "icaoCode");
    }

    @Override
    public Iterable<Airport> findAll() {
        return match("1=1");
    }

    @Override
    public boolean existsByIATACode(final IATACode iataCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iata", iataCode.code());
        return matchOne("e.iataCode.iata = :iata", params).isPresent();
    }
}
