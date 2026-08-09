package com.propcycle.app.data.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AuthInputValidatorTest {

    @Test
    public void login_acceptsTrimmedEmailAndValidPassword() {
        AuthInputValidator.ValidationResult result =
                AuthInputValidator.validateLogin("  member@example.com  ", "secret1");

        assertTrue(result.isValid());
    }

    @Test
    public void login_rejectsMalformedEmail() {
        AuthInputValidator.ValidationResult result =
                AuthInputValidator.validateLogin("not-an-email", "secret1");

        assertFalse(result.isValid());
        assertEquals("Enter a valid email address.", result.getMessage());
    }

    @Test
    public void login_rejectsShortPassword() {
        AuthInputValidator.ValidationResult result =
                AuthInputValidator.validateLogin("member@example.com", "12345");

        assertFalse(result.isValid());
        assertEquals("Password must be at least 6 characters.", result.getMessage());
    }

    @Test
    public void registration_requiresDisplayName() {
        AuthInputValidator.ValidationResult result =
                AuthInputValidator.validateRegistration(
                        "   ", "member@example.com", "secret1");

        assertFalse(result.isValid());
        assertEquals("Enter your full name.", result.getMessage());
    }

    @Test
    public void registration_rejectsOverlongDisplayName() {
        AuthInputValidator.ValidationResult result =
                AuthInputValidator.validateRegistration(
                        "A".repeat(81), "member@example.com", "secret1");

        assertFalse(result.isValid());
        assertEquals("Full name must be 80 characters or fewer.", result.getMessage());
    }

    @Test
    public void registration_acceptsValidInputs() {
        AuthInputValidator.ValidationResult result =
                AuthInputValidator.validateRegistration(
                        "Aisyah Rahman", "aisyah@example.com", "a-safe-password");

        assertTrue(result.isValid());
    }
}
