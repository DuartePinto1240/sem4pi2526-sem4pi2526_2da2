package persistence.impl.jpa;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.EngineModelRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of {@link EngineModelRepository} backed by an H2 file-based
 * database.
 *
 * <p>Data persists across application restarts. The database file is located
 * at {@code ./db/alsafe} relative to the working directory.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaEngineModelRepository
        extends JpaAutoTxRepository<EngineModel, Long, Long>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final String persistenceUnitName,
                                    final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    public JpaEngineModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    @Override
    public <S extends EngineModel> S save(final S engineModel) {
        return super.save(engineModel);
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return ofIdentity(id).isPresent();
    }

    @Override
    public Optional<EngineModel> ofIdentity(final Long id) {
        return super.ofIdentity(id);
    }

    @Override
    public Iterable<EngineModel> findAll() {
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
    public Optional<EngineModel> findByNameAndManufacturer(final String name,
                                                           final String manufacturer) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name",         name.toLowerCase());
        params.put("manufacturer", manufacturer.toLowerCase());
        return matchOne(
                "LOWER(e.name) = :name AND LOWER(e.manufacturer) = :manufacturer",
                params);
    }

    @Override
    public Iterable<EngineModel> findByMotorizationType(final MotorizationType type) {
        final Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        return match("e.motorizationType = :type", params);
    }
}