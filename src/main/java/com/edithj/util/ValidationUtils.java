package com.edithj.util;

/**
 * Centralized validation utilities for common checks.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Normalize and validate non-null text.
     *
     * @param value the value to normalize
     * @return trimmed text, never null
     */
    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Check if text is empty after normalization.
     */
    public static boolean isEmpty(String value) {
        return normalize(value).isBlank();
    }

    /**
     * Check if ID is valid (non-null, non-blank).
     */
    public static boolean isValidId(String id) {
        return id != null && !id.isBlank();
    }

    /**
     * Validate that text is not empty.
     *
     * @throws IllegalArgumentException if text is empty
     */
    public static String validateNonEmpty(String text, String fieldName) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return normalized;
    }

    /**
     * Truncate text to maximum length with ellipsis.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
