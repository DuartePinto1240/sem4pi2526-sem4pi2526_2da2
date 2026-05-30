// ─── repositories/FlightRouteRepository.java ───────────────────────────────

package repositories;

import domain.flightroute.FlightRoute;
import domain.flightroute.RouteDesignator;
import eapli.framework.domain.repositories.DomainRepository;

public interface FlightRouteRepository
        extends DomainRepository<RouteDesignator, FlightRoute> {

    boolean existsByDesignator(String designator);

    Iterable<FlightRoute> findAll();

    Iterable<FlightRoute> findByCompanyIata(String companyIata);
}