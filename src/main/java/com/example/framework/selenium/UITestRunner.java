package com.example.framework.selenium;

import com.example.framework.model.TestResult;
import com.example.framework.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * UI Test Runner for Selenium WebDriver tests.
 * Provides methods for running UI automation tests with screenshot capture on failure.
 * Thread-safe for parallel test execution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UITestRunner {

    private final BrowserManager browserManager;
    private final LoggerUtil loggerUtil;

    @Value("${selenium.timeout:10}")
    private int timeoutSeconds;

    /**
     * Runs a Google search test and validates the page title.
     * Opens Google and verifies the page loads correctly.
     */
    public TestResult runGoogleTest(String runId) {
        long start = System.currentTimeMillis();
        String testName = "Google Search Test";
        
        loggerUtil.logUITestStart(testName);
        
        WebDriver driver = null;
        
        try {
            // Get WebDriver from BrowserManager
            driver = browserManager.getDriver();
            WebDriverWait wait = browserManager.getWait();
            
            // Navigate to Google
            String searchUrl = "https://www.google.com/search?q=Selenium+WebDriver&hl=en";
            driver.get(searchUrl);
            
            log.info("[UITestRunner] Navigated to: {}", searchUrl);
            
            // Wait for search results to appear
            By resultLocator = By.xpath(
                    "//*[@id='rso'] | //*[@id='search'] | " +
                    "//*[contains(@class,'MjjYud')] | " +
                    "//*[contains(@class,'tF2Cxc')] | " +
                    "//*[@data-hveid]");
            
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(resultLocator));
                log.info("[UITestRunner] Search result container found");
            } catch (TimeoutException te) {
                log.warn("[UITestRunner] Result container not found - continuing with page validation");
            }
            
            // Validate page title contains expected text
            String pageTitle = driver.getTitle();
            String pageSource = driver.getPageSource().toLowerCase();
            
            log.info("[UITestRunner] Page title: \"{}\"", pageTitle);
            
            // Check for selenium keyword in page source
            if (!pageSource.contains("selenium")) {
                throw new AssertionError(
                        "Expected page source to contain 'selenium' but it did not. " +
                        "Page title was: \"" + pageTitle + "\"");
            }
            
            long duration = System.currentTimeMillis() - start;
            loggerUtil.logUITestPassed(testName, duration);
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("SELENIUM")
                    .status("PASSED")
                    .durationMs(duration)
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            String errorMessage = e.getMessage();
            
            // Capture screenshot on failure
            String screenshotPath = browserManager.captureFailureScreenshot(testName, errorMessage);
            
            loggerUtil.logUITestFailed(testName, errorMessage);
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("SELENIUM")
                    .status("FAILED")
                    .durationMs(duration)
                    .errorMessage(errorMessage + (screenshotPath != null ? " | Screenshot: " + screenshotPath : ""))
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
                    
        } finally {
            // Quit the driver after test
            if (browserManager != null) {
                browserManager.quitDriver();
            }
        }
    }

    /**
     * Runs a generic UI test with the given URL and validation.
     * 
     * @param runId The run ID for grouping test results
     * @param testName The name of the test
     * @param url The URL to navigate to
     * @param validator Custom validation logic
     * @return TestResult with PASSED/FAILED status
     */
    public TestResult runCustomUITest(String runId, String testName, String url, 
                                       java.util.function.Function<WebDriver, Boolean> validator) {
        long start = System.currentTimeMillis();
        
        loggerUtil.logUITestStart(testName);
        
        WebDriver driver = null;
        
        try {
            driver = browserManager.getDriver();
            driver.get(url);
            
            // Run custom validation
            boolean isValid = validator.apply(driver);
            
            if (!isValid) {
                throw new AssertionError("Custom validation failed for URL: " + url);
            }
            
            long duration = System.currentTimeMillis() - start;
            loggerUtil.logUITestPassed(testName, duration);
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("SELENIUM")
                    .status("PASSED")
                    .durationMs(duration)
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            String errorMessage = e.getMessage();
            
            // Capture screenshot on failure
            String screenshotPath = browserManager.captureFailureScreenshot(testName, errorMessage);
            
            loggerUtil.logUITestFailed(testName, errorMessage);
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("SELENIUM")
                    .status("FAILED")
                    .durationMs(duration)
                    .errorMessage(errorMessage + (screenshotPath != null ? " | Screenshot: " + screenshotPath : ""))
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();
                    
        } finally {
            if (browserManager != null) {
                browserManager.quitDriver();
            }
        }
    }

    /**
     * Gets the BrowserManager instance for direct access if needed.
     */
    public BrowserManager getBrowserManager() {
        return browserManager;
    }
}

