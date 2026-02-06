package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Definition Unit Tests")
class DefinitionTest {

    @Test
    @DisplayName("Should create Definition with all valid fields")
    void shouldCreateWithAllValidFields() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String name = "Test Definition";
        String description = "Test Description";

        // Act
        Definition definition = new Definition(id, name, description);

        // Assert
        assertNotNull(definition);
        assertEquals(id, definition.id());
        assertEquals(name, definition.name());
        assertEquals(description, definition.description());
    }

    @Test
    @DisplayName("Should convert null description to empty string")
    void shouldConvertNullDescriptionToEmptyString() {
        // Arrange
        DefinitionId id = new DefinitionId("456");
        String name = "Named Definition";

        // Act
        Definition definition = new Definition(id, name, null);

        // Assert
        assertEquals("", definition.description());
        assertNotNull(definition.description());
    }

    @Test
    @DisplayName("Should preserve empty string description")
    void shouldPreserveEmptyStringDescription() {
        // Arrange
        DefinitionId id = new DefinitionId("789");
        String name = "Named Definition";
        String description = "";

        // Act
        Definition definition = new Definition(id, name, description);

        // Assert
        assertEquals("", definition.description());
    }

    @Test
    @DisplayName("Should throw NullPointerException when id is null")
    void shouldThrowWhenIdIsNull() {
        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Definition(null, "Name", "Description")
        );
        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when name is null")
    void shouldThrowWhenNameIsNull() {
        // Arrange
        DefinitionId id = new DefinitionId("123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Definition(id, null, "Description")
        );
        assertEquals("name must not be blank", exception.getMessage());
    }

    @ParameterizedTest
    @DisplayName("Should throw IllegalArgumentException when name is blank")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    void shouldThrowWhenNameIsBlank(String blankName) {
        // Arrange
        DefinitionId id = new DefinitionId("123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Definition(id, blankName, "Description")
        );
        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should create Definition with minimal whitespace name")
    void shouldCreateWithMinimalWhitespaceName() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String name = "a";

        // Act
        Definition definition = new Definition(id, name, "Description");

        // Assert
        assertEquals("a", definition.name());
    }

    @Test
    @DisplayName("Should preserve name with leading/trailing spaces")
    void shouldPreserveNameWithSpaces() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String name = "  Definition Name  ";

        // Act
        Definition definition = new Definition(id, name, "Description");

        // Assert
        assertEquals("  Definition Name  ", definition.name());
    }

    @Test
    @DisplayName("Should preserve description with special characters")
    void shouldPreserveDescriptionWithSpecialChars() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String description = "Description with special chars: @#$%^&*()";

        // Act
        Definition definition = new Definition(id, "Name", description);

        // Assert
        assertEquals(description, definition.description());
    }

    @Test
    @DisplayName("Should preserve description with newlines")
    void shouldPreserveDescriptionWithNewlines() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String description = "Line 1\nLine 2\nLine 3";

        // Act
        Definition definition = new Definition(id, "Name", description);

        // Assert
        assertEquals(description, definition.description());
    }

    @Test
    @DisplayName("Should preserve very long description")
    void shouldPreserveLongDescription() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String description = "A".repeat(10000);

        // Act
        Definition definition = new Definition(id, "Name", description);

        // Assert
        assertEquals(description, definition.description());
        assertEquals(10000, definition.description().length());
    }

    @Test
    @DisplayName("Should return correct id")
    void shouldReturnCorrectId() {
        // Arrange
        DefinitionId id = new DefinitionId("test-id-123");
        Definition definition = new Definition(id, "Name", "Description");

        // Act
        DefinitionId returnedId = definition.id();

        // Assert
        assertEquals(id, returnedId);
        assertEquals("test-id-123", returnedId.value());
    }

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
        // Arrange
        String name = "Test Definition Name";
        Definition definition = new Definition(new DefinitionId("123"), name, "Description");

        // Act
        String returnedName = definition.name();

        // Assert
        assertEquals(name, returnedName);
    }

    @Test
    @DisplayName("Should create multiple instances independently")
    void shouldCreateMultipleInstancesIndependently() {
        // Arrange
        DefinitionId id1 = new DefinitionId("id1");
        DefinitionId id2 = new DefinitionId("id2");
        Definition def1 = new Definition(id1, "Name 1", "Description 1");
        Definition def2 = new Definition(id2, "Name 2", "Description 2");

        // Act & Assert
        assertEquals("Name 1", def1.name());
        assertEquals("Name 2", def2.name());
        assertNotEquals(def1.id(), def2.id());
        assertNotEquals(def1.name(), def2.name());
    }

    @Test
    @DisplayName("Should handle name with only non-whitespace characters")
    void shouldHandleNameWithOnlyNonWhitespace() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String name = "ValidNameWithNoSpaces";

        // Act
        Definition definition = new Definition(id, name, "Description");

        // Assert
        assertEquals(name, definition.name());
    }

    @Test
    @DisplayName("Should handle Unicode characters in name and description")
    void shouldHandleUnicodeCharacters() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        String name = "定義 🎯 Définition";
        String description = "中文 🔥 Русский العربية";

        // Act
        Definition definition = new Definition(id, name, description);

        // Assert
        assertEquals(name, definition.name());
        assertEquals(description, definition.description());
    }
}
