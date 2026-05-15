package persistence.impl.jpa;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.AirTransportCompany;
import domain.CompanyCode;
import dto.CustomerDTO;
import repositories.CustomerRepository;

import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of {@link CustomerRepository}.
 *
 * Delegates to the existing JpaAirTransportCompanyRepository and
 * JpaAirControlAreaRepository — it composes them rather than extending
 * JpaAutoTxRepository, because it manages two different entity types.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaCustomerRepository implements CustomerRepository {

    private final JpaAirTransportCompanyRepository companyRepo;
    private final JpaAirControlAreaRepository      areaRepo;

    public JpaCustomerRepository(final String persistenceUnitName,
                                 final Map extendedProperties) {
        this.companyRepo = new JpaAirTransportCompanyRepository(persistenceUnitName, extendedProperties);
        this.areaRepo    = new JpaAirControlAreaRepository(persistenceUnitName, extendedProperties);
    }

    @Override
    public Iterable<AirTransportCompany> findAllAirTransportCompanies() {
        return companyRepo.findAll();
    }

    @Override
    public Iterable<AirControlArea> findAllAirControlAreas() {
        return areaRepo.findAll();
    }

    @Override
    public Optional<Object> findById(final String customerId,
                                     final CustomerDTO.CustomerType customerType) {
        return switch (customerType) {
            case AIR_TRANSPORT_COMPANY -> {
                final String[] parts = customerId.split("/");
                yield companyRepo.ofIdentity(new CompanyCode(parts[0], parts[1]))
                        .map(c -> (Object) c);
            }
            case AIR_CONTROL_AREA -> areaRepo.ofIdentity(new AirControlAreaCode(customerId))
                    .map(a -> (Object) a);
            default -> Optional.empty();
        };
    }

    @Override
    public AirTransportCompany save(final AirTransportCompany company) {
        return companyRepo.save(company);
    }

    @Override
    public AirControlArea save(final AirControlArea area) {
        return areaRepo.save(area);
    }
}