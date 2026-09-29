package io.github.selharem.fizzbuzz.application.port.out;

import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;
import io.github.selharem.fizzbuzz.domain.model.RequestStatistics;

import java.util.Optional;

public interface RequestStatisticsPort {

    /**
     * Atomically increments the hit counter of the given parameter set, creating it on first use.
     */
    void record(FizzBuzzParameters parameters);

    /**
     * Returns the parameter set with the most hits; ties go to the one recorded first.
     */
    Optional<RequestStatistics> findMostFrequent();
}
