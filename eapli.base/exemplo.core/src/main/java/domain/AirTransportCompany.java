package domain;

import dto.CollaboratorDTO;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumns({
            @JoinColumn(name = "COMPANY_IATA", referencedColumnName = "iata"),
            @JoinColumn(name = "COMPANY_ICAO", referencedColumnName = "icao")
    })
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

    public List<CollaboratorDTO> listActiveCollaborators() {
        List<CollaboratorDTO> activeCollaborators = new ArrayList<>();

        for (Collaborator collaborator : collaborators) {
            if (collaborator.isActive()) {
                activeCollaborators.add(CollaboratorDTO.from(collaborator));
            }
        }

        return activeCollaborators;
    }

    public void updateCollaboratorEmail(final String oldEmail, final String newEmail) {
        Optional<Collaborator> collaborator = findCollaboratorByEmail(oldEmail);

        if (collaborator.isEmpty()) {
            throw new IllegalArgumentException(
                    "No collaborator with email '" + oldEmail + "' in company " + name);
        }

        Collaborator c = collaborator.get();
        c.updateEmail(newEmail);
    }

    public void disableCollaborator(final String email) {
        Optional<Collaborator> collaborator = findCollaboratorByEmail(email);

        if (collaborator.isPresent()) {
            collaborator.get().disable();
        } else {
            throw new IllegalArgumentException(
                    "No collaborator with email '" + email + "' in area " + name);
        }
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