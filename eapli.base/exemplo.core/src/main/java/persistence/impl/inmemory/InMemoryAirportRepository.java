package persistence.impl.inmemory;

import domain.Airport;
import domain.IATACode;
import domain.ICAOCode;
import repositories.AirportRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of {@link AirportRepository} (US052).
 *
 * <p>Used during development and testing. Data does not persist across
 * application restarts. For persistent storage use
 * {@link persistence.impl.jpa.JpaAirportRepository} instead.</p>
 */
public class InMemoryAirportRepository implements AirportRepository {

    private final List<Airport> airports = new ArrayList<>();

    @Override
    public Iterable<Airport> findAll() {
        return new ArrayList<>(airports);
    }

    @Override
    public boolean existsByIATACode(final IATACode iataCode) {
        return airports.stream()
                .anyMatch(a -> a.iataCode().equals(iataCode));
    }

    @Override
    public Airport save(final Airport airport) {
        airports.removeIf(a -> a.identity().equals(airport.identity()));
        airports.add(airport);
        return airport;
    }

    @Override
    public Optional<Airport> ofIdentity(final ICAOCode icaoCode) {
        return airports.stream()
                .filter(a -> a.identity().equals(icaoCode))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final ICAOCode icaoCode) {
        return airports.stream()
                .anyMatch(a -> a.identity().equals(icaoCode));
    }

    @Override
    public void delete(final Airport airport) {
        airports.remove(airport);
    }

    @Override
    public void deleteOfIdentity(final ICAOCode icaoCode) {
        airports.removeIf(a -> a.identity().equals(icaoCode));
    }

    @Override
    public long count() {
        return airports.size();
    }
}
