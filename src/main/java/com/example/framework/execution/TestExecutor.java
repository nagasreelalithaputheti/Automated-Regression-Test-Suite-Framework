package com.example.framework.execution;

import com.example.framework.model.TestResult;
import com.example.framework.repository.TestResultRepository;
import com.example.framework.selenium.UITestRunner;
import com.example.framework.service.ApiTestService;
import com.example.framework.util.LoggerUtil;
import com.example.framework.util.ReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Test Execution Engine - Coordinates running multiple tests (API and UI).
 * Implements parallel test execution using Java ExecutorService.
 * Logs all test execution to file and generates HTML reports.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestExecutor {

    private final ApiTestService apiTestService;
    private final UITestRunner uiTestRunner;
    private final TestResultRepository resultRepository;
    private final ReportGenerator reportGenerator;
    private final LoggerUtil loggerUtil;

    @Value("${test.parallel.threads:4}")
    private int threadCount;

    /** Timeout per individual test (minutes) */
    private static final long TASK_TIMEOUT_MINUTES = 3;

    /**
     * Runs all tests (API and UI) in parallel.
     * 
     * @return List of all TestResult objects
     */
    public List<TestResult> runAllTests() {
        String runId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        loggerUtil.logInfo("========================================");
        loggerUtil.logInfo("TEST EXECUTION STARTED - Run ID: " + runId);
        loggerUtil.logInfo("========================================");
        
        log.info("╔══════════════════════════════════════════╗");
        log.info("║  PARALLEL TEST RUN STARTED               ║");
        log.info("║  Run ID   : {}                      ║", runId);
        log.info("║  Threads  : {}                            ║", threadCount);
        log.info("╚══════════════════════════════════════════╝");

        // Build list of test callables
        List<Callable<TestResult>> tasks = new ArrayList<>();
        
        // Add API tests
        tasks.add(() -> runApiTestsWithLogging(runId).get(0)); // Get post test
        tasks.add(() -> runApiTestsWithLogging(runId).get(1)); // Create post test
        tasks.add(() -> runApiTestsWithLogging(runId).get(2)); // Get user test
        
        // Add UI test
        tasks.add(() -> uiTestRunner.runGoogleTest(runId));

        // Create named thread pool
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(threadCount, tasks.size()),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("test-thread-" + t.getId());
                    t.setDaemon(true);
                    return t;
                });

        List<Future<TestResult>> futures = new ArrayList<>();
        List<TestResult> results = new ArrayList<>();

        // Submit all tasks
        for (Callable<TestResult> task : tasks) {
            futures.add(executor.submit(task));
        }

        // Collect results with timeout
        for (Future<TestResult> future : futures) {
            try {
                TestResult result = future.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                results.add(result);

                // Persist to DB
                resultRepository.save(result);
                log.info("Saved result → [{}] {} ({}ms)",
                        result.getStatus(), result.getTestName(), result.getDurationMs());

            } catch (TimeoutException e) {
                log.error("Test TIMED OUT after {}min", TASK_TIMEOUT_MINUTES);
                future.cancel(true);
                results.add(buildTimeoutResult(runId));

            } catch (ExecutionException e) {
                log.error("Test threw an exception: {}", e.getCause().getMessage());
                results.add(buildErrorResult(runId, e.getCause().getMessage()));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for test result");
            }
        }

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Print and log summary
        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.size() - passed;

        loggerUtil.logInfo("========================================");
        loggerUtil.logInfo("TEST EXECUTION COMPLETED");
        loggerUtil.logInfo("Total Tests: " + results.size());
        loggerUtil.logInfo("Passed: " + passed);
        loggerUtil.logInfo("Failed: " + failed);
        loggerUtil.logInfo("========================================");
        
        loggerUtil.logTestSummary(results.size(), (int) passed, (int) failed);

        log.info("╔══════════════════════════════════════════╗");
        log.info("║  RUN COMPLETE  [{}]                  ║", runId);
        log.info("║  Total : {}  |  Passed : {}  |  Failed : {} ║",
                results.size(), passed, failed);
        log.info("╚══════════════════════════════════════════╝");

        // Generate HTML report
        try {
            String reportPath = reportGenerator.generate(runId, results);
            loggerUtil.logInfo("HTML Report generated: " + reportPath);
            log.info("HTML Report → {}", reportPath);
        } catch (Exception e) {
            log.warn("Report generation failed: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Runs API tests with proper logging.
     */
    private List<TestResult> runApiTestsWithLogging(String runId) {
        return apiTestService.runAllApiTests(runId);
    }

    /**
     * Runs only API tests.
     * 
     * @return List of API TestResult objects
     */
    public List<TestResult> runApiTestsOnly() {
        String runId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        loggerUtil.logInfo("Starting API Tests Only - Run ID: " + runId);
        
        List<TestResult> results = apiTestService.runAllApiTests(runId);
        
        // Save results to database
        for (TestResult result : results) {
            resultRepository.save(result);
        }
        
        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.size() - passed;
        
        loggerUtil.logTestSummary(results.size(), (int) passed, (int) failed);
        
        return results;
    }

    /**
     * Runs only UI tests.
     * 
     * @return List of UI TestResult objects
     */
    public List<TestResult> runUITestsOnly() {
        String runId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        loggerUtil.logInfo("Starting UI Tests Only - Run ID: " + runId);
        
        List<TestResult> results = new ArrayList<>();
        results.add(uiTestRunner.runGoogleTest(runId));
        
        // Save results to database
        for (TestResult result : results) {
            resultRepository.save(result);
        }
        
        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.size() - passed;
        
        loggerUtil.logTestSummary(results.size(), (int) passed, (int) failed);
        
        return results;
    }

    /**
     * Runs a single test by name.
     * 
     * @param testName Name of the test to run
     * @return TestResult
     */
    public TestResult runSingleTest(String testName) {
        String runId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        loggerUtil.logInfo("Running single test: " + testName);
        
        TestResult result;
        
        if (testName.toLowerCase().contains("ui") || testName.toLowerCase().contains("google")) {
            result = uiTestRunner.runGoogleTest(runId);
        } else {
            // Default to API test
            result = apiTestService.runGetPostTest(runId);
        }
        
        resultRepository.save(result);
        
        return result;
    }

    // Helper methods

    private TestResult buildTimeoutResult(String runId) {
        return TestResult.builder()
                .testName("Unknown (timed out)")
                .testType("UNKNOWN")
                .status("FAILED")
                .errorMessage("Test exceeded " + TASK_TIMEOUT_MINUTES + " minute timeout")
                .threadName(Thread.currentThread().getName())
                .runId(runId)
                .build();
    }

    private TestResult buildErrorResult(String runId, String message) {
        return TestResult.builder()
                .testName("Unknown (executor error)")
                .testType("UNKNOWN")
                .status("FAILED")
                .errorMessage(message)
                .threadName(Thread.currentThread().getName())
                .runId(runId)
                .build();
    }

    /**
     * Gets the current thread count.
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Sets the thread count for parallel execution.
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}

