package io.github.selharem.fizzbuzz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI fizzBuzzOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FizzBuzz API")
                .version("v1")
                .description("Configurable FizzBuzz generator with request statistics."));
    }
}
