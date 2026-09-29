package io.github.selharem.fizzbuzz;

import io.github.selharem.fizzbuzz.application.port.out.RequestStatisticsPort;
import io.github.selharem.fizzbuzz.domain.model.FizzBuzzParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "management.server.port=0")
@AutoConfigureMockMvc
@Testcontainers
class FizzBuzzApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private RequestStatisticsPort requestStatistics;

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void clearStatistics() {
        jdbcClient.sql("TRUNCATE TABLE request_statistics RESTART IDENTITY").update();
    }

    @Test
    void returnsFizzBuzzSequenceAndRecordsTheRequest() throws Exception {
        mockMvc.perform(get("/api/v1/fizzbuzz")
                        .queryParam("int1", "3")
                        .queryParam("int2", "5")
                        .queryParam("limit", "16")
                        .queryParam("str1", "fizz")
                        .queryParam("str2", "buzz"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("""
                        ["1","2","fizz","4","buzz","fizz","7","8","fizz","buzz","11","fizz","13","14","fizzbuzz","16"]
                        """));

        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.int1").value(3))
                .andExpect(jsonPath("$.int2").value(5))
                .andExpect(jsonPath("$.limit").value(16))
                .andExpect(jsonPath("$.str1").value("fizz"))
                .andExpect(jsonPath("$.str2").value("buzz"))
                .andExpect(jsonPath("$.hits").value(1));
    }

    @Test
    void countsRepeatedRequestsAndReturnsTheMostFrequentOne() throws Exception {
        for (int hit = 0; hit < 2; hit++) {
            mockMvc.perform(get("/api/v1/fizzbuzz?int1=3&int2=5&limit=15&str1=fizz&str2=buzz"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/fizzbuzz?int1=2&int2=7&limit=20&str1=foo&str2=bar"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.int1").value(3))
                .andExpect(jsonPath("$.hits").value(2));
    }

    @Test
    void recordsConcurrentRequestsWithoutLostUpdates() throws Exception {
        var parameters = new FizzBuzzParameters(3, 5, 100, "fizz", "buzz");
        int requestCount = 50;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int request = 0; request < requestCount; request++) {
                executor.submit(() -> requestStatistics.record(parameters));
            }
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(requestStatistics.findMostFrequent())
                .hasValueSatisfying(statistics -> {
                    assertThat(statistics.parameters()).isEqualTo(parameters);
                    assertThat(statistics.hits()).isEqualTo(requestCount);
                });
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(get("/api/v1/fizzbuzz")
                        .queryParam("int1", "0")
                        .queryParam("int2", "5")
                        .queryParam("limit", "10001")
                        .queryParam("str2", "buzz"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid request parameters"))
                .andExpect(jsonPath("$.errors.int1").exists())
                .andExpect(jsonPath("$.errors.limit").exists())
                .andExpect(jsonPath("$.errors.str1").exists());
    }

    @Test
    void rejectsNonNumericIntegers() throws Exception {
        mockMvc.perform(get("/api/v1/fizzbuzz?int1=3&int2=abc&limit=15&str1=fizz&str2=buzz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.int2").value("must be a valid integer"));
    }

    @Test
    void acceptsReplacementsWithSurroundingWhitespace() throws Exception {
        mockMvc.perform(get("/api/v1/fizzbuzz")
                        .queryParam("int1", "1")
                        .queryParam("int2", "7")
                        .queryParam("limit", "1")
                        .queryParam("str1", " fizz ")
                        .queryParam("str2", "buzz"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [" fizz "]
                        """));
    }

    @Test
    void returnsNoContentBeforeAnyRequestHasBeenRecorded() throws Exception {
        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isNoContent());
    }

    @Test
    void publishesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/fizzbuzz'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/statistics'].get").exists());
    }

    @Test
    void exposesHealthAndPrometheusMetricsOnManagementPortOnly() throws Exception {
        String managementUrl = "http://localhost:" + environment.getRequiredProperty("local.management.port");
        try (var client = HttpClient.newHttpClient()) {
            HttpResponse<String> health = client.send(
                    HttpRequest.newBuilder(URI.create(managementUrl + "/actuator/health")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(health.body()).contains("\"status\":\"UP\"");

            HttpResponse<String> metrics = client.send(
                    HttpRequest.newBuilder(URI.create(managementUrl + "/actuator/prometheus")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(metrics.statusCode()).isEqualTo(200);
            assertThat(metrics.body()).contains("jvm_info");
        }

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }
}
