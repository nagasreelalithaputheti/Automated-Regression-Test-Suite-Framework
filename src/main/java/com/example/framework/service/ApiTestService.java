package com.example.framework.service;

import com.example.framework.model.TestResult;
import com.example.framework.util.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for executing REST API tests.
 * Uses REST Assured for HTTP requests.
 * Logs test execution to file via LoggerUtil.
 */
@Slf4j
@Service
public class ApiTestService {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    
    private final LoggerUtil loggerUtil;

    public ApiTestService(LoggerUtil loggerUtil) {
        this.loggerUtil = loggerUtil;
    }

    /**
     * Runs all API tests and returns their results.
     */
    public List<TestResult> runAllApiTests(String runId) {
        List<TestResult> results = new ArrayList<>();
        
        results.add(runGetPostTest(runId));
        results.add(runCreatePostTest(runId));
        results.add(runGetUserTest(runId));
        
        return results;
    }

    /**
     * Test 1: GET /posts/1 — Fetch a single post
     * Verifies HTTP 200 and presence of title field.
     */
    public TestResult runGetPostTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "GET /posts/1 — Fetch Single Post";
        
        loggerUtil.logApiTestStart(testName);
        
        try {
            Response response = RestAssured
                    .given()
                        .baseUri(BASE_URL)
                        .header("Accept", "application/json")
                    .when()
                        .get("/posts/1")
                    .then()
                        .extract().response();

            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                throw new AssertionError("Expected 200 but got " + statusCode);
            }

            String title = response.jsonPath().getString("title");
            if (title == null || title.isBlank()) {
                throw new AssertionError("Response body missing 'title' field");
            }

            long duration = System.currentTimeMillis() - start;
            loggerUtil.logApiTestPassed(testName, duration);

            return TestResult.builder()
                    .testName(testName)
                    .testType("API")
                    .status("PASSED")
                    .durationMs(duration)
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            loggerUtil.logApiTestFailed(testName, e.getMessage());
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("API")
                    .status("FAILED")
                    .durationMs(duration)
                    .errorMessage(e.getMessage())
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
        }
    }

    /**
     * Test 2: POST /posts — Create a new post
     * Verifies HTTP 201 and echo of posted data.
     */
    public TestResult runCreatePostTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "POST /posts — Create New Post";
        
        loggerUtil.logApiTestStart(testName);
        
        try {
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
                        .header("Accept", "application/json")
                        .body(requestBody)
                    .when()
                        .post("/posts")
                    .then()
                        .extract().response();

            int statusCode = response.getStatusCode();
            if (statusCode != 201) {
                throw new AssertionError("Expected 201 Created but got " + statusCode);
            }

            String returnedTitle = response.jsonPath().getString("title");
            if (!"Automation Framework Test".equals(returnedTitle)) {
                throw new AssertionError("Returned title mismatch: " + returnedTitle);
            }

            long duration = System.currentTimeMillis() - start;
            loggerUtil.logApiTestPassed(testName, duration);

            return TestResult.builder()
                    .testName(testName)
                    .testType("API")
                    .status("PASSED")
                    .durationMs(duration)
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            loggerUtil.logApiTestFailed(testName, e.getMessage());
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("API")
                    .status("FAILED")
                    .durationMs(duration)
                    .errorMessage(e.getMessage())
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
        }
    }

    /**
     * Test 3: GET /users/1 — Fetch user profile
     * Verifies HTTP 200 and presence of username field.
     */
    public TestResult runGetUserTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "GET /users/1 — Fetch User Profile";
        
        loggerUtil.logApiTestStart(testName);
        
        try {
            Response response = RestAssured
                    .given()
                        .baseUri(BASE_URL)
                        .header("Accept", "application/json")
                    .when()
                        .get("/users/1")
                    .then()
                        .extract().response();

            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                throw new AssertionError("Expected 200 but got " + statusCode);
            }

            String username = response.jsonPath().getString("username");
            if (username == null || username.isBlank()) {
                throw new AssertionError("Response body missing 'username' field");
            }

            long duration = System.currentTimeMillis() - start;
            loggerUtil.logApiTestPassed(testName, duration);

            return TestResult.builder()
                    .testName(testName)
                    .testType("API")
                    .status("PASSED")
                    .durationMs(duration)
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            loggerUtil.logApiTestFailed(testName, e.getMessage());
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("API")
                    .status("FAILED")
                    .durationMs(duration)
                    .errorMessage(e.getMessage())
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
        }
    }
}

