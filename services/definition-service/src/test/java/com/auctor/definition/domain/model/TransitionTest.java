package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Transition domain model.
 */
class TransitionTest {
    
    @Test
    void shouldCreateValidTransition() {
        Transition transition = new Transition("DRAFT", "APPROVED", null, null);
        assertNotNull(transition);
        assertEquals("DRAFT", transition.fromState());
        assertEquals("APPROVED", transition.toState());
    }
    
    @Test
    void shouldRejectSameFromAndToState() {
        assertThrows(IllegalArgumentException.class, () ->
            new Transition("DRAFT", "DRAFT", null, null)
        );
    }
    
    @Test
    void shouldRejectBlankFromState() {
        assertThrows(IllegalArgumentException.class, () ->
            new Transition("", "APPROVED", null, null)
        );
    }
    
    @Test
    void shouldRejectBlankToState() {
        assertThrows(IllegalArgumentException.class, () ->
            new Transition("DRAFT", "", null, null)
        );
    }
}
