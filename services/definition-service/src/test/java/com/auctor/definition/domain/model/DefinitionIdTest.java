package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefinitionId Unit Tests")
class DefinitionIdTest {

    @Test
    @DisplayName("Should create DefinitionId with valid value")
    void shouldCreateWithValidValue() {
        // Act
        DefinitionId id = new DefinitionId("valid-id-123");

        // Assert
        assertNotNull(id);
        assertEquals("valid-id-123", id.value());
    }

    @Test
    @DisplayName("Should create DefinitionId with numeric value")
    void shouldCreateWithNumericValue() {
        // Act
        DefinitionId id = new DefinitionId("12345");

        // Assert
        assertEquals("12345", id.value());
    }

    @Test
    @DisplayName("Should create DefinitionId with UUID format")
    void shouldCreateWithUuidFormat() {
        // Act
        DefinitionId id = new DefinitionId("550e8400-e29b-41d4-a716-446655440000");

        // Assert
        assertEquals("550e8400-e29b-41d4-a716-446655440000", id.value());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when value is null")
    void shouldThrowWhenValueIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DefinitionId(null)
        );
        assertEquals("DefinitionId cannot be blank", exception.getMessage());
    }

    @ParameterizedTest
    @DisplayName("Should throw IllegalArgumentException when value is blank")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    void shouldThrowWhenValueIsBlank(String blankValue) {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefinitionId(blankValue)
        );
    }

    @Test
    @DisplayName("Should return value correctly")
    void shouldReturnValueCorrectly() {
        // Arrange
        String expectedValue = "test-definition-id";
        DefinitionId id = new DefinitionId(expectedValue);

        // Act
        String actualValue = id.value();

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    @DisplayName("Should be equal when values are same")
    void shouldBeEqualWhenValuesSame() {
        // Arrange
        DefinitionId id1 = new DefinitionId("same-id");
        DefinitionId id2 = new DefinitionId("same-id");

        // Act & Assert
        assertEquals(id1, id2);
    }

    @Test
    @DisplayName("Should not be equal when values are different")
    void shouldNotBeEqualWhenValuesDifferent() {
        // Arrange
        DefinitionId id1 = new DefinitionId("id-1");
        DefinitionId id2 = new DefinitionId("id-2");

        // Act & Assert
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
        // Arrange
        DefinitionId id = new DefinitionId("self-id");

        // Act & Assert
        assertEquals(id, id);
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
        // Arrange
        DefinitionId id = new DefinitionId("valid-id");

        // Act & Assert
        assertNotEquals(id, null);
        assertFalse(id.equals(null));
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
        // Arrange
        DefinitionId id = new DefinitionId("valid-id");
        String stringValue = "valid-id";

        // Act & Assert
        assertNotEquals(id, stringValue);
    }

    @Test
    @DisplayName("Should have same hash code for equal objects")
    void shouldHaveSameHashCodeForEqualObjects() {
        // Arrange
        DefinitionId id1 = new DefinitionId("same-id");
        DefinitionId id2 = new DefinitionId("same-id");

        // Act & Assert
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("Should have different hash code for different values")
    void shouldHaveDifferentHashCodeForDifferentValues() {
        // Arrange
        DefinitionId id1 = new DefinitionId("id-1");
        DefinitionId id2 = new DefinitionId("id-2");

        // Act & Assert
        assertNotEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("Should be usable in hash-based collections")
    void shouldBeUsableInHashBasedCollections() {
        // Arrange
        DefinitionId id1 = new DefinitionId("id-1");
        DefinitionId id2 = new DefinitionId("id-1");
        DefinitionId id3 = new DefinitionId("id-2");

        // Act
        java.util.HashMap<DefinitionId, String> map = new java.util.HashMap<>();
        map.put(id1, "value1");
        map.put(id3, "value2");

        // Assert
        assertEquals("value1", map.get(id2)); // id2 equals id1, should retrieve same value
        assertEquals(2, map.size());
    }
}
