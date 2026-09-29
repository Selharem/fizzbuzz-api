package io.github.selharem.fizzbuzz.domain.model;

import java.util.Objects;

public record FizzBuzzParameters(
        int firstDivisor,
        int secondDivisor,
        int limit,
        String firstReplacement,
        String secondReplacement
) {

    public FizzBuzzParameters {
        requirePositive(firstDivisor, "firstDivisor");
        requirePositive(secondDivisor, "secondDivisor");
        requirePositive(limit, "limit");
        Objects.requireNonNull(firstReplacement, "firstReplacement must not be null");
        Objects.requireNonNull(secondReplacement, "secondReplacement must not be null");
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
