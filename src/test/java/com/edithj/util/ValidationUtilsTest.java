package com.edithj.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidationUtilsTest {

    @Test
    void normalize_trims_and_lowercases() {
        assertEquals("hello world", ValidationUtils.normalize("  HELLO WORLD  "));
    }

    @Test
    void normalize_handlesNull() {
        assertEquals("", ValidationUtils.normalize(null));
    }

    @Test
    void normalize_handlesEmptyString() {
        assertEquals("", ValidationUtils.normalize(""));
    }

    @Test
    void normalize_preservesInternalSpaces() {
        assertEquals("hello world test", ValidationUtils.normalize("  HELLO   WORLD   TEST  "));
    }

    @Test
    void isEmpty_returnsTrueForNull() {
        assertTrue(ValidationUtils.isEmpty(null));
    }

    @Test
    void isEmpty_returnsTrueForBlank() {
        assertTrue(ValidationUtils.isEmpty(""));
        assertTrue(ValidationUtils.isEmpty("   "));
    }

    @Test
    void isEmpty_returnsFalseForText() {
        assertFalse(ValidationUtils.isEmpty("hello"));
    }

    @Test
    void validateNonEmpty_throwsForNull() {
        try {
            ValidationUtils.validateNonEmpty(null, "test");
            assertTrue(false, "Should have thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("test"));
        }
    }

    @Test
    void validateNonEmpty_throwsForBlank() {
        try {
            ValidationUtils.validateNonEmpty("   ", "test");
            assertTrue(false, "Should have thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("test"));
        }
    }

    @Test
    void validateNonEmpty_returnsNormalizedValue() {
        String result = ValidationUtils.validateNonEmpty("  HELLO  ", "test");
        assertEquals("hello", result);
    }

    @Test
    void isValidId_returnsTrueForUUID() {
        assertTrue(ValidationUtils.isValidId("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void isValidId_returnsFalseForNull() {
        assertFalse(ValidationUtils.isValidId(null));
    }

    @Test
    void isValidId_returnsFalseForBlank() {
        assertFalse(ValidationUtils.isValidId(""));
        assertFalse(ValidationUtils.isValidId("   "));
    }

    @Test
    void isValidId_returnsFalseForInvalidFormat() {
        assertFalse(ValidationUtils.isValidId("not-a-uuid"));
    }
}

