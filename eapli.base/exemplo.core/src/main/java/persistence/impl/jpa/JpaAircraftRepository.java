package persistence.impl.jpa;

import domain.aircraft.Aircraft;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.AircraftRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of {@link AircraftRepository} backed by an H2 file-based
 * database.
 *
 * <p>Data persists across application restarts. The database file is located
 * at {@code ./db/alsafe} relative to the working directory.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaAircraftRepository
        extends JpaAutoTxRepository<Aircraft, String, String>
        implements AircraftRepository {

    public JpaAircraftRepository(final String persistenceUnitName,
                                 final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "registration");
    }

    public JpaAircraftRepository(final TransactionalContext autoTx) {
        super(autoTx, "registration");
    }

    @Override
    public <S extends Aircraft> S save(final S aircraft) {
        return super.save(aircraft);
    }

    @Override
    public boolean containsOfIdentity(final String registration) {
        return ofIdentity(registration).isPresent();
    }

    @Override
    public Optional<Aircraft> ofIdentity(final String registration) {
        return super.ofIdentity(registration);
    }

    @Override
    public Iterable<Aircraft> findAll() {
        return super.findAll();
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public boolean existsByRegistration(final String registration) {
        return ofIdentity(registration).isPresent();
    }

    @Override
    public Iterable<Aircraft> findByCompanyIata(final String companyIata) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyIata", companyIata.toUpperCase());
        return match("e.companyIata = :companyIata", params);
    }

    @Override
    public Iterable<Aircraft> findByCompanyIataAndModelId(final String companyIata,
                                                          final Long modelId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyIata",    companyIata.toUpperCase());
        params.put("aircraftModelId", modelId);
        return match("e.companyIata = :companyIata AND e.aircraftModelId = :aircraftModelId",
                params);
    }

    @Override
    public Iterable<Aircraft> findByCompanyIataAndManufacturer(final String companyIata,
                                                               final String manufacturer) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyIata",    companyIata.toUpperCase());
        params.put("manufacturer",   manufacturer.toLowerCase());
        return match("e.companyIata = :companyIata"
                + " AND LOWER(e.manufacturer) = :manufacturer", params);
    }
}