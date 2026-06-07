package persistence.impl.jpa;

import domain.flightroute.FlightRoute;
import domain.flightroute.RouteDesignator;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.FlightRouteRepository;

import java.util.Map;

public class JpaFlightRouteRepository
        extends JpaAutoTxRepository<FlightRoute, RouteDesignator, RouteDesignator>
        implements FlightRouteRepository {

    public JpaFlightRouteRepository(final String persistenceUnitName,
                                    final Map<String, Object> props) {
        super(persistenceUnitName, props, "designator");
    }

    @Override
    public boolean existsByDesignator(final String designator) {
        return !createQuery(
                "SELECT r FROM FlightRoute r " +
                        "WHERE UPPER(r.designator.designator) = :d",
                FlightRoute.class)
                .setParameter("d", designator.toUpperCase().trim())
                .getResultList()
                .isEmpty();
    }

    @Override
    public Iterable<FlightRoute> findAll() {
        return createQuery(
                "SELECT r FROM FlightRoute r ORDER BY r.designator.designator",
                FlightRoute.class)
                .getResultList();
    }

    @Override
    public Iterable<FlightRoute> findByCompanyIata(final String companyIata) {
        return createQuery(
                "SELECT r FROM FlightRoute r " +
                        "WHERE UPPER(r.companyIata) = :iata",
                FlightRoute.class)
                .setParameter("iata", companyIata.toUpperCase().trim())
                .getResultList();
    }
}