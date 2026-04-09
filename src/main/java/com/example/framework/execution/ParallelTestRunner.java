package com.example.framework.execution;

import com.example.framework.api.SampleApiTest;
import com.example.framework.model.TestResult;
import com.example.framework.repository.TestResultRepository;
import com.example.framework.selenium.GoogleSearchTest;
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
 * Executes all tests in parallel using a fixed Java thread pool.
 *
 * How it works:
 *  1. Generates a unique runId for this batch
 *  2. Submits each test as a Callable to an ExecutorService
 *  3. Collects all Future results with a per-task timeout
 *  4. Persists every TestResult to the database via JPA
 *  5. Triggers HTML report generation
 *
 * Thread safety:
 *  - Each test (Selenium, API) is self-contained and stateless
 *  - Selenium creates its own WebDriver instance per thread
 *  - REST-Assured is inherently stateless
 *  - DB writes use Spring's JPA transaction handling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParallelTestRunner {

    private final GoogleSearchTest       googleSearchTest;
    private final SampleApiTest          sampleApiTest;
    private final TestResultRepository   resultRepository;
    private final ReportGenerator        reportGenerator;

    @Value("${test.parallel.threads:4}")
    private int threadCount;

    /** Timeout per individual test (minutes) */
    private static final long TASK_TIMEOUT_MINUTES = 3;

    /**
     * Runs all registered tests in parallel.
     *
     * @return list of all TestResult objects from this run
     */
    public List<TestResult> runAll() {
        String runId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("╔══════════════════════════════════════════╗");
        log.info("║  PARALLEL TEST RUN STARTED               ║");
        log.info("║  Run ID   : {}                      ║", runId);
        log.info("║  Threads  : {}                            ║", threadCount);
        log.info("╚══════════════════════════════════════════╝");

        // ── Build the list of test callables ──────────────────────
        List<Callable<TestResult>> tasks = List.of(
                () -> googleSearchTest.run(runId),
                () -> sampleApiTest.runGetPostTest(runId),
                () -> sampleApiTest.runCreatePostTest(runId),
                () -> sampleApiTest.runGetUserTest(runId)
        );

        // ── Create named thread pool ───────────────────────────────
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(threadCount, tasks.size()),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("test-thread-" + t.getId());
                    t.setDaemon(true);
                    return t;
                });

        List<Future<TestResult>> futures  = new ArrayList<>();
        List<TestResult>         results  = new ArrayList<>();

        // ── Submit all tasks ───────────────────────────────────────
        for (Callable<TestResult> task : tasks) {
            futures.add(executor.submit(task));
        }

        // ── Collect results with timeout ───────────────────────────
        for (Future<TestResult> future : futures) {
            try {
                TestResult result = future.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                results.add(result);

                // Persist to DB immediately
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

        // ── Shutdown executor ──────────────────────────────────────
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // ── Print summary ─────────────────────────────────────────
        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.size() - passed;

        log.info("╔══════════════════════════════════════════╗");
        log.info("║  RUN COMPLETE  [{}]                  ║", runId);
        log.info("║  Total : {}  |  Passed : {}  |  Failed : {} ║",
                results.size(), passed, failed);
        log.info("╚══════════════════════════════════════════╝");

        // ── Generate HTML report ──────────────────────────────────
        try {
            String reportPath = reportGenerator.generate(runId, results);
            log.info("HTML Report → {}", reportPath);
        } catch (Exception e) {
            log.warn("Report generation failed: {}", e.getMessage());
        }

        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────

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
}
