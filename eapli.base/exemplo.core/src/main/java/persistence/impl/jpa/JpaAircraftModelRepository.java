package persistence.impl.jpa;

import domain.aircraftmodel.AircraftModel;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.AircraftModelRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of {@link AircraftModelRepository} backed by an H2 file-based
 * database.
 *
 * <p>Data persists across application restarts. The database file is located
 * at {@code ./db/alsafe} relative to the working directory.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaAircraftModelRepository
        extends JpaAutoTxRepository<AircraftModel, Long, Long>
        implements AircraftModelRepository {

    public JpaAircraftModelRepository(final String persistenceUnitName,
                                      final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    public JpaAircraftModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    @Override
    public <S extends AircraftModel> S save(final S aircraftModel) {
        return super.save(aircraftModel);
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return ofIdentity(id).isPresent();
    }

    @Override
    public Optional<AircraftModel> ofIdentity(final Long id) {
        return super.ofIdentity(id);
    }

    @Override
    public Iterable<AircraftModel> findAll() {
        return super.findAll();
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public boolean existsByNameAndManufacturer(final String name,
                                               final String manufacturer) {
        return findByNameAndManufacturer(name, manufacturer).isPresent();
    }

    @Override
    public Optional<AircraftModel> findByNameAndManufacturer(final String name,
                                                             final String manufacturer) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name",         name.toLowerCase());
        params.put("manufacturer", manufacturer.toLowerCase());
        return matchOne(
                "LOWER(e.modelName) = :name AND LOWER(e.manufacturer) = :manufacturer",
                params);
    }
}