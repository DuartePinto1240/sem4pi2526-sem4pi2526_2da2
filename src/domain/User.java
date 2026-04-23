package domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.infrastructure.authz.domain.model.Role;
import jakarta.persistence.*;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

@Entity
public final class User implements AggregateRoot<EmailAddress> {

    @EmbeddedId
    private EmailAddress email;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;

    private boolean active;

    @ElementCollection
    private Set<Role> roles = new HashSet<>();

    protected User() { /* JPA */ }

    private Calendar securityClearanceExpiry; // Garante que este campo existe

    public User(final EmailAddress email, final String name, final String phoneNumber,
                final Set<Role> roles, final Calendar expiry) { // <--- Adiciona o 5º aqui
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.roles = roles;
        this.securityClearanceExpiry = expiry; // <--- Guarda o valor
        this.active = true;
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }
    public boolean isActive() { return this.active; }

    @Override
    public boolean sameAs(Object other) {
        if (!(other instanceof User)) return false;
        return this.email.equals(((User) other).email);
    }

    @Override
    public EmailAddress identity() { return this.email; }
}