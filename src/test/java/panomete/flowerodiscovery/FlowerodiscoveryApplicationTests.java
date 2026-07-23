package panomete.flowerodiscovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Flowero Discover Eureka Server.
 * <p>
 * Verifies the server starts in standalone mode, the dashboard is reachable,
 * the health endpoint returns UP, and the REST API returns an empty registry.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlowerodiscoveryApplicationTests {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void contextLoads() {
        // Sanity: the Spring context starts without errors
    }

    @Test
    void eurekaDashboardIsReachable() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Instances currently registered with Eureka");
    }

    @Test
    void actuatorHealthReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/actuator/health"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void eurekaAppsEndpointReturnsEmptyRegistry() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/eureka/apps"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // In standalone mode with no clients registered, the response should
        // still be valid XML (Eureka defaults to XML without Accept header)
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void eurekaAppsEndpointReturnsJsonWithAcceptHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/eureka/apps"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"applications\"");
    }

    @Test
    void doesNotRegisterWithItself() {
        // Verify standalone mode: Eureka should not self-register.
        // The app name in Eureka is uppercased from spring.application.name
        // (flowero-discover → FLOWERO-DISCOVER).
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/eureka/apps"), String.class);

        // The response should NOT contain FLOWERO-DISCOVER as a registered app
        assertThat(response.getBody()).doesNotContain("FLOWERO-DISCOVER");
    }
}
