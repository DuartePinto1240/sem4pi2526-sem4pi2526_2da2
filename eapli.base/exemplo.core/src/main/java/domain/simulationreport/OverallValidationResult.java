// ─── domain/simulationreport/OverallValidationResult.java ─────────────────

package domain.simulationreport;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class OverallValidationResult implements ValueObject {

    public enum Result { PASS, FAIL }

    @Enumerated(EnumType.STRING)
    private Result result;

    private OverallValidationResult(final Result result) {
        this.result = result;
    }

    public static OverallValidationResult pass() {
        return new OverallValidationResult(Result.PASS);
    }

    public static OverallValidationResult fail() {
        return new OverallValidationResult(Result.FAIL);
    }

    public boolean isPassed() { return result == Result.PASS; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof OverallValidationResult)) return false;
        return result == ((OverallValidationResult) o).result;
    }

    @Override
    public int hashCode() { return Objects.hash(result); }

    @Override
    public String toString() { return result.name(); }

    protected OverallValidationResult() {}
}