package io.github.selharem.fizzbuzz.domain.service;

import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;

import java.util.List;
import java.util.stream.IntStream;

public final class FizzBuzzGenerator {

    public List<String> generate(FizzBuzzParameters parameters) {
        return IntStream.rangeClosed(1, parameters.limit())
                .mapToObj(number -> valueFor(number, parameters))
                .toList();
    }

    private String valueFor(int number, FizzBuzzParameters parameters) {
        boolean matchesFirst = number % parameters.firstDivisor() == 0;
        boolean matchesSecond = number % parameters.secondDivisor() == 0;

        if (matchesFirst && matchesSecond) {
            return parameters.firstReplacement() + parameters.secondReplacement();
        }
        if (matchesFirst) {
            return parameters.firstReplacement();
        }
        if (matchesSecond) {
            return parameters.secondReplacement();
        }
        return Integer.toString(number);
    }
}
