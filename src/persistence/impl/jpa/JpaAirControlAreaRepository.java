package persistence.impl.jpa;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.GeographicBoundary;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.AirControlAreaRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA implementation of {@link AirControlAreaRepository} backed by H2 (US050).
 *
 * <p>Extends {@link JpaAutoTxRepository} — the same base class used by
 * {@link JpaUserRepository} in this project — which manages its own
 * transactions per operation (no external transaction context needed
 * for simple CRUD).</p>
 *
 * <p>Data persists across restarts in the H2 file at {@code ./db/alsafe}.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaAirControlAreaRepository
        extends JpaAutoTxRepository<AirControlArea, AirControlAreaCode, AirControlAreaCode>
        implements AirControlAreaRepository {

    /**
     * Standalone constructor — manages its own transactions.
     *
     * @param persistenceUnitName the JPA persistence unit name
     * @param extendedProperties  additional JPA properties
     */
    public JpaAirControlAreaRepository(final String persistenceUnitName,
                                        final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "areaCode");
    }

    /**
     * Shared-transaction constructor.
     *
     * @param autoTx the shared transactional context
     */
    public JpaAirControlAreaRepository(final TransactionalContext autoTx) {
        super(autoTx, "areaCode");
    }

    @Override
    public Iterable<AirControlArea> findAll() {
        return match("1=1");
    }

    @Override
    public boolean existsByName(final String name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name.trim());
        return matchOne("LOWER(e.name) = LOWER(:name)", params).isPresent();
    }

    @Override
    public boolean existsOverlappingBoundary(final GeographicBoundary boundary) {
        for (final AirControlArea area : findAll()) {
            if (area.boundary().overlapsWith(boundary)) {
                return true;
            }
        }
        return false;
    }
}
