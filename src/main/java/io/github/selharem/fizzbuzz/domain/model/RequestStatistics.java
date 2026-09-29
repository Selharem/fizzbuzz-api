package io.github.selharem.fizzbuzz.domain.model;

import java.util.Objects;

public record RequestStatistics(
        FizzBuzzParameters parameters,
        long hits
) {

    public RequestStatistics {
        Objects.requireNonNull(parameters, "parameters must not be null");
        if (hits <= 0) {
            throw new IllegalArgumentException("hits must be positive");
        }
    }
}
