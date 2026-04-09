package com.example.framework.service;

import com.example.framework.execution.ParallelTestRunner;
import com.example.framework.model.TestResult;
import com.example.framework.repository.TestResultRepository;
import com.example.framework.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service layer between the REST controller and the execution engine.
 *
 * Responsibilities:
 *  - Trigger parallel test execution via ParallelTestRunner
 *  - Query stored results from the DB via TestResultRepository
 *  - Aggregate statistics for the summary endpoint
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestExecutionService {

    private final ParallelTestRunner   parallelTestRunner;
    private final TestResultRepository resultRepository;
    private final LoggerUtil           loggerUtil;

    /**
     * Triggers a full parallel test run.
     * Blocks until all tests complete, then returns their results.
     */
    public List<TestResult> runAllTests() {
        log.info("[SERVICE] Triggering parallel test run");
        loggerUtil.logInfo("Starting Test Execution via TestExecutionService");
        
        List<TestResult> results = parallelTestRunner.runAll();
        
        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.size() - passed;
        loggerUtil.logTestSummary(results.size(), (int) passed, (int) failed);
        
        return results;
    }

    /** Returns every TestResult stored in the database */
    public List<TestResult> getAllResults() {
        return resultRepository.findAllOrderByDateDesc();
    }

    /** Returns all results for a specific run batch */
    public List<TestResult> getResultsByRunId(String runId) {
        return resultRepository.findByRunId(runId);
    }

    /** Returns only PASSED results */
    public List<TestResult> getPassedResults() {
        return resultRepository.findByStatus("PASSED");
    }

    /** Returns only FAILED results */
    public List<TestResult> getFailedResults() {
        return resultRepository.findByStatus("FAILED");
    }

    /**
     * Aggregated statistics across all stored results.
     *
     * Returns:
     *  totalTests, passed, failed, passRatePct,
     *  avgSeleniumDurationMs, avgApiDurationMs
     */
    public Map<String, Object> getSummary() {
        long total  = resultRepository.count();
        long passed = resultRepository.findByStatus("PASSED").size();
        long failed = total - passed;

        Double avgSelenium = resultRepository.avgDurationByType("SELENIUM");
        Double avgApi      = resultRepository.avgDurationByType("API");

        double passRate = total > 0 ? (double) passed / total * 100 : 0.0;

        return Map.of(
                "totalTests",           total,
                "passed",               passed,
                "failed",               failed,
                "passRatePct",          String.format("%.1f%%", passRate),
                "avgSeleniumDurationMs", avgSelenium != null ? avgSelenium.longValue() : 0,
                "avgApiDurationMs",      avgApi      != null ? avgApi.longValue()      : 0
        );
    }
}
