package io.github.selharem.fizzbuzz.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class FizzBuzzParametersTest {

    @ParameterizedTest
    @CsvSource({
        "0, 5, 10, firstDivisor",
        "3, -1, 10, secondDivisor",
        "3, 5, 0, limit"
    })
    void rejectsNonPositiveNumbers(int firstDivisor, int secondDivisor, int limit, String field) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FizzBuzzParameters(firstDivisor, secondDivisor, limit, "fizz", "buzz"))
                .withMessage(field + " must be positive");
    }

    @Test
    void rejectsMissingReplacements() {
        assertThatNullPointerException()
                .isThrownBy(() -> new FizzBuzzParameters(3, 5, 10, null, "buzz"));
        assertThatNullPointerException()
                .isThrownBy(() -> new FizzBuzzParameters(3, 5, 10, "fizz", null));
    }

    @Test
    void rejectsStatisticsWithoutHits() {
        var parameters = new FizzBuzzParameters(3, 5, 10, "fizz", "buzz");

        assertThatIllegalArgumentException().isThrownBy(() -> new RequestStatistics(parameters, 0));
    }
}
