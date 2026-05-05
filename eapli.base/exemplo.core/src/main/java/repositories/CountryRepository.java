package repositories;

import domain.Country;

import java.util.Optional;

/**
 * Repository interface for {@link Country} (bootstrap data).
 *
 * <p>Country is a Value Object, not an Aggregate Root, so this interface
 * does NOT extend {@code DomainRepository} — that interface requires the
 * entity type to implement {@code AggregateRoot}, which Country must not.</p>
 *
 * <p>Countries are pre-loaded at system startup (bootstrap) and are
 * read-only during normal operation. The operator selects one when
 * registering an airport.</p>
 */
public interface CountryRepository {

    /**
     * Persists a country (used by bootstrap).
     *
     * @param country the country to save
     * @return the saved country
     */
    Country save(Country country);

    /**
     * Returns all available countries for display in the UI selection list.
     *
     * @return iterable of all registered {@link Country} records
     */
    Iterable<Country> findAll();

    /**
     * Finds a country by its exact name (case-insensitive).
     *
     * @param name the country name
     * @return an {@link Optional} containing the country if found
     */
    Optional<Country> findByName(String name);

    /**
     * Checks whether a country with the given name exists.
     *
     * @param name the country name (case-insensitive)
     * @return {@code true} if the country exists
     */
    boolean existsByName(String name);

    /**
     * Returns the total number of registered countries.
     *
     * @return country count
     */
    long count();
}