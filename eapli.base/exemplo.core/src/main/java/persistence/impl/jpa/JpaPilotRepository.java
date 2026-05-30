package persistence.impl.jpa;

import domain.AirTransportCompany;
import domain.pilot.Pilot;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.PilotRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, Long, Long>
        implements PilotRepository {

    public JpaPilotRepository(final String persistenceUnitName,
                              final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    @Override
    public Iterable<Pilot> findActiveByCompany(final AirTransportCompany company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iata", company.companyCode().iata());
        params.put("icao", company.companyCode().icao());
        return match(
                "e.company.companyCode.iata = :iata " +
                        "AND e.company.companyCode.icao = :icao " +
                        "AND e.active = true",
                params);
    }

    @Override
    public Optional<Pilot> findByEmail(final String email) {
        final Map<String, Object> params = new HashMap<>();
        params.put("email", email.trim().toLowerCase());
        return matchOne("e.email = :email", params);
    }

    @Override
    public Iterable<Pilot> findAllByCompany(final AirTransportCompany company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iata", company.companyCode().iata());
        params.put("icao", company.companyCode().icao());
        return match(
                "e.company.companyCode.iata = :iata " +
                        "AND e.company.companyCode.icao = :icao",
                params);
    }

    @Override
    public <S extends Pilot> S save(final S pilot) {
        return super.save(pilot);
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return ofIdentity(id).isPresent();
    }

    @Override
    public Optional<Pilot> ofIdentity(final Long id) {
        return super.ofIdentity(id);
    }

    @Override
    public long count() {
        return super.count();
    }
}