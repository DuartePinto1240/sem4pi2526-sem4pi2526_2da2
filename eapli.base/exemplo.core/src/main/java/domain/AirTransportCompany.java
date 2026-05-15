package domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "AIR_TRANSPORT_COMPANY")
public class AirTransportCompany implements AggregateRoot<CompanyCode> {

    @EmbeddedId
    private CompanyCode companyCode;

    private String name;

    private List<Collaborator> collaborators = new ArrayList<>();

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


    public void addCollaborator(final String email,
                                final String name,
                                final String position) {
        if (findCollaboratorByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "A collaborator with email '" + email
                            + "' already exists in company " + this.name);
        }
        final Collaborator c = new Collaborator(email, name, position);
        collaborators.add(c);
    }


    // Helper function
    public Optional<Collaborator> findCollaboratorByEmail(final String email) {
        if (email == null) return Optional.empty();

        final String collaboratorNormalized = email.trim().toLowerCase();

        for (Collaborator c : collaborators) {
            if (c.email().equals(collaboratorNormalized)) {
                return Optional.of(c);
            }
        }

        return Optional.empty();
    }


}
