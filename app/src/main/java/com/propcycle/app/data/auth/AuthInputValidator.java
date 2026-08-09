package com.propcycle.app.data.auth;

import java.util.regex.Pattern;

/** Pure Java validation shared by the authentication screens and local tests. */
public final class AuthInputValidator {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_DISPLAY_NAME_LENGTH = 80;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private AuthInputValidator() {
    }

    public static ValidationResult validateLogin(String email, String password) {
        ValidationResult emailResult = validateEmail(email);
        if (!emailResult.isValid()) {
            return emailResult;
        }
        return validatePassword(password);
    }

    public static ValidationResult validateRegistration(
            String displayName,
            String email,
            String password) {
        String normalizedName = normalize(displayName);
        if (normalizedName.isEmpty()) {
            return ValidationResult.error("Enter your full name.");
        }
        if (normalizedName.length() > MAX_DISPLAY_NAME_LENGTH) {
            return ValidationResult.error("Full name must be 80 characters or fewer.");
        }

        ValidationResult emailResult = validateEmail(email);
        if (!emailResult.isValid()) {
            return emailResult;
        }
        return validatePassword(password);
    }

    private static ValidationResult validateEmail(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail.isEmpty()) {
            return ValidationResult.error("Enter your email address.");
        }
        if (normalizedEmail.length() > MAX_EMAIL_LENGTH
                || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            return ValidationResult.error("Enter a valid email address.");
        }
        return ValidationResult.valid();
    }

    private static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Enter your password.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.error("Password must be at least 6 characters.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return ValidationResult.error("Password must be 128 characters or fewer.");
        }
        return ValidationResult.valid();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ValidationResult {

        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
