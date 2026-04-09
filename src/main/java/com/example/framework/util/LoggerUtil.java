package com.example.framework.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom logger utility for writing test execution logs to a file.
 * 
 * Logs are stored in /resources/logs/test-execution.log
 * Thread-safe for parallel test execution.
 */
@Slf4j
@Component
public class LoggerUtil {

    private static final String LOG_DIRECTORY = "src/main/resources/logs";
    private static final String LOG_FILE = "test-execution.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final Path logPath;

    public LoggerUtil() {
        try {
            Path dir = Paths.get(LOG_DIRECTORY);
            Files.createDirectories(dir);
            logPath = dir.resolve(LOG_FILE);
            
            // Initialize log file with header if it doesn't exist
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
                writeToFile("═".repeat(80));
                writeToFile("  AUTOMATION FRAMEWORK - TEST EXECUTION LOG");
                writeToFile("  Started: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
                writeToFile("═".repeat(80));
            }
            
            log.info("[LoggerUtil] Log file initialized at: {}", logPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[LoggerUtil] Failed to initialize log file: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize log file", e);
        }
    }

    /**
     * Formats a log message with timestamp and level.
     */
    private String formatMessage(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String threadName = Thread.currentThread().getName();
        return String.format("[%s] [%s] [%s] %s", timestamp, level, threadName, message);
    }

    /**
     * Writes a message to the log file.
     * Thread-safe operation.
     */
    private synchronized void writeToFile(String message) {
        try {
            Files.writeString(logPath, message + System.lineSeparator(), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write to log file: {}", e.getMessage());
        }
    }

    /**
     * Logs an info message with timestamp to both console and file.
     */
    public void logInfo(String message) {
        String formattedMessage = formatMessage("INFO", message);
        log.info(message);
        writeToFile(formattedMessage);
    }

    /**
     * Logs an error message with timestamp to both console and file.
     */
    public void logError(String message) {
        String formattedMessage = formatMessage("ERROR", message);
        log.error(message);
        writeToFile(formattedMessage);
    }

    /**
     * Logs a warning message with timestamp to both console and file.
     */
    public void logWarning(String message) {
        String formattedMessage = formatMessage("WARN", message);
        log.warn(message);
        writeToFile(formattedMessage);
    }

    /**
     * Logs a debug message with timestamp to both console and file.
     */
    public void logDebug(String message) {
        String formattedMessage = formatMessage("DEBUG", message);
        log.debug(message);
        writeToFile(formattedMessage);
    }

    /**
     * Logs the start of an API test.
     */
    public void logApiTestStart(String testName) {
        logInfo("Starting API Test: " + testName);
    }

    /**
     * Logs the successful completion of an API test.
     */
    public void logApiTestPassed(String testName, long durationMs) {
        logInfo("API Test Passed: " + testName + " (Duration: " + durationMs + "ms)");
    }

    /**
     * Logs the failure of an API test.
     */
    public void logApiTestFailed(String testName, String errorMessage) {
        logError("API Test Failed: " + testName + " - " + errorMessage);
    }

    /**
     * Logs the start of a UI test.
     */
    public void logUITestStart(String testName) {
        logInfo("Starting UI Test: " + testName);
    }

    /**
     * Logs the successful completion of a UI test.
     */
    public void logUITestPassed(String testName, long durationMs) {
        logInfo("UI Test Passed: " + testName + " (Duration: " + durationMs + "ms)");
    }

    /**
     * Logs the failure of a UI test.
     */
    public void logUITestFailed(String testName, String errorMessage) {
        logError("UI Test Failed: " + testName + " - " + errorMessage);
    }

    /**
     * Logs a test execution summary.
     */
    public void logTestSummary(int totalTests, int passed, int failed) {
        String separator = "─".repeat(50);
        logInfo(separator);
        logInfo("TEST EXECUTION SUMMARY");
        logInfo("Total Tests: " + totalTests);
        logInfo("Passed: " + passed);
        logInfo("Failed: " + failed);
        logInfo("Pass Rate: " + (totalTests > 0 ? String.format("%.2f%%", (passed * 100.0 / totalTests)) : "0%"));
        logInfo(separator);
    }

    /**
     * Returns the absolute path to the log file.
     */
    public String getLogFilePath() {
        return logPath.toAbsolutePath().toString();
    }

    /**
     * Clears the log file (for testing purposes).
     */
    public void clearLog() {
        try {
            Files.writeString(logPath, "", StandardOpenOption.TRUNCATE_EXISTING);
            logInfo("Log file cleared");
        } catch (IOException e) {
            log.error("Failed to clear log file: {}", e.getMessage());
        }
    }

    /**
     * Reads the last N lines from the log file.
     */
    public String readLastLines(int lines) {
        try {
            String content = Files.readString(logPath);
            String[] allLines = content.split(System.lineSeparator());
            int start = Math.max(0, allLines.length - lines);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < allLines.length; i++) {
                sb.append(allLines[i]).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("Failed to read log file: {}", e.getMessage());
            return "";
        }
    }
}

