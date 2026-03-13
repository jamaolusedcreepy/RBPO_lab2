package com.support.security;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                "Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException(
                "Password must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException(
                "Password must contain at least one special character (!@#$%^&* etc.)");
        }
    }
}
