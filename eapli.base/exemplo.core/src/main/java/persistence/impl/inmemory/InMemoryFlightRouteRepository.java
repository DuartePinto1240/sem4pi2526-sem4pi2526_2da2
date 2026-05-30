package persistence.impl.inmemory;

import domain.flightroute.FlightRoute;
import domain.flightroute.RouteDesignator;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import repositories.FlightRouteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryFlightRouteRepository
        extends InMemoryDomainRepository<FlightRoute, RouteDesignator>
        implements FlightRouteRepository {

    @Override
    public boolean existsByDesignator(final String designator) {
        for (final FlightRoute r : findAll()) {
            if (r.identity().value().equalsIgnoreCase(designator.toUpperCase().trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<FlightRoute> findAll() {
        return super.findAll();
    }

    @Override
    public Iterable<FlightRoute> findByCompanyIata(final String companyIata) {
        final List<FlightRoute> result = new ArrayList<>();
        for (final FlightRoute r : findAll()) {
            if (r.companyIata().equalsIgnoreCase(companyIata)) {
                result.add(r);
            }
        }
        return result;
    }
}