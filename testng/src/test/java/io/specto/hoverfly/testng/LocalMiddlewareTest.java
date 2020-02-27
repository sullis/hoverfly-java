package io.specto.hoverfly.testng;

import io.specto.hoverfly.testng.api.TestNGClassRule;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static io.specto.hoverfly.junit.core.HoverflyConfig.localConfigs;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Listeners(HoverflyExecutor.class)
public class LocalMiddlewareTest {

    @TestNGClassRule
    public static HoverflyTestNG hoverflyTestNG = HoverflyTestNG.inSimulationMode(dsl(
            service("www.other-anotherservice.com")
                    .put("/api/bookings/1").body("{\"flightId\": \"1\", \"class\": \"PREMIUM\"}")
                    .willReturn(success())),
            localConfigs().localMiddleware("python", "middleware/middleware.py")).printSimulationData();

    private final RestTemplate restTemplate = new RestTemplate();

    private static void requirePython() {
        try {
            Runtime.getRuntime().exec("python");
        } catch (IOException e) {
            throw new IllegalStateException("Required executable 'python' not available. Please install prior to executing this test.");
        }
    }

    @Test
    public void shouldBeAbleToChangeStatusCodeUsingHoverflyLocalMiddleware() throws URISyntaxException {
        requirePython();

        // Given
        final RequestEntity<String> bookFlightRequest =
                RequestEntity.put(new URI("http://www.other-anotherservice.com/api/bookings/1"))
                        .contentType(APPLICATION_JSON)
                        .body("{\"flightId\": \"1\", \"class\": \"PREMIUM\"}");

        // When
        final ResponseEntity<String> bookFlightResponse = restTemplate.exchange(bookFlightRequest, String.class);

        // Then
        assertThat(bookFlightResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
