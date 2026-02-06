package com.auctor.definition.domain.service;

import com.auctor.definition.domain.model.Definition;
import com.auctor.definition.domain.model.DefinitionId;
import com.auctor.definition.domain.port.DefinitionQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DefinitionService Unit Tests")
class DefinitionServiceTest {

    @Mock
    private DefinitionQueryPort queryPort;

    private DefinitionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefinitionService(queryPort);
    }

    @Test
    @DisplayName("Should return definition when found with valid ID")
    void shouldReturnDefinitionWhenFound() {
        // Arrange
        DefinitionId id = new DefinitionId("123");
        Definition expected = new Definition(
                id,
                "sample-definition",
                "stored in database"
        );
        when(queryPort.findById(id)).thenReturn(Optional.of(expected));

        // Act
        Definition result = service.getDefinition(id);

        // Assert
        assertNotNull(result);
        assertEquals("123", result.id().value());
        assertEquals("sample-definition", result.name());
        assertEquals("stored in database", result.description());
        verify(queryPort, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should return definition with empty description when not provided")
    void shouldReturnDefinitionWithEmptyDescription() {
        // Arrange
        DefinitionId id = new DefinitionId("456");
        Definition expected = new Definition(
                id,
                "another-definition",
                null
        );
        when(queryPort.findById(id)).thenReturn(Optional.of(expected));

        // Act
        Definition result = service.getDefinition(id);

        // Assert
        assertNotNull(result);
        assertEquals("456", result.id().value());
        assertEquals("another-definition", result.name());
        assertEquals("", result.description());
        verify(queryPort, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when definition not found")
    void shouldThrowWhenDefinitionNotFound() {
        // Arrange
        DefinitionId id = new DefinitionId("missing");
        when(queryPort.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getDefinition(id)
        );
        assertEquals("Definition not found: missing", exception.getMessage());
        verify(queryPort, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException with correct ID in message for non-existent ID")
    void shouldIncludeIdInExceptionMessage() {
        // Arrange
        DefinitionId id = new DefinitionId("non-existent-id-12345");
        when(queryPort.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getDefinition(id)
        );
        assertTrue(exception.getMessage().contains("non-existent-id-12345"));
        verify(queryPort, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should call query port with correct ID parameter")
    void shouldCallQueryPortWithCorrectId() {
        // Arrange
        DefinitionId id = new DefinitionId("test-id");
        when(queryPort.findById(id)).thenReturn(Optional.empty());

        // Act
        try {
            service.getDefinition(id);
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Assert
        verify(queryPort, times(1)).findById(id);
        verify(queryPort).findById(argThat(arg -> arg.value().equals("test-id")));
    }

    @Test
    @DisplayName("Should return definition for multiple different IDs")
    void shouldHandleMultipleDifferentIds() {
        // Arrange
        DefinitionId id1 = new DefinitionId("id1");
        DefinitionId id2 = new DefinitionId("id2");
        Definition def1 = new Definition(id1, "Definition 1", "Desc 1");
        Definition def2 = new Definition(id2, "Definition 2", "Desc 2");

        when(queryPort.findById(id1)).thenReturn(Optional.of(def1));
        when(queryPort.findById(id2)).thenReturn(Optional.of(def2));

        // Act
        Definition result1 = service.getDefinition(id1);
        Definition result2 = service.getDefinition(id2);

        // Assert
        assertEquals("Definition 1", result1.name());
        assertEquals("Definition 2", result2.name());
        verify(queryPort, times(1)).findById(id1);
        verify(queryPort, times(1)).findById(id2);
    }

    @Test
    @DisplayName("Should return same definition object on multiple calls with same ID")
    void shouldReturnSameDefinitionOnMultipleCalls() {
        // Arrange
        DefinitionId id = new DefinitionId("same-id");
        Definition expected = new Definition(id, "Consistent Definition", "Consistent Desc");
        when(queryPort.findById(id)).thenReturn(Optional.of(expected));

        // Act
        Definition result1 = service.getDefinition(id);
        Definition result2 = service.getDefinition(id);

        // Assert
        assertEquals(result1, result2);
        assertEquals(result1.id(), result2.id());
        assertEquals(result1.name(), result2.name());
        verify(queryPort, times(2)).findById(id);
    }

    @Test
    @DisplayName("Should properly handle definition with special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Arrange
        DefinitionId id = new DefinitionId("special-123");
        Definition expected = new Definition(
                id,
                "Definition-With-Special_Chars.v1",
                "Description with special chars: @#$%"
        );
        when(queryPort.findById(id)).thenReturn(Optional.of(expected));

        // Act
        Definition result = service.getDefinition(id);

        // Assert
        assertNotNull(result);
        assertEquals("Definition-With-Special_Chars.v1", result.name());
        assertTrue(result.description().contains("@#$%"));
        verify(queryPort, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should properly handle definition with long names and descriptions")
    void shouldHandleLongContent() {
        // Arrange
        DefinitionId id = new DefinitionId("long-id");
        String longName = "A".repeat(255);
        String longDescription = "B".repeat(1000);
        Definition expected = new Definition(id, longName, longDescription);
        when(queryPort.findById(id)).thenReturn(Optional.of(expected));

        // Act
        Definition result = service.getDefinition(id);

        // Assert
        assertNotNull(result);
        assertEquals(longName, result.name());
        assertEquals(longDescription, result.description());
        verify(queryPort, times(1)).findById(id);
    }
}
