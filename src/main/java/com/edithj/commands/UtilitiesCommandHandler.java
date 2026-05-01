package com.edithj.commands;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import com.edithj.assistant.IntentType;

public class UtilitiesCommandHandler implements CommandHandler {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Override
    public IntentType intentType() {
        return IntentType.UTILITIES;
    }

    @Override
    public String handle(CommandContext context) {
        String input = context == null ? "" : context.normalizedInput();
        String lower = input.toLowerCase(Locale.ROOT).trim();

        if (lower.isBlank()) {
            return examples();
        }

        if (isDateRequest(lower)) {
            return "Today is " + LocalDate.now().format(DATE_FORMAT) + ".";
        }

        if (isTimeRequest(lower)) {
            return "Current time is " + LocalTime.now().format(TIME_FORMAT) + ".";
        }

        if (isDayRequest(lower)) {
            return "Today is " + LocalDate.now().getDayOfWeek().name().toLowerCase(Locale.ROOT) + ".";
        }

        String expression = extractExpression(input);
        if (!expression.isBlank()) {
            try {
                BigDecimal value = new ExpressionParser(expression).parse();
                return "Result: " + formatNumber(value);
            } catch (IllegalArgumentException exception) {
                if (exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.ROOT).contains("division by zero")) {
                    return "Cannot divide by zero. Try a different calculation.";
                }
                return "I couldn't parse that calculation. Try: calculate 125 * (4 + 2).";
            }
        }

        String normalizedInput = Objects.toString(input, "").trim().toLowerCase(Locale.ROOT);
        if (normalizedInput.startsWith("calculate") || normalizedInput.startsWith("calc ")) {
            return "I couldn't parse that calculation. Try: calculate 125 * (4 + 2).";
        }

        return examples();
    }

    private boolean isDateRequest(String lower) {
        return lower.contains("date")
                || lower.equals("today")
                || lower.contains("today date")
                || lower.contains("what is today");
    }

    private boolean isTimeRequest(String lower) {
        return lower.contains("time")
                || lower.contains("current time")
                || lower.contains("what time");
    }

    private boolean isDayRequest(String lower) {
        return lower.contains("what day")
                || lower.contains("day today")
                || lower.contains("today day")
                || lower.contains("day of week");
    }

    private String extractExpression(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String expr = input.trim()
                .replaceAll("(?i)^calculate\\s+", "")
                .replaceAll("(?i)^calc\\s+", "")
                .replaceAll("(?i)^what is\\s+", "")
                .replaceAll("(?i)^what's\\s+", "")
                .replaceAll("(?i)\\?+$", "")
                .trim();

        expr = expr.replaceAll("(?i)plus", "+")
                .replaceAll("(?i)minus", "-")
                .replaceAll("(?i)multiplied by", "*")
                .replaceAll("(?i)times", "*")
                .replaceAll("(?i)x", "*")
                .replaceAll("(?i)divided by", "/");

        if (expr.matches(".*\\d+\\s*[+\\-*/()]\\s*\\d+.*")) {
            return expr;
        }

        return "";
    }

    private String formatNumber(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }

    private String examples() {
        return "Try: what time is it, what is today's date, what day is today, calculate 245/7.";
    }

    private static final class ExpressionParser {

        private final String input;
        private int index;

        private ExpressionParser(String input) {
            this.input = input == null ? "" : input.replaceAll("\\s+", "");
        }

        private BigDecimal parse() {
            BigDecimal value = parseExpression();
            if (index != input.length()) {
                throw new IllegalArgumentException("Unexpected input");
            }
            return value;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();

            while (hasMore()) {
                char operator = peek();
                if (operator == '+' || operator == '-') {
                    index++;
                    BigDecimal right = parseTerm();
                    value = operator == '+' ? value.add(right) : value.subtract(right);
                } else {
                    break;
                }
            }

            return value;
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();

            while (hasMore()) {
                char operator = peek();
                if (operator == '*' || operator == '/') {
                    index++;
                    BigDecimal right = parseFactor();
                    if (operator == '*') {
                        value = value.multiply(right);
                    } else {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw new IllegalArgumentException("Division by zero");
                        }
                        value = value.divide(right, new MathContext(12, RoundingMode.HALF_UP));
                    }
                } else {
                    break;
                }
            }

            return value;
        }

        private BigDecimal parseFactor() {
            if (!hasMore()) {
                throw new IllegalArgumentException("Missing value");
            }

            char current = peek();
            if (current == '(') {
                index++;
                BigDecimal value = parseExpression();
                if (!hasMore() || peek() != ')') {
                    throw new IllegalArgumentException("Missing closing bracket");
                }
                index++;
                return value;
            }

            if (current == '+' || current == '-') {
                index++;
                BigDecimal value = parseFactor();
                return current == '-' ? value.negate() : value;
            }

            return parseNumber();
        }

        private BigDecimal parseNumber() {
            int start = index;
            boolean dotSeen = false;

            while (hasMore()) {
                char current = peek();
                if (Character.isDigit(current)) {
                    index++;
                    continue;
                }
                if (current == '.') {
                    if (dotSeen) {
                        throw new IllegalArgumentException("Invalid number");
                    }
                    dotSeen = true;
                    index++;
                    continue;
                }
                break;
            }

            if (start == index) {
                throw new IllegalArgumentException("Expected number");
            }

            return new BigDecimal(input.substring(start, index));
        }

        private boolean hasMore() {
            return index < input.length();
        }

        private char peek() {
            return input.charAt(index);
        }
    }
}
