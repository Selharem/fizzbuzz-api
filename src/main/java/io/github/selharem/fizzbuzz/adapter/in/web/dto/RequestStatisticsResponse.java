package io.github.selharem.fizzbuzz.adapter.in.web.dto;

import io.github.selharem.fizzbuzz.domain.model.RequestStatistics;

public record RequestStatisticsResponse(
        int int1,
        int int2,
        int limit,
        String str1,
        String str2,
        long hits
) {

    public static RequestStatisticsResponse from(RequestStatistics statistics) {
        var parameters = statistics.parameters();
        return new RequestStatisticsResponse(
                parameters.firstDivisor(),
                parameters.secondDivisor(),
                parameters.limit(),
                parameters.firstReplacement(),
                parameters.secondReplacement(),
                statistics.hits());
    }
}
