package com.example.framework.controller;

import com.example.framework.execution.TestExecutor;
import com.example.framework.model.TestResult;
import com.example.framework.repository.TestResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for frontend dashboard test execution endpoints.
 * Provides endpoints for running UI tests, API tests, and all tests.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Dashboard Test API",
     description = "API endpoints for the web dashboard to trigger test execution")
public class DashboardTestController {

    private final TestExecutor testExecutor;
    private final TestResultRepository testResultRepository;
    
    // Track execution status
    private static final Map<String, Map<String, Object>> executionStatus = new ConcurrentHashMap<>();

    /**
     * POST /api/test/run-ui - Run UI (Selenium) tests only
     */
    @Operation(
        summary = "Run UI Tests",
        description = "Executes all UI tests using Selenium (e.g., GoogleSearchTest)"
    )
    @PostMapping("/run-ui")
    public ResponseEntity<Map<String, Object>> runUITests() {
        try {
            String testId = UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> response = new HashMap<>();
            response.put("testId", testId);
            response.put("status", "running");
            response.put("message", "UI Tests are running...");
            response.put("timestamp", System.currentTimeMillis());
            response.put("testType", "UI");
            
            // Run tests asynchronously and update status
            new Thread(() -> {
                try {
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "running");
                        put("progress", 0);
                        put("testType", "UI");
                    }});
                    
                    List<TestResult> results = testExecutor.runUITestsOnly();
                    
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "completed");
                        put("progress", 100);
                        put("results", results);
                        put("testType", "UI");
                        put("totalTests", results.size());
                        put("passedTests", results.stream().filter(r -> "PASSED".equals(r.getStatus())).count());
                        put("failedTests", results.stream().filter(r -> "FAILED".equals(r.getStatus())).count());
                    }});
                } catch (Exception e) {
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "error");
                        put("progress", 0);
                        put("message", "Error: " + e.getMessage());
                    }});
                }
            }).start();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error starting UI tests: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * POST /api/test/run-api - Run API (REST-Assured) tests only
     */
    @Operation(
        summary = "Run API Tests",
        description = "Executes all API tests using REST-Assured (e.g., SampleApiTest)"
    )
    @PostMapping("/run-api")
    public ResponseEntity<Map<String, Object>> runAPITests() {
        try {
            String testId = UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> response = new HashMap<>();
            response.put("testId", testId);
            response.put("status", "running");
            response.put("message", "API Tests are running...");
            response.put("timestamp", System.currentTimeMillis());
            response.put("testType", "API");
            
            // Run tests asynchronously and update status
            new Thread(() -> {
                try {
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "running");
                        put("progress", 0);
                        put("testType", "API");
                    }});
                    
                    List<TestResult> results = testExecutor.runApiTestsOnly();
                    
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "completed");
                        put("progress", 100);
                        put("results", results);
                        put("testType", "API");
                        put("totalTests", results.size());
                        put("passedTests", results.stream().filter(r -> "PASSED".equals(r.getStatus())).count());
                        put("failedTests", results.stream().filter(r -> "FAILED".equals(r.getStatus())).count());
                    }});
                } catch (Exception e) {
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "error");
                        put("progress", 0);
                        put("message", "Error: " + e.getMessage());
                    }});
                }
            }).start();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error starting API tests: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * POST /api/test/run-all - Run both UI and API tests (parallel)
     */
    @Operation(
        summary = "Run All Tests",
        description = "Executes both UI and API tests in parallel"
    )
    @PostMapping("/run-all")
    public ResponseEntity<Map<String, Object>> runAllTests() {
        try {
            String testId = UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> response = new HashMap<>();
            response.put("testId", testId);
            response.put("status", "running");
            response.put("message", "All Tests are running...");
            response.put("timestamp", System.currentTimeMillis());
            response.put("testType", "ALL");
            
            // Run tests asynchronously and update status
            new Thread(() -> {
                try {
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "running");
                        put("progress", 0);
                        put("testType", "ALL");
                    }});
                    
                    List<TestResult> results = testExecutor.runAllTests();
                    
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "completed");
                        put("progress", 100);
                        put("results", results);
                        put("testType", "ALL");
                        put("totalTests", results.size());
                        put("passedTests", results.stream().filter(r -> "PASSED".equals(r.getStatus())).count());
                        put("failedTests", results.stream().filter(r -> "FAILED".equals(r.getStatus())).count());
                    }});
                } catch (Exception e) {
                    executionStatus.put(testId, new HashMap<String, Object>() {{
                        put("status", "error");
                        put("progress", 0);
                        put("message", "Error: " + e.getMessage());
                    }});
                }
            }).start();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error starting all tests: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * GET /api/test/status/{testId} - Get test execution status
     */
    @Operation(
        summary = "Get Test Status",
        description = "Returns the status of a specific test execution"
    )
    @GetMapping("/status/{testId}")
    public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable String testId) {
        Map<String, Object> status = executionStatus.getOrDefault(testId, new HashMap<String, Object>() {{
            put("status", "not_found");
            put("message", "Test execution not found");
        }});
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/test/results - Get all recent test results
     */
    @Operation(
        summary = "Get Test Results",
        description = "Returns all stored test results from the database"
    )
    @GetMapping("/results")
    public ResponseEntity<List<TestResult>> getTestResults() {
        List<TestResult> results = testResultRepository.findAll();
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/test/summary - Get test execution summary
     */
    @Operation(
        summary = "Get Test Summary",
        description = "Returns aggregate statistics of all test executions"
    )
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTestSummary() {
        List<TestResult> allResults = testResultRepository.findAll();
        
        long total = allResults.size();
        long passed = allResults.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = total - passed;
        double successRate = total > 0 ? (passed * 100.0 / total) : 0;
        long totalDuration = allResults.stream().mapToLong(TestResult::getDurationMs).sum();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTests", total);
        summary.put("passedTests", passed);
        summary.put("failedTests", failed);
        summary.put("successRate", String.format("%.2f", successRate));
        summary.put("totalDuration", totalDuration);
        summary.put("averageDuration", total > 0 ? totalDuration / total : 0);
        
        return ResponseEntity.ok(summary);
    }
}
