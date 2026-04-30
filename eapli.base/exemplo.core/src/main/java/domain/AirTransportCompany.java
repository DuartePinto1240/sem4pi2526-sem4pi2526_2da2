package domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "AIR_TRANSPORT_COMPANY")
public class AirTransportCompany implements AggregateRoot<CompanyCode> {

    @EmbeddedId
    private CompanyCode companyCode;

    private String name;

    public AirTransportCompany(final String name,
                               final String iata,
                               final String icao) {
        Preconditions.nonEmpty(name, "Company name cannot be empty");
        this.name = name;
        this.companyCode = new CompanyCode(iata, icao);
    }

    @Override
    public CompanyCode identity() {
        return companyCode;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AirTransportCompany)) return false;
        final AirTransportCompany that = (AirTransportCompany) other;
        return this.companyCode.equals(that.companyCode);
    }

    public String name() {
        return name;
    }

    public CompanyCode companyCode() {
        return companyCode;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, companyCode);
    }

    protected AirTransportCompany() {
        // for ORM
    }
}
