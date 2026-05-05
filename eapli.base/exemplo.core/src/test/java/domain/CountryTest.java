package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Country} value object (US052).
 */
public class CountryTest {

    @Test
    public void ensureValidCountryIsCreated() {
        final Country country = new Country("Portugal");

        assertEquals("Portugal", country.name());
    }

    @Test
    public void ensureCountryNameIsTrimmed() {
        final Country country = new Country("  Portugal  ");

        assertEquals("Portugal", country.name());
    }

    @Test
    public void ensureBlankCountryNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Country("   "));
    }

    @Test
    public void ensureNullCountryNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Country(null));
    }

    @Test
    public void ensureTwoCountriesWithSameNameAreEqualAndHaveSameHashCode() {
        final Country first = new Country("Portugal");
        final Country second = new Country("Portugal");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void ensureCountryIsEqualToItself() {
        final Country country = new Country("Portugal");

        assertEquals(country, country);
    }

    @Test
    public void ensureCountriesWithDifferentNamesAreNotEqual() {
        assertNotEquals(new Country("Portugal"), new Country("Spain"));
    }

    @Test
    public void ensureCountryIsNotEqualToNullOrOtherType() {
        final Country country = new Country("Portugal");

        assertNotEquals(country, null);
        assertNotEquals(country, "Portugal");
    }

    @Test
    public void ensureToStringReturnsCountryName() {
        assertEquals("Portugal", new Country("Portugal").toString());
    }
}

