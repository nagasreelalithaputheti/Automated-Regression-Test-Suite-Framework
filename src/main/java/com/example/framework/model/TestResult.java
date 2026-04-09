package com.example.framework.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the result of a single test execution.
 *
 * Persisted to H2 (dev) or MySQL (prod) via Spring Data JPA / JDBC.
 *
 * Table: test_results
 */
@Entity
@Table(name = "test_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the test (e.g., "Google Search Test") */
    @Column(nullable = false)
    private String testName;

    /** Category: SELENIUM or API */
    @Column(nullable = false)
    private String testType;

    /** PASSED or FAILED */
    @Column(nullable = false)
    private String status;

    /** Execution time in milliseconds */
    private long durationMs;

    /** Error message if the test failed; null if passed */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** Thread that executed this test */
    private String threadName;

    /** When this result was recorded */
    @Column(nullable = false)
    private LocalDateTime executedAt;

    /** Batch / run group identifier */
    private String runId;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) executedAt = LocalDateTime.now();
    }
}
