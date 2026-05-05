package repositories;

import domain.Airport;
import domain.IATACode;
import domain.ICAOCode;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link Airport} aggregate roots (US052).
 *
 * <p>Extends the EAPLI Framework's {@link DomainRepository} which already
 * provides {@code save}, {@code ofIdentity}, {@code containsOfIdentity},
 * {@code delete}, {@code deleteOfIdentity}, and {@code count}.</p>
 */
public interface AirportRepository extends DomainRepository<ICAOCode, Airport> {

    /**
     * Returns all registered airports.
     *
     * @return iterable of all {@link Airport} records
     */
    Iterable<Airport> findAll();

    /**
     * Checks whether an airport with the given IATA code already exists.
     *
     * <p>IATA codes must be unique system-wide (AC US052.3). This method
     * supports the uniqueness check in {@code CreateAirportController}.</p>
     *
     * @param iataCode the IATA code to check
     * @return {@code true} if an airport with that IATA code already exists
     */
    boolean existsByIATACode(IATACode iataCode);
}
