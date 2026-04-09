package com.example.framework.selenium;

import com.example.framework.model.TestResult;
import com.example.framework.util.LoggerUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Selenium WebDriver test — automates a Google search and
 * verifies that results appear on the page.
 *
 * Thread-safe: creates a fresh WebDriver instance per call.
 * ChromeDriver is auto-managed by WebDriverManager (no manual setup).
 *
 * Flow:
 * 1. Open Google search URL directly (avoids form-submit redirect flakiness)
 * 2. Wait for any result container to appear in DOM (XPath OR conditions)
 * 3. Assert page source contains the search keyword "selenium"
 * 4. Quit the browser
 * 5. Capture screenshot on failure
 */
@Slf4j
@Component
public class GoogleSearchTest {

    @Value("${selenium.headless:true}")
    private boolean headless;

    @Value("${selenium.timeout:10}")
    private int timeoutSeconds;

    private final LoggerUtil loggerUtil;

    // Real Chrome UA — prevents Google from serving a degraded headless-only
    // response
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    public GoogleSearchTest(LoggerUtil loggerUtil) {
        this.loggerUtil = loggerUtil;
    }

    /**
     * Runs the Google search automation test.
     *
     * @param runId batch run identifier for grouping results
     * @return TestResult with PASSED/FAILED status and duration
     */
    public TestResult run(String runId) {
        long start = System.currentTimeMillis();
        WebDriver driver = null;

        String testName = "YouTube Test";
        loggerUtil.logUITestStart(testName);

        try {
            log.info("[SELENIUM] Starting GoogleSearchTest on thread: {}",
                    Thread.currentThread().getName());

            // ── Step 1: Set up Chrome ──────────────────────────────
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--window-size=1920,1080",
                    "--user-agent=" + USER_AGENT);
            if (headless) {
                options.addArguments("--headless=new", "--disable-gpu");
            }
            driver = new ChromeDriver(options);
            // Use explicit waits only — implicit wait conflicts with explicit waits
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            // ── Step 2: Navigate directly to Google search results ─
            // Direct URL is more reliable than form submission in headless mode
            String searchUrl = "https://www.youtube.com";            driver.get(searchUrl);
            log.debug("[SELENIUM] Navigated to search URL | Title: {}", driver.getTitle());

            // ── Step 3: Wait for any search result container ────────
            // NOTE: By.cssSelector("a, b") does NOT work in Selenium — use XPath 'or'
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds * 3L));
            By resultLocator = By.xpath("//*[@id='contents']");

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(resultLocator));
                log.info("[SELENIUM] Search result container found in DOM");
            } catch (TimeoutException te) {
                // Not a hard failure — fall through to page-source check below
                log.warn("[SELENIUM] Result container not found by XPath — falling back to page source check");
            }

            // ── Step 4: Ultimate assertion — keyword in page source ─
            // Even if Google changes its DOM classes, 'selenium' will still appear in
            // links/snippets
            String pageSource = driver.getPageSource().toLowerCase();
            log.info("[SELENIUM] Page title: \"{}\" | Source length: {} chars",
                    driver.getTitle(), pageSource.length());

            if (!pageSource.contains("youtube")) {
                throw new AssertionError(
                        "Expected page source to contain 'youtube' but it did not. " +
                                "Page title was: \"" + driver.getTitle() + "\"");
            }

            log.info("[SELENIUM] PASSED — 'selenium' keyword confirmed in search results page");
            
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
            log.error("[SELENIUM] FAILED — {}", e.getMessage());
            
            // Capture screenshot on failure
            String screenshotPath = captureScreenshot(driver, testName);
            
            long duration = System.currentTimeMillis() - start;
            String errorMessage = e.getMessage() + (screenshotPath != null ? " | Screenshot: " + screenshotPath : "");
            
            loggerUtil.logUITestFailed(testName, errorMessage);
            
            return TestResult.builder()
                    .testName(testName)
                    .testType("SELENIUM")
                    .status("FAILED")
                    .durationMs(duration)
                    .errorMessage(errorMessage)
                    .threadName(Thread.currentThread().getName())
                    .executedAt(LocalDateTime.now())
                    .runId(runId)
                    .build();

        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Captures a screenshot if the driver is available.
     */
    private String captureScreenshot(WebDriver driver, String testName) {
        if (driver == null) {
            return null;
        }
        
        try {
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            java.io.File sourceFile = screenshot.getScreenshotAs(OutputType.FILE);
            
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String fileName = testName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".png";
            
            java.nio.file.Path targetPath = java.nio.file.Paths.get("src/main/resources/screenshots", fileName);
            java.nio.file.Files.createDirectories(targetPath.getParent());
            java.nio.file.Files.copy(sourceFile.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            String screenshotPath = targetPath.toAbsolutePath().toString();
            log.error("[SELENIUM] Failure screenshot saved: {}", screenshotPath);
            return screenshotPath;
            
        } catch (Exception e) {
            log.error("[SELENIUM] Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }
}
