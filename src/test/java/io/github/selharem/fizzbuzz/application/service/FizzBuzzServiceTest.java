package io.github.selharem.fizzbuzz.application.service;

import io.github.selharem.fizzbuzz.application.port.out.RequestStatisticsPort;
import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;
import io.github.selharem.fizzbuzz.domain.model.RequestStatistics;
import io.github.selharem.fizzbuzz.domain.service.FizzBuzzGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FizzBuzzServiceTest {

    private final InMemoryRequestStatistics requestStatistics = new InMemoryRequestStatistics();
    private final FizzBuzzService service = new FizzBuzzService(new FizzBuzzGenerator(), requestStatistics);

    @Test
    void generatesTheSequenceAndRecordsTheRequest() {
        var parameters = new FizzBuzzParameters(3, 5, 3, "fizz", "buzz");

        assertThat(service.generate(parameters)).containsExactly("1", "2", "fizz");
        assertThat(requestStatistics.recorded).containsExactly(parameters);
    }

    @Test
    void doesNotReturnASequenceWhenRecordingFails() {
        var failingService = new FizzBuzzService(new FizzBuzzGenerator(), new FailingRequestStatistics());

        assertThatThrownBy(() -> failingService.generate(new FizzBuzzParameters(3, 5, 3, "fizz", "buzz")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnsTheMostFrequentRequestFromThePort() {
        var statistics = new RequestStatistics(new FizzBuzzParameters(3, 5, 15, "fizz", "buzz"), 7);
        requestStatistics.mostFrequent = Optional.of(statistics);

        assertThat(service.getMostFrequentRequest()).contains(statistics);
    }

    private static final class InMemoryRequestStatistics implements RequestStatisticsPort {

        private final List<FizzBuzzParameters> recorded = new ArrayList<>();
        private Optional<RequestStatistics> mostFrequent = Optional.empty();

        @Override
        public void record(FizzBuzzParameters parameters) {
            recorded.add(parameters);
        }

        @Override
        public Optional<RequestStatistics> findMostFrequent() {
            return mostFrequent;
        }
    }

    private static final class FailingRequestStatistics implements RequestStatisticsPort {

        @Override
        public void record(FizzBuzzParameters parameters) {
            throw new IllegalStateException("storage unavailable");
        }

        @Override
        public Optional<RequestStatistics> findMostFrequent() {
            return Optional.empty();
        }
    }
}
