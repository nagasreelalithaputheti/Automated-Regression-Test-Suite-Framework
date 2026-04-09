package com.example.framework.api;

import com.example.framework.model.TestResult;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * REST-Assured API tests against JSONPlaceholder (free public API).
 *
 * Tests three real HTTP endpoints:
 *  1. GET  /posts/1       — fetch a single post, assert 200 + title field
 *  2. POST /posts         — create a post, assert 201 + returned body
 *  3. GET  /users/1       — fetch a user, assert username field present
 *
 * Each test method returns a TestResult that gets persisted to the DB.
 */
@Slf4j
@Component
public class SampleApiTest {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    // ─────────────────────────────────────────────────────────────
    // Test 1: GET /posts/1
    // ─────────────────────────────────────────────────────────────

    /**
     * Verifies that fetching post #1 returns HTTP 200
     * and that the response body contains a "title" field.
     */
    public TestResult runGetPostTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "GET /posts/1 — Fetch Single Post";

        try {
            log.info("[API] Running: {} on thread: {}", testName,
                    Thread.currentThread().getName());

            Response response = RestAssured
                    .given()
                        .baseUri(BASE_URL)
                        .header("Accept", "application/json")
                    .when()
                        .get("/posts/1")
                    .then()
                        .extract().response();

            // ── Assertions ──────────────────────────────────────────
            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                throw new AssertionError(
                        "Expected 200 but got " + statusCode);
            }

            String title = response.jsonPath().getString("title");
            if (title == null || title.isBlank()) {
                throw new AssertionError(
                        "Response body missing 'title' field");
            }

            int userId = response.jsonPath().getInt("userId");
            if (userId != 1) {
                throw new AssertionError(
                        "Expected userId=1 but got " + userId);
            }

            log.info("[API] PASSED — status={}, title=\"{}\"", statusCode, title);

            return buildResult(testName, "PASSED", null,
                    System.currentTimeMillis() - start, runId);

        } catch (Exception e) {
            log.error("[API] FAILED — {}: {}", testName, e.getMessage());
            return buildResult(testName, "FAILED", e.getMessage(),
                    System.currentTimeMillis() - start, runId);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Test 2: POST /posts
    // ─────────────────────────────────────────────────────────────

    /**
     * Verifies that creating a new post returns HTTP 201
     * and that the response echoes back the posted body with an assigned id.
     */
    public TestResult runCreatePostTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "POST /posts — Create New Post";

        try {
            log.info("[API] Running: {} on thread: {}", testName,
                    Thread.currentThread().getName());

            String requestBody = """
                    {
                      "title":  "Automation Framework Test",
                      "body":   "Created by REST-Assured in parallel test run",
                      "userId": 1
                    }
                    """;

            Response response = RestAssured
                    .given()
                        .baseUri(BASE_URL)
                        .header("Content-Type", "application/json")
                        .header("Accept",       "application/json")
                        .body(requestBody)
                    .when()
                        .post("/posts")
                    .then()
                        .extract().response();

            // ── Assertions ──────────────────────────────────────────
            int statusCode = response.getStatusCode();
            if (statusCode != 201) {
                throw new AssertionError(
                        "Expected 201 Created but got " + statusCode);
            }

            String returnedTitle = response.jsonPath().getString("title");
            if (!"Automation Framework Test".equals(returnedTitle)) {
                throw new AssertionError(
                        "Returned title mismatch: " + returnedTitle);
            }

            int newId = response.jsonPath().getInt("id");
            log.info("[API] PASSED — status={}, new post id={}", statusCode, newId);

            return buildResult(testName, "PASSED", null,
                    System.currentTimeMillis() - start, runId);

        } catch (Exception e) {
            log.error("[API] FAILED — {}: {}", testName, e.getMessage());
            return buildResult(testName, "FAILED", e.getMessage(),
                    System.currentTimeMillis() - start, runId);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Test 3: GET /users/1
    // ─────────────────────────────────────────────────────────────

    /**
     * Verifies that fetching user #1 returns HTTP 200
     * and that the response body includes a non-empty "username".
     */
    public TestResult runGetUserTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "GET /users/1 — Fetch User Profile";

        try {
            log.info("[API] Running: {} on thread: {}", testName,
                    Thread.currentThread().getName());

            Response response = RestAssured
                    .given()
                        .baseUri(BASE_URL)
                        .header("Accept", "application/json")
                    .when()
                        .get("/users/1")
                    .then()
                        .extract().response();

            // ── Assertions ──────────────────────────────────────────
            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                throw new AssertionError(
                        "Expected 200 but got " + statusCode);
            }

            String username = response.jsonPath().getString("username");
            if (username == null || username.isBlank()) {
                throw new AssertionError(
                        "Response body missing 'username' field");
            }

            String email = response.jsonPath().getString("email");
            log.info("[API] PASSED — status={}, username={}, email={}",
                    statusCode, username, email);

            return buildResult(testName, "PASSED", null,
                    System.currentTimeMillis() - start, runId);

        } catch (Exception e) {
            log.error("[API] FAILED — {}: {}", testName, e.getMessage());
            return buildResult(testName, "FAILED", e.getMessage(),
                    System.currentTimeMillis() - start, runId);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private TestResult buildResult(String name, String status,
                                   String error, long duration, String runId) {
        return TestResult.builder()
                .testName(name)
                .testType("API")
                .status(status)
                .durationMs(duration)
                .errorMessage(error)
                .threadName(Thread.currentThread().getName())
                .executedAt(LocalDateTime.now())
                .runId(runId)
                .build();
    }
}
