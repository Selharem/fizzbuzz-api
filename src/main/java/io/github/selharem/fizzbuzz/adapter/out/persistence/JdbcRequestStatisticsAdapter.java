package io.github.selharem.fizzbuzz.adapter.out.persistence;

import io.github.selharem.fizzbuzz.application.port.out.RequestStatisticsPort;
import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;
import io.github.selharem.fizzbuzz.domain.model.RequestStatistics;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcRequestStatisticsAdapter implements RequestStatisticsPort {

    private final JdbcClient jdbcClient;

    public JdbcRequestStatisticsAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void record(FizzBuzzParameters parameters) {
        jdbcClient.sql("""
                        INSERT INTO request_statistics (
                            int1, int2, request_limit, str1, str2, hits
                        )
                        VALUES (
                            :int1, :int2, :requestLimit, :str1, :str2, 1
                        )
                        ON CONFLICT (int1, int2, request_limit, str1, str2)
                        DO UPDATE SET
                            hits = request_statistics.hits + 1,
                            last_requested_at = CURRENT_TIMESTAMP
                        """)
                .param("int1", parameters.firstDivisor())
                .param("int2", parameters.secondDivisor())
                .param("requestLimit", parameters.limit())
                .param("str1", parameters.firstReplacement())
                .param("str2", parameters.secondReplacement())
                .update();
    }

    @Override
    public Optional<RequestStatistics> findMostFrequent() {
        return jdbcClient.sql("""
                        SELECT int1, int2, request_limit, str1, str2, hits
                        FROM request_statistics
                        ORDER BY hits DESC, first_requested_at ASC, id ASC
                        LIMIT 1
                        """)
                .query((resultSet, rowNumber) -> new RequestStatistics(
                        new FizzBuzzParameters(
                                resultSet.getInt("int1"),
                                resultSet.getInt("int2"),
                                resultSet.getInt("request_limit"),
                                resultSet.getString("str1"),
                                resultSet.getString("str2")),
                        resultSet.getLong("hits")))
                .optional();
    }
}
