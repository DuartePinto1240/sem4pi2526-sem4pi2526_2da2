package persistence.impl.inmemory;

import domain.AirTransportCompany;
import domain.CompanyCode;
import repositories.AirTransportCompanyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAirTransportCompanyRepository
        implements AirTransportCompanyRepository {

    private final List<AirTransportCompany> companies = new ArrayList<>();

    @Override
    public boolean existsByIata(final String iata) {
        return companies.stream()
                .anyMatch(c -> c.companyCode().iata().equalsIgnoreCase(iata));
    }

    @Override
    public boolean existsByIcao(final String icao) {
        return companies.stream()
                .anyMatch(c -> c.companyCode().icao().equalsIgnoreCase(icao));
    }

    @Override
    public Iterable<AirTransportCompany> findAll() {
        return new ArrayList<>(companies);
    }

    @Override
    public Optional<AirTransportCompany> findByCollaborator(String collaboratorEmail) {
            return companies.stream()
                    .filter(c -> c.findCollaboratorByEmail(collaboratorEmail).isPresent())
                    .findFirst();

        }

    @Override
    public AirTransportCompany save(final AirTransportCompany company) {
        companies.add(company);
        return company;
    }

    @Override
    public Optional<AirTransportCompany> ofIdentity(final CompanyCode id) {
        return companies.stream()
                .filter(c -> c.identity().equals(id))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final CompanyCode id) {
        return companies.stream()
                .anyMatch(c -> c.identity().equals(id));
    }

    @Override
    public void delete(final AirTransportCompany company) {
        companies.remove(company);
    }

    @Override
    public void deleteOfIdentity(final CompanyCode id) {
        companies.removeIf(c -> c.identity().equals(id));
    }

    @Override
    public long count() {
        return companies.size();
    }
}
