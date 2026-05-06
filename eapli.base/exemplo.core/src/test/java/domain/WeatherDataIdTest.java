package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataIdTest {

    @Test
    void ensureValidWeatherDataIdIsCreated() {
        WeatherDataId id = new WeatherDataId(1L);

        assertEquals(1L, id.value());
    }

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherDataId(null));
    }

    @Test
    void ensureZeroIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherDataId(0L));
    }

    @Test
    void ensureNegativeIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherDataId(-1L));
    }

    @Test
    void ensureEqualsWorksForSameObject() {
        WeatherDataId id = new WeatherDataId(1L);

        assertEquals(id, id);
    }

    @Test
    void ensureEqualsWorksForSameValue() {
        assertEquals(new WeatherDataId(1L), new WeatherDataId(1L));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentValue() {
        assertNotEquals(new WeatherDataId(1L), new WeatherDataId(2L));
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(new WeatherDataId(1L), null);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(new WeatherDataId(1L), "1");
    }

    @Test
    void ensureHashCodeIsEqualForSameValue() {
        assertEquals(
                new WeatherDataId(1L).hashCode(),
                new WeatherDataId(1L).hashCode()
        );
    }

    @Test
    void ensureCompareToReturnsZeroForSameValue() {
        assertEquals(0, new WeatherDataId(1L).compareTo(new WeatherDataId(1L)));
    }

    @Test
    void ensureCompareToReturnsNegativeForSmallerValue() {
        assertTrue(new WeatherDataId(1L).compareTo(new WeatherDataId(2L)) < 0);
    }

    @Test
    void ensureCompareToReturnsPositiveForGreaterValue() {
        assertTrue(new WeatherDataId(2L).compareTo(new WeatherDataId(1L)) > 0);
    }

    @Test
    void ensureToStringReturnsIdValue() {
        assertEquals("123", new WeatherDataId(123L).toString());
    }
}