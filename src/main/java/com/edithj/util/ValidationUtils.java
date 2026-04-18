package com.edithj.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Centralized validation utilities for common checks.
 */
public final class ValidationUtils {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Normalize and validate non-null text.
     *
     * @param value the value to normalize
     * @return trimmed, space-normalized lowercase text, never null
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Check if text is empty after normalization.
     */
    public static boolean isEmpty(String value) {
        return normalize(value).isBlank();
    }

    /**
     * Check if ID is valid for the expected UUID format.
     */
    public static boolean isValidId(String id) {
        if (id == null) {
            return false;
        }

        String normalized = id.trim();
        if (normalized.isBlank()) {
            return false;
        }

        return ID_PATTERN.matcher(normalized).matches();
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
