package io.github.selharem.fizzbuzz.application.port.in;

import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;

import java.util.List;

public interface GenerateFizzBuzzUseCase {

    List<String> generate(FizzBuzzParameters parameters);
}
