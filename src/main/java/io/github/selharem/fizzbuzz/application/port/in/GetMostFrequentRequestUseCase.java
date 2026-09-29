package io.github.selharem.fizzbuzz.application.port.in;

import io.github.selharem.fizzbuzz.domain.model.RequestStatistics;

import java.util.Optional;

public interface GetMostFrequentRequestUseCase {

    Optional<RequestStatistics> getMostFrequentRequest();
}
