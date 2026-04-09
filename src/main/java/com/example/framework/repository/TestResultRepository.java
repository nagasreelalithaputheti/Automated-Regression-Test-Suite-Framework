package com.example.framework.repository;

import com.example.framework.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for TestResult.
 *
 * Spring auto-generates the JDBC implementation at runtime.
 * No SQL needed for basic CRUD — just call the methods.
 *
 * Custom queries use JPQL (Java Persistence Query Language).
 */
@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    /** All results for a given run batch */
    List<TestResult> findByRunId(String runId);

    /** All results with a specific status (PASSED / FAILED) */
    List<TestResult> findByStatus(String status);

    /** All results of a test type (SELENIUM / API) */
    List<TestResult> findByTestType(String testType);

    /** Most recent N results ordered by time descending */
    @Query("SELECT r FROM TestResult r ORDER BY r.executedAt DESC")
    List<TestResult> findAllOrderByDateDesc();

    /** Count passed tests in a run */
    @Query("SELECT COUNT(r) FROM TestResult r WHERE r.runId = :runId AND r.status = 'PASSED'")
    long countPassedByRunId(@Param("runId") String runId);

    /** Count failed tests in a run */
    @Query("SELECT COUNT(r) FROM TestResult r WHERE r.runId = :runId AND r.status = 'FAILED'")
    long countFailedByRunId(@Param("runId") String runId);

    /** Average duration for a test type */
    @Query("SELECT AVG(r.durationMs) FROM TestResult r WHERE r.testType = :type")
    Double avgDurationByType(@Param("type") String type);
}
