package persistence.impl.inmemory;

import domain.AirTransportCompany;
import domain.pilot.Pilot;
import repositories.PilotRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryPilotRepository implements PilotRepository {

    private final List<Pilot> store = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public Iterable<Pilot> findActiveByCompany(final AirTransportCompany company) {
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : store) {
            if (p.isActive() && p.company().sameAs(company)) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public Optional<Pilot> findByEmail(final String email) {
        if (email == null) return Optional.empty();
        final String normalized = email.trim().toLowerCase();
        for (final Pilot p : store) {
            if (p.email().equals(normalized)) return Optional.of(p);
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Pilot> findAllByCompany(final AirTransportCompany company) {
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : store) {
            if (p.company().sameAs(company)) result.add(p);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Pilot> S save(final S pilot) {
        // simulate auto-generated id
        final Optional<Pilot> existing = findByEmail(pilot.email());
        if (existing.isPresent()) {
            store.remove(existing.get());
        }
        store.add(pilot);
        return pilot;
    }

    @Override
    public Optional<Pilot> ofIdentity(final Long id) {
        for (final Pilot p : store) {
            if (id.equals(p.identity())) return Optional.of(p);
        }
        return Optional.empty();
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return ofIdentity(id).isPresent();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public Iterable<Pilot> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public void delete(final Pilot pilot) {
        store.remove(pilot);
    }

    @Override
    public void deleteOfIdentity(final Long id) {
        ofIdentity(id).ifPresent(store::remove);
    }
}