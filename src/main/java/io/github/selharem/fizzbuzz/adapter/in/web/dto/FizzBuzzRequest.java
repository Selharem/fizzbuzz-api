package io.github.selharem.fizzbuzz.adapter.in.web.dto;

import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FizzBuzzRequest(
        @Schema(description = "First divisor", example = "3")
        @NotNull @Positive Integer int1,
        @Schema(description = "Second divisor", example = "5")
        @NotNull @Positive Integer int2,
        @Schema(description = "Last number of the sequence", example = "100")
        @NotNull @Min(1) @Max(MAX_LIMIT) Integer limit,
        @Schema(description = "Replacement for multiples of int1", example = "fizz")
        @NotNull @Size(min = 1, max = MAX_STRING_LENGTH) String str1,
        @Schema(description = "Replacement for multiples of int2", example = "buzz")
        @NotNull @Size(min = 1, max = MAX_STRING_LENGTH) String str2
) {

    public static final int MAX_LIMIT = 10_000;
    public static final int MAX_STRING_LENGTH = 100;

    public FizzBuzzParameters toParameters() {
        return new FizzBuzzParameters(int1, int2, limit, str1, str2);
    }
}
