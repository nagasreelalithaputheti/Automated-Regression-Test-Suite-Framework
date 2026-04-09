package com.example.framework.controller;

import com.example.framework.execution.TestExecutor;
import com.example.framework.model.TestResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Root-level REST controller for test execution.
 /run-tests endpoint at the root level * Provides the as required.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Root Test Execution API",
     description = "Root-level endpoint for running tests")
public class RootTestController {

    private final TestExecutor testExecutor;

    /**
     * POST /run-tests - Run all tests (API + UI)
     * This is the main endpoint as specified in requirements.
     */
    @Operation(
        summary     = "Run all tests",
        description = "Executes all tests (API + UI) in parallel. Returns test results."
    )
    @PostMapping("/run-tests")
    public ResponseEntity<List<TestResult>> runTests() {
        List<TestResult> results = testExecutor.runAllTests();
        return ResponseEntity.ok(results);
    }
}

