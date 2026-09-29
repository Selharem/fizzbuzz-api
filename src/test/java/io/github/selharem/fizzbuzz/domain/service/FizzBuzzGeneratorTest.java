package io.github.selharem.fizzbuzz.domain.service;

import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FizzBuzzGeneratorTest {

    private final FizzBuzzGenerator generator = new FizzBuzzGenerator();

    @Test
    void generatesValuesForBothDivisorsAndOrdinaryNumbers() {
        var parameters = new FizzBuzzParameters(3, 5, 16, "fizz", "buzz");

        assertThat(generator.generate(parameters))
                .containsExactly(
                        "1", "2", "fizz", "4", "buzz", "fizz", "7", "8",
                        "fizz", "buzz", "11", "fizz", "13", "14", "fizzbuzz", "16");
    }

    @Test
    void handlesEqualDivisorsUsingBothReplacementStrings() {
        var parameters = new FizzBuzzParameters(2, 2, 4, "foo", "bar");

        assertThat(generator.generate(parameters))
                .containsExactly("1", "foobar", "3", "foobar");
    }

    @Test
    void returnsAnImmutableResult() {
        var result = generator.generate(new FizzBuzzParameters(2, 3, 3, "foo", "bar"));

        assertThat(result).isUnmodifiable();
    }
}
