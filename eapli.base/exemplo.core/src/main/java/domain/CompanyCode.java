package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class CompanyCode implements ValueObject, Comparable<CompanyCode> {

    private String iata;
    private String icao;

    public CompanyCode(final String iata, final String icao) {
        Preconditions.nonEmpty(iata, "IATA code cannot be empty");
        Preconditions.nonEmpty(icao, "ICAO code cannot be empty");

        if (!iata.toUpperCase().matches("[A-Z]{2}")) {
            throw new IllegalArgumentException(
                    "IATA code must be exactly 2 uppercase letters");
        }
        if (!icao.toUpperCase().matches("[A-Z]{2,3}")) {
            throw new IllegalArgumentException(
                    "ICAO code must be 2 or 3 uppercase letters");
        }

        this.iata = iata.toUpperCase();
        this.icao = icao.toUpperCase();
    }

    public String iata() {
        return iata;
    }

    public String icao() {
        return icao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyCode)) return false;
        CompanyCode that = (CompanyCode) o;
        return Objects.equals(iata, that.iata) && Objects.equals(icao, that.icao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iata, icao);
    }

    @Override
    public int compareTo(CompanyCode other) {
        int c = this.iata.compareTo(other.iata);
        return c != 0 ? c : this.icao.compareTo(other.icao);
    }

    @Override
    public String toString() {
        return String.format("IATA: %s | ICAO: %s", iata, icao);
    }

    protected CompanyCode() {
        // for ORM
    }
}