package domain;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o Aggregate Root AirTransportCompany.
 * Cobre US060 - Registar companhia de transporte aéreo.
 */
class AirTransportCompanyTest {

    private AirTransportCompany buildCompany() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    // ----------------------------------------------------------------
    // Criação válida
    // ----------------------------------------------------------------

    @Test
    void validCompanyIsCreated() {
        AirTransportCompany company = buildCompany();
        assertNotNull(company);
        assertEquals("TAP Air Portugal", company.name());
        assertEquals("TP", company.identity().iata());
        assertEquals("TAP", company.identity().icao());
    }


    @Test
    void lowercaseCodesAreAcceptedAndConverted() {
        AirTransportCompany company =
                new AirTransportCompany("TAP Air Portugal", "tp", "tap");
        assertEquals("TP", company.identity().iata());
        assertEquals("TAP", company.identity().icao());
    }

    // ----------------------------------------------------------------
    // Nome inválido
    // ----------------------------------------------------------------

    @Test
    void emptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("", "TP", "TAP"));
    }

    @Test
    void nullNameIsRejected() {
        assertThrows(Exception.class,
                () -> new AirTransportCompany(null, "TP", "TAP"));
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("   ", "TP", "TAP"));
    }

    // ----------------------------------------------------------------
    // IATA inválido
    // ----------------------------------------------------------------

    @Test
    void iataWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "T", "TAP"));
    }

    @Test
    void iataWithThreeLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TAP", "TAP"));
    }

    @Test
    void emptyIataIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "", "TAP"));
    }

    // ----------------------------------------------------------------
    // ICAO inválido
    // ----------------------------------------------------------------

    @Test
    void icaoWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TP", "T"));
    }

    @Test
    void icaoWithFourLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TP", "TAPP"));
    }

    @Test
    void emptyIcaoIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TP", ""));
    }

    // ----------------------------------------------------------------
    // Identidade e igualdade
    // ----------------------------------------------------------------

    @Test
    void identityIsCompanyCode() {
        AirTransportCompany company = buildCompany();
        CompanyCode code = company.identity();
        assertEquals("TP", code.iata());
        assertEquals("TAP", code.icao());
    }

    @Test
    void sameAsReturnsTrueForSameCodes() {
        AirTransportCompany c1 = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
        AirTransportCompany c2 = new AirTransportCompany("TAP", "TP", "TAP");
        assertTrue(c1.sameAs(c2));
    }

    @Test
    void sameAsReturnsFalseForDifferentCodes() {
        AirTransportCompany c1 = new AirTransportCompany("TAP", "TP", "TAP");
        AirTransportCompany c2 = new AirTransportCompany("Ryanair", "FR", "RYR");
        assertFalse(c1.sameAs(c2));
    }

    @Test
    void sameAsReturnsFalseForNonCompany() {
        AirTransportCompany company = buildCompany();
        assertFalse(company.sameAs("not a company"));
    }

    @Test
    void toStringContainsNameAndCodes() {
        AirTransportCompany company = buildCompany();
        String result = company.toString();
        assertTrue(result.contains("TAP Air Portugal"));
        assertTrue(result.contains("TP"));
        assertTrue(result.contains("TAP"));
    }

    @Test
    void companyCodeGetterReturnsCorrectCode() {

        AirTransportCompany company = buildCompany();

        assertNotNull(company.companyCode());
        assertEquals("TP", company.companyCode().iata());
        assertEquals("TAP", company.companyCode().icao());
    }

    @Test
    void nameGetterReturnsCorrectName() {

        AirTransportCompany company = buildCompany();

        assertEquals("TAP Air Portugal", company.name());
    }

    @Test
    void sameAsReturnsTrueForSameInstance() {

        AirTransportCompany company = buildCompany();

        assertTrue(company.sameAs(company));
    }

    @Test
    void identityNeverReturnsNull() {

        AirTransportCompany company = buildCompany();

        assertNotNull(company.identity());
    }

    @Test
    void toStringIsNotEmpty() {

        AirTransportCompany company = buildCompany();

        assertFalse(company.toString().isBlank());
    }


    @Test
    void companyWithTrimmedNameIsAccepted() {

        AirTransportCompany company =
                new AirTransportCompany(
                        " TAP Air Portugal ",
                        "TP",
                        "TAP");

        assertTrue(company.name().contains("TAP"));
    }

    @Test
    void nullIataIsRejected() {

        assertThrows(Exception.class,
                () -> new AirTransportCompany(
                        "TAP",
                        null,
                        "TAP"));
    }

    @Test
    void nullIcaoIsRejected() {

        assertThrows(Exception.class,
                () -> new AirTransportCompany(
                        "TAP",
                        "TP",
                        null));
    }

    @Test
    void companyCodeObjectIsConsistent() {

        AirTransportCompany company = buildCompany();

        CompanyCode code = company.companyCode();

        assertEquals(company.identity(), code);
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<AirTransportCompany> ctor =
                AirTransportCompany.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final AirTransportCompany instance = ctor.newInstance();
        assertNotNull(instance);
    }

// ── Collaborator management (US061–US064) ─────────────────────

    @Test
    void ensureAddCollaboratorSuccessfully() {
        // Arrange
        final AirTransportCompany company = buildCompany();

        // Act
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");

        // Assert — US061
        assertTrue(company.findCollaboratorByEmail("joao@tap.pt").isPresent());
    }

    @Test
    void ensureNewCollaboratorIsActive() {
        // Arrange
        final AirTransportCompany company = buildCompany();

        // Act
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");

        // Assert — US061.5
        assertTrue(company.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }

    @Test
    void ensureAddDuplicateEmailCollaboratorThrows() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> company.addCollaborator("joao@tap.pt", "Other Person", "Analyst"));
    }

    @Test
    void ensureListActiveCollaboratorsExcludesDisabled() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("active@tap.pt", "Active", "Manager");
        company.addCollaborator("disabled@tap.pt", "Disabled", "Analyst");
        company.disableCollaborator("disabled@tap.pt");

        // Act
        final var list = company.listActiveCollaborators();

        // Assert — US062
        assertEquals(1, list.size());
        assertEquals("active@tap.pt", list.get(0).email);
    }

    @Test
    void ensureListActiveCollaboratorsIsEmptyWhenAllDisabled() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("only@tap.pt", "Only", "Manager");
        company.disableCollaborator("only@tap.pt");

        // Act & Assert — US062
        assertTrue(company.listActiveCollaborators().isEmpty());
    }

    @Test
    void ensureUpdateCollaboratorEmailSuccessfully() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("old@tap.pt", "João", "Manager");

        // Act — US063
        company.updateCollaboratorEmail("old@tap.pt", "new@tap.pt");

        // Assert
        assertFalse(company.findCollaboratorByEmail("old@tap.pt").isPresent());
        assertTrue(company.findCollaboratorByEmail("new@tap.pt").isPresent());
    }

    @Test
    void ensureUpdateCollaboratorEmailForNonExistentThrows() {
        // Arrange
        final AirTransportCompany company = buildCompany();

        // Act & Assert — US063
        assertThrows(IllegalArgumentException.class,
                () -> company.updateCollaboratorEmail("ghost@tap.pt", "new@tap.pt"));
    }

    @Test
    void ensureNameIsUnchangedAfterEmailUpdate() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");

        // Act — US063: only email changes
        company.updateCollaboratorEmail("joao@tap.pt", "joao.new@tap.pt");

        // Assert — name immutable
        assertEquals("João Silva",
                company.findCollaboratorByEmail("joao.new@tap.pt").get().name());
    }

    @Test
    void ensurePositionIsUnchangedAfterEmailUpdate() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");

        // Act — US063: only email changes
        company.updateCollaboratorEmail("joao@tap.pt", "joao.new@tap.pt");

        // Assert — position immutable
        assertEquals("Manager",
                company.findCollaboratorByEmail("joao.new@tap.pt").get().position());
    }

    @Test
    void ensureDisableCollaboratorSetsActiveFalse() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("joao@tap.pt", "João", "Manager");

        // Act — US064
        company.disableCollaborator("joao@tap.pt");

        // Assert
        assertFalse(company.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }

    @Test
    void ensureDisableCollaboratorIsIdempotent() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("joao@tap.pt", "João", "Manager");
        company.disableCollaborator("joao@tap.pt");

        // Act & Assert — US064.5
        assertDoesNotThrow(() -> company.disableCollaborator("joao@tap.pt"));
    }

    @Test
    void ensureDisableCollaboratorForNonExistentThrows() {
        // Arrange
        final AirTransportCompany company = buildCompany();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> company.disableCollaborator("ghost@tap.pt"));
    }

    @Test
    void ensureCollaboratorRecordRetainedAfterDisable() {
        // Arrange
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("joao@tap.pt", "João", "Manager");

        // Act — US064 soft delete
        company.disableCollaborator("joao@tap.pt");

        // Assert: record still exists, just inactive
        assertTrue(company.findCollaboratorByEmail("joao@tap.pt").isPresent());
        assertFalse(company.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }
}