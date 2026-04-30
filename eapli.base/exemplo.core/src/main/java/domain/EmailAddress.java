package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class EmailAddress implements ValueObject, Serializable, Comparable<EmailAddress> {
    private String email;

    protected EmailAddress() { /* JPA */ }

    public EmailAddress(final String email) {
        Preconditions.nonEmpty(email, "Email should not be empty");
        // Regex básica de validação
        Preconditions.matches(Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"), email, "Invalid email format");
        this.email = email;
    }

    public static EmailAddress valueOf(final String email) {
        return new EmailAddress(email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() { return Objects.hash(email); }

    @Override
    public String toString() { return this.email; }

    @Override
    public int compareTo(EmailAddress o) { return this.email.compareTo(o.email); }
}