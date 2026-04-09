package com.example.framework.controller;

import com.example.framework.execution.TestExecutor;
import com.example.framework.model.TestResult;
import com.example.framework.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing all test execution and results endpoints.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  POST  /api/tests/run              — run all tests           │
 * │  POST  /run-tests                   — run all tests (alt)     │
 * │  GET   /api/tests/results          — all stored results      │
 * │  GET   /api/tests/results/{runId}  — results for one run     │
 * │  GET   /api/tests/results/passed   — only passed tests       │
 * │  GET   /api/tests/results/failed   — only failed tests       │
 * │  GET   /api/tests/summary          — aggregate stats         │
 * └─────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Test Execution API",
     description = "Trigger test runs and query results via REST")
public class TestController {

    private final TestExecutionService testExecutionService;
    private final TestExecutor testExecutor;

    // ─────────────────────────────────────────────────────────────
    // TRIGGER
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Run all tests in parallel",
        description = "Executes GoogleSearchTest (Selenium) + 3 API tests (REST-Assured) " +
                      "concurrently using a thread pool. Saves results to DB and generates " +
                      "an HTML report. Returns all TestResult objects when complete."
    )
    @PostMapping("/run")
    public ResponseEntity<List<TestResult>> runTests() {
        List<TestResult> results = testExecutionService.runAllTests();
        return ResponseEntity.ok(results);
    }

    /**
     * POST /run-tests - Alternative endpoint to run all tests.
     * This is the main endpoint as specified in requirements.
     */
    @Operation(
        summary     = "Run all tests (POST /run-tests)",
        description = "Executes all tests (API + UI) in parallel. Returns test results."
    )
    @PostMapping("/run-tests")
    public ResponseEntity<List<TestResult>> runTestsEndpoint() {
        List<TestResult> results = testExecutor.runAllTests();
        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/tests/run-tests - Same as above but with /api/tests prefix
     */
    @PostMapping("/api/tests/run-tests")
    public ResponseEntity<List<TestResult>> runTestsWithPrefix() {
        List<TestResult> results = testExecutor.runAllTests();
        return ResponseEntity.ok(results);
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY RESULTS
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get all stored test results (most recent first)")
    @GetMapping("/results")
    public ResponseEntity<List<TestResult>> getAllResults() {
        return ResponseEntity.ok(testExecutionService.getAllResults());
    }

    @Operation(summary = "Get results for a specific run by runId")
    @GetMapping("/results/{runId}")
    public ResponseEntity<List<TestResult>> getResultsByRunId(@PathVariable String runId) {
        List<TestResult> results = testExecutionService.getResultsByRunId(runId);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Get only PASSED test results")
    @GetMapping("/results/passed")
    public ResponseEntity<List<TestResult>> getPassedResults() {
        return ResponseEntity.ok(testExecutionService.getPassedResults());
    }

    @Operation(summary = "Get only FAILED test results")
    @GetMapping("/results/failed")
    public ResponseEntity<List<TestResult>> getFailedResults() {
        return ResponseEntity.ok(testExecutionService.getFailedResults());
    }

    // ─────────────────────────────────────────────────────────────
    // SUMMARY
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Get aggregate test statistics",
        description = "Returns total tests, passed, failed, pass rate, " +
                      "and average durations by test type."
    )
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(testExecutionService.getSummary());
    }

    // ─────────────────────────────────────────────────────────────
    // HEALTH
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",  "UP",
                "app",     "automation-framework",
                "version", "1.0.0"
        ));
    }
}
