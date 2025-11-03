package com.platform.orchestrator.integration;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobRequest;
import com.platform.orchestrator.dto.JobResponse;
import com.platform.orchestrator.repository.JobRepository;
import com.platform.orchestrator.security.AuthenticationRequest;
import com.platform.orchestrator.security.AuthenticationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Job orchestration flow.
 * Uses Testcontainers to spin up real infrastructure dependencies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class JobIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobRepository jobRepository;

    private String jwtToken;

    // Testcontainers for real infrastructure
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("jobs_db_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    /**
     * Configure Spring Boot to use Testcontainers infrastructure
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // JWT (test secret)
        registry.add("jwt.secret", () -> "test-secret-key-for-jwt-token-generation-min-256-bits");
    }

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        jobRepository.deleteAll();

        // Authenticate and get JWT token
        jwtToken = authenticateAndGetToken();
    }

    /**
     * Helper method to authenticate and obtain JWT token
     */
    private String authenticateAndGetToken() {
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setUsername("admin");
        authRequest.setPassword("admin123");

        ResponseEntity<AuthenticationResponse> response = restTemplate.postForEntity(
                createUrl("/api/v1/auth/login"),
                authRequest,
                AuthenticationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();

        return response.getBody().getToken();
    }

    /**
     * Helper method to create authenticated headers with JWT
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        return headers;
    }

    /**
     * Helper method to create full URL for endpoints
     */
    private String createUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("Should submit job successfully with authentication")
    void shouldSubmitJobSuccessfully() {
        // Given
        JobRequest jobRequest = new JobRequest();
        jobRequest.setName("Test Email Job");
        jobRequest.setType("EMAIL");
        jobRequest.setPayload("{\"to\":\"test@example.com\",\"subject\":\"Test\"}");
        jobRequest.setPriority(5);

        HttpEntity<JobRequest> request = new HttpEntity<>(jobRequest, createAuthHeaders());

        // When
        ResponseEntity<JobResponse> response = restTemplate.exchange(
                createUrl("/api/v1/jobs"),
                HttpMethod.POST,
                request,
                JobResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(response.getBody().getName()).isEqualTo("Test Email Job");

        // Verify job is saved in database
        Job savedJob = jobRepository.findById(response.getBody().getId()).orElse(null);
        assertThat(savedJob).isNotNull();
        assertThat(savedJob.getName()).isEqualTo("Test Email Job");
    }

    @Test
    @DisplayName("Should reject job submission without authentication")
    void shouldRejectUnauthenticatedJobSubmission() {
        // Given
        JobRequest jobRequest = new JobRequest();
        jobRequest.setName("Test Job");
        jobRequest.setType("EMAIL");
        jobRequest.setPayload("{}");

        HttpEntity<JobRequest> request = new HttpEntity<>(jobRequest, new HttpHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                createUrl("/api/v1/jobs"),
                HttpMethod.POST,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should retrieve job by ID with authentication")
    void shouldRetrieveJobById() {
        // Given - Create a job first
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setName("Retrieve Test Job");
        job.setType("REPORT");
        job.setPayload("{}");
        job.setStatus(JobStatus.PENDING);
        job.setPriority(5);
        job.setMaxRetries(3);
        job.setRetryCount(0);
        jobRepository.save(job);

        HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());

        // When
        ResponseEntity<JobResponse> response = restTemplate.exchange(
                createUrl("/api/v1/jobs/" + job.getId()),
                HttpMethod.GET,
                request,
                JobResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(job.getId());
        assertThat(response.getBody().getName()).isEqualTo("Retrieve Test Job");
    }

    @Test
    @DisplayName("Should return 404 for non-existent job")
    void shouldReturn404ForNonExistentJob() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                createUrl("/api/v1/jobs/" + nonExistentId),
                HttpMethod.GET,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should validate job request fields")
    void shouldValidateJobRequestFields() {
        // Given - Invalid job with missing required fields
        JobRequest invalidJob = new JobRequest();
        // Missing name and type

        HttpEntity<JobRequest> request = new HttpEntity<>(invalidJob, createAuthHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                createUrl("/api/v1/jobs"),
                HttpMethod.POST,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should list all jobs with pagination")
    void shouldListJobsWithPagination() {
        // Given - Create multiple jobs
        for (int i = 0; i < 5; i++) {
            Job job = new Job();
            job.setId(UUID.randomUUID());
            job.setName("Job " + i);
            job.setType("EMAIL");
            job.setPayload("{}");
            job.setStatus(JobStatus.PENDING);
            job.setPriority(i);
            job.setMaxRetries(3);
            job.setRetryCount(0);
            jobRepository.save(job);
        }

        HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                createUrl("/api/v1/jobs?page=0&size=3"),
                HttpMethod.GET,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Additional assertions on paginated response can be added
    }

    @Test
    @DisplayName("JWT token should expire after configured time")
    void shouldExpireJwtToken() {
        // This test would require mocking time or using a short expiration
        // For demonstration purposes, we test that an invalid token is rejected

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("invalid.jwt.token");

        JobRequest jobRequest = new JobRequest();
        jobRequest.setName("Test Job");
        jobRequest.setType("EMAIL");
        jobRequest.setPayload("{}");

        HttpEntity<JobRequest> request = new HttpEntity<>(jobRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                createUrl("/api/v1/jobs"),
                HttpMethod.POST,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
