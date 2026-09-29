package io.github.selharem.fizzbuzz.application.service;

import io.github.selharem.fizzbuzz.application.port.in.GenerateFizzBuzzUseCase;
import io.github.selharem.fizzbuzz.application.port.in.GetMostFrequentRequestUseCase;
import io.github.selharem.fizzbuzz.application.port.out.RequestStatisticsPort;
import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;
import io.github.selharem.fizzbuzz.domain.model.RequestStatistics;
import io.github.selharem.fizzbuzz.domain.service.FizzBuzzGenerator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FizzBuzzService implements GenerateFizzBuzzUseCase, GetMostFrequentRequestUseCase {

    private final FizzBuzzGenerator generator;
    private final RequestStatisticsPort requestStatistics;

    public FizzBuzzService(FizzBuzzGenerator generator, RequestStatisticsPort requestStatistics) {
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
        this.requestStatistics = Objects.requireNonNull(requestStatistics, "requestStatistics must not be null");
    }

    @Override
    public List<String> generate(FizzBuzzParameters parameters) {
        List<String> result = generator.generate(parameters);
        requestStatistics.record(parameters);
        return result;
    }

    @Override
    public Optional<RequestStatistics> getMostFrequentRequest() {
        return requestStatistics.findMostFrequent();
    }
}
