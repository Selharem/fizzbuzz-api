package io.github.selharem.fizzbuzz.adapter.in.web;

import io.github.selharem.fizzbuzz.adapter.in.web.dto.FizzBuzzRequest;
import io.github.selharem.fizzbuzz.adapter.in.web.dto.RequestStatisticsResponse;
import io.github.selharem.fizzbuzz.application.port.in.GenerateFizzBuzzUseCase;
import io.github.selharem.fizzbuzz.application.port.in.GetMostFrequentRequestUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "FizzBuzz")
public class FizzBuzzController {

    private final GenerateFizzBuzzUseCase generateFizzBuzz;
    private final GetMostFrequentRequestUseCase getMostFrequentRequest;

    public FizzBuzzController(
            GenerateFizzBuzzUseCase generateFizzBuzz,
            GetMostFrequentRequestUseCase getMostFrequentRequest
    ) {
        this.generateFizzBuzz = generateFizzBuzz;
        this.getMostFrequentRequest = getMostFrequentRequest;
    }

    @GetMapping("/fizzbuzz")
    @Operation(summary = "Generate a configurable FizzBuzz sequence")
    @ApiResponse(responseCode = "200", description = "Sequence from 1 to limit")
    @ApiResponse(responseCode = "400", description = "Invalid or missing parameters")
    public List<String> fizzBuzz(@Valid @ParameterObject FizzBuzzRequest request) {
        return generateFizzBuzz.generate(request.toParameters());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get the most frequent FizzBuzz request and its hit count")
    @ApiResponse(responseCode = "200", description = "Most frequent request")
    @ApiResponse(responseCode = "204", description = "No request has been recorded yet")
    public ResponseEntity<RequestStatisticsResponse> statistics() {
        return getMostFrequentRequest.getMostFrequentRequest()
                .map(RequestStatisticsResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
