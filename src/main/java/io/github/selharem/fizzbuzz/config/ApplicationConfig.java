package io.github.selharem.fizzbuzz.config;

import io.github.selharem.fizzbuzz.application.port.out.RequestStatisticsPort;
import io.github.selharem.fizzbuzz.application.service.FizzBuzzService;
import io.github.selharem.fizzbuzz.domain.service.FizzBuzzGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Wires the framework-free domain and application core into the Spring context.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {

    @Bean
    public static RequiredDatabasePasswordCheck requiredDatabasePasswordCheck(Environment environment) {
        return new RequiredDatabasePasswordCheck(environment);
    }

    @Bean
    public FizzBuzzGenerator fizzBuzzGenerator() {
        return new FizzBuzzGenerator();
    }

    @Bean
    public FizzBuzzService fizzBuzzService(FizzBuzzGenerator generator, RequestStatisticsPort requestStatistics) {
        return new FizzBuzzService(generator, requestStatistics);
    }
}
