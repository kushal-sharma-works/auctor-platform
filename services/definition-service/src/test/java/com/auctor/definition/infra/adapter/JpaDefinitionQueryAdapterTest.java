package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.Definition;
import com.auctor.definition.domain.model.DefinitionId;
import com.auctor.definition.infra.jpa.DefinitionJpaEntity;
import com.auctor.definition.infra.jpa.DefinitionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JpaDefinitionQueryAdapter Unit Tests")
class JpaDefinitionQueryAdapterTest {

    @Mock
    private DefinitionJpaRepository repository;

    private JpaDefinitionQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new JpaDefinitionQueryAdapter(repository);
    }

    @Test
    @DisplayName("Should return Definition when entity is found")
    void shouldReturnDefinitionWhenEntityFound() {
        // Arrange
        var id = new DefinitionId("123");
        var entity = new DefinitionJpaEntity("123", "Sample Definition", "Test Description");
        when(repository.findById("123")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(id);

        // Assert
        assertTrue(result.isPresent());
        var definition = result.get();
        assertAll(
                () -> assertEquals("123", definition.id().value()),
                () -> assertEquals("Sample Definition", definition.name()),
                () -> assertEquals("Test Description", definition.description())
        );
        verify(repository, times(1)).findById("123");
    }

    @Test
    @DisplayName("Should return empty Optional when entity not found")
    void shouldReturnEmptyOptionalWhenEntityNotFound() {
        // Arrange
        var id = new DefinitionId("missing-id");
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        // Act
        var result = adapter.findById(id);

        // Assert
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findById("missing-id");
    }

    @Test
    @DisplayName("Should map entity to domain model correctly")
    void shouldMapEntityToDomainModelCorrectly() {
        // Arrange
        var entity = new DefinitionJpaEntity("entity-id-123", "Entity Definition", "Entity Description");
        when(repository.findById("entity-id-123")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId("entity-id-123"));

        // Assert
        assertTrue(result.isPresent());
        var definition = result.get();
        assertAll(
                () -> assertEquals(entity.getId(), definition.id().value()),
                () -> assertEquals(entity.getName(), definition.name()),
                () -> assertEquals(entity.getDescription(), definition.description())
        );
    }

    @Test
    @DisplayName("Should handle null description in entity")
    void shouldHandleNullDescriptionInEntity() {
        // Arrange
        var entity = new DefinitionJpaEntity("456", "Definition Without Description", null);
        when(repository.findById("456")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId("456"));

        // Assert
        assertTrue(result.isPresent());
        assertEquals("", result.get().description());
    }

    @Test
    @DisplayName("Should call repository with correct ID")
    void shouldCallRepositoryWithCorrectId() {
        // Arrange
        var id = new DefinitionId("specific-id");
        when(repository.findById("specific-id")).thenReturn(Optional.empty());

        // Act
        adapter.findById(id);

        // Assert
        verify(repository, times(1)).findById("specific-id");
    }

    @Test
    @DisplayName("Should create new DefinitionId from entity ID")
    void shouldCreateNewDefinitionIdFromEntityId() {
        // Arrange
        var entity = new DefinitionJpaEntity("new-def-id", "Name", "Desc");
        when(repository.findById("new-def-id")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId("new-def-id"));

        // Assert
        assertTrue(result.isPresent());
        var returnedId = result.get().id();
        assertEquals("new-def-id", returnedId.value());
    }

    @Test
    @DisplayName("Should handle multiple queries independently")
    void shouldHandleMultipleQueriesIndependently() {
        // Arrange
        var entity1 = new DefinitionJpaEntity("id-1", "Definition 1", "Description 1");
        var entity2 = new DefinitionJpaEntity("id-2", "Definition 2", "Description 2");
        when(repository.findById("id-1")).thenReturn(Optional.of(entity1));
        when(repository.findById("id-2")).thenReturn(Optional.of(entity2));

        // Act
        var result1 = adapter.findById(new DefinitionId("id-1"));
        var result2 = adapter.findById(new DefinitionId("id-2"));

        // Assert
        assertAll(
                () -> assertTrue(result1.isPresent()),
                () -> assertTrue(result2.isPresent()),
                () -> assertEquals("Definition 1", result1.get().name()),
                () -> assertEquals("Definition 2", result2.get().name())
        );
        verify(repository, times(1)).findById("id-1");
        verify(repository, times(1)).findById("id-2");
    }

    @Test
    @DisplayName("Should preserve special characters in mapping")
    void shouldPreserveSpecialCharactersInMapping() {
        // Arrange
        var entity = new DefinitionJpaEntity("id@123", "Definition-With-Special_Chars.v1", "Description with @#$%^& special chars");
        when(repository.findById("id@123")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId("id@123"));

        // Assert
        assertTrue(result.isPresent());
        var definition = result.get();
        assertAll(
                () -> assertEquals("Definition-With-Special_Chars.v1", definition.name()),
                () -> assertEquals("Description with @#$%^& special chars", definition.description())
        );
    }

    @Test
    @DisplayName("Should handle very long strings in mapping")
    void shouldHandleVeryLongStringsInMapping() {
        // Arrange
        var longId = "id".repeat(500);
        var longName = "Name".repeat(500);
        var longDescription = "Description".repeat(500);
        var entity = new DefinitionJpaEntity(longId, longName, longDescription);
        when(repository.findById(longId)).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId(longId));

        // Assert
        assertTrue(result.isPresent());
        var definition = result.get();
        assertAll(
                () -> assertEquals(longName, definition.name()),
                () -> assertEquals(longDescription, definition.description())
        );
    }

    @Test
    @DisplayName("Should handle empty string description")
    void shouldHandleEmptyStringDescription() {
        // Arrange
        var entity = new DefinitionJpaEntity("id-with-empty-desc", "Definition", "");
        when(repository.findById("id-with-empty-desc")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId("id-with-empty-desc"));

        // Assert
        assertTrue(result.isPresent());
        assertEquals("", result.get().description());
    }

    @Test
    @DisplayName("Should call repository once per query")
    void shouldCallRepositoryOncePerQuery() {
        // Arrange
        var entity = new DefinitionJpaEntity("single-query-id", "Definition", "Description");
        when(repository.findById("single-query-id")).thenReturn(Optional.of(entity));

        // Act
        adapter.findById(new DefinitionId("single-query-id"));

        // Assert
        verify(repository, times(1)).findById("single-query-id");
    }

    @Test
    @DisplayName("Should handle unicode characters in mapping")
    void shouldHandleUnicodeCharactersInMapping() {
        // Arrange
        var entity = new DefinitionJpaEntity("unicode-id", "定義 🎯 Définition", "中文 🔥 Русский العربية");
        when(repository.findById("unicode-id")).thenReturn(Optional.of(entity));

        // Act
        var result = adapter.findById(new DefinitionId("unicode-id"));

        // Assert
        assertTrue(result.isPresent());
        var definition = result.get();
        assertAll(
                () -> assertEquals("定義 🎯 Définition", definition.name()),
                () -> assertEquals("中文 🔥 Русский العربية", definition.description())
        );
    }
}
