package persistence.impl.inmemory;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.AirTransportCompany;
import domain.CompanyCode;
import dto.CustomerDTO;
import repositories.CustomerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of {@link CustomerRepository} (US061–US064).
 *
 * Holds both Air Transport Companies and Air Control Areas in the same
 * repository, as shown in the sequence diagrams where a single
 * CustomerRepository participant responds to both findAllAirTransportCompanies()
 * and findAllAirControlAreas() calls.
 *
 * save() uses upsert semantics on both collections.
 */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final List<AirTransportCompany> companies = new ArrayList<>();
    private final List<AirControlArea>      areas     = new ArrayList<>();

    @Override
    public Iterable<AirTransportCompany> findAllAirTransportCompanies() {
        return new ArrayList<>(companies);
    }

    @Override
    public Iterable<AirControlArea> findAllAirControlAreas() {
        return new ArrayList<>(areas);
    }

    /**
     * Routes to the correct internal collection based on customerType.
     * For AIR_TRANSPORT_COMPANY: id format is "IATA/ICAO" (e.g. "TP/TAP").
     * For AIR_CONTROL_AREA:      id is the area code string (e.g. "LPPC").
     */
    @Override
    public Optional<Object> findById(final String customerId,
                                     final CustomerDTO.CustomerType customerType) {
        switch (customerType) {
            case AIR_TRANSPORT_COMPANY: {
                final String[] parts = customerId.split("/");
                final CompanyCode code = new CompanyCode(parts[0], parts[1]);
                return companies.stream()
                        .filter(c -> c.identity().equals(code))
                        .map(c -> (Object) c)
                        .findFirst();
            }
            case AIR_CONTROL_AREA: {
                final AirControlAreaCode code = new AirControlAreaCode(customerId);
                return areas.stream()
                        .filter(a -> a.identity().equals(code))
                        .map(a -> (Object) a)
                        .findFirst();
            }
            default:
                return Optional.empty();
        }
    }

    @Override
    public AirTransportCompany save(final AirTransportCompany company) {
        companies.removeIf(c -> c.identity().equals(company.identity()));
        companies.add(company);
        return company;
    }

    @Override
    public AirControlArea save(final AirControlArea area) {
        areas.removeIf(a -> a.identity().equals(area.identity()));
        areas.add(area);
        return area;
    }
}