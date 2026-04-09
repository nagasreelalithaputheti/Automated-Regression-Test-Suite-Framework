package com.example.framework.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebDriver lifecycle for Selenium tests.
 * Provides browser initialization, configuration, and screenshot capture functionality.
 * Thread-safe for parallel test execution - each thread gets its own WebDriver instance.
 */
@Slf4j
@Component
public class BrowserManager {

    // Real Chrome UA to prevent Google from serving degraded headless response
    private static final String CHROME_USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    
    private static final String EDGE_USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edge/124.0.0.0";

    @Value("${selenium.browser:chrome}")
    private String browser;

    @Value("${selenium.headless:true}")
    private boolean headless;

    @Value("${selenium.timeout:10}")
    private int timeoutSeconds;

    @Value("${selenium.window.size:1920,1080}")
    private String windowSize;

    @Value("${selenium.screenshots.enabled:true}")
    private boolean screenshotsEnabled;

    @Value("${selenium.screenshots.directory:src/main/resources/screenshots}")
    private String screenshotsDirectory;

    // Thread-local WebDriver instances for parallel execution
    private final ThreadLocal<WebDriver> webDriver = new ThreadLocal<>();
    private final ThreadLocal<WebDriverWait> webDriverWait = new ThreadLocal<>();
    private final ConcurrentHashMap<Long, String> screenshotPaths = new ConcurrentHashMap<>();

    private Path screenshotDir;

    public BrowserManager() {
        try {
            screenshotDir = Paths.get("src/main/resources/screenshots");
            Files.createDirectories(screenshotDir);
        } catch (IOException e) {
            log.error("Failed to create screenshots directory: {}", e.getMessage());
        }
    }

    /**
     * Initializes and returns a WebDriver instance for the current thread.
     * Creates a new instance if one doesn't exist.
     */
    public WebDriver getDriver() {
        if (webDriver.get() == null) {
            WebDriver driver = createDriver();
            webDriver.set(driver);
            webDriverWait.set(new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds * 3L)));
            log.info("[BrowserManager] Created new WebDriver for thread: {}", Thread.currentThread().getName());
        }
        return webDriver.get();
    }

    /**
     * Returns the WebDriverWait instance for the current thread.
     */
    public WebDriverWait getWait() {
        if (webDriverWait.get() == null) {
            getDriver(); // This will initialize both driver and wait
        }
        return webDriverWait.get();
    }

    /**
     * Creates a new WebDriver instance based on configured browser.
     */
    private WebDriver createDriver() {
        switch (browser.toLowerCase()) {
            case "chrome":
                return createChromeDriver();
            case "firefox":
                return createFirefoxDriver();
            case "edge":
                return createEdgeDriver();
            default:
                log.warn("[BrowserManager] Unknown browser '{}', defaulting to Chrome", browser);
                return createChromeDriver();
        }
    }

    /**
     * Creates Chrome WebDriver with optimized options.
     */
    private ChromeDriver createChromeDriver() {
        try {
            WebDriverManager.chromedriver().setup();
        } catch (Exception e) {
            log.warn("[BrowserManager] WebDriverManager setup failed: {}, using system chromedriver", e.getMessage());
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-blink-features=AutomationControlled",
                "--disable-extensions",
                "--disable-plugins",
                "--disable-popup-blocking",
                "--start-maximized",
                "--user-agent=" + CHROME_USER_AGENT);

        // Set window size
        String[] size = windowSize.split(",");
        if (size.length == 2) {
            options.addArguments("--window-size=" + size[0] + "," + size[1]);
        }

        if (headless) {
            options.addArguments(
                    "--headless=new",
                    "--disable-gpu",
                    "--disable-software-rasterizer");
        }

        // Add preferences to disable automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        return new ChromeDriver(options);
    }

    /**
     * Creates Firefox WebDriver with optimized options.
     */
    private FirefoxDriver createFirefoxDriver() {
        try {
            WebDriverManager.firefoxdriver().setup();
        } catch (Exception e) {
            log.warn("[BrowserManager] WebDriverManager setup failed: {}", e.getMessage());
        }

        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("--headless");
        }
        options.addArguments("--width=1920", "--height=1080");

        return new FirefoxDriver(options);
    }

    /**
     * Creates Edge WebDriver with optimized options.
     */
    private EdgeDriver createEdgeDriver() {
        try {
            WebDriverManager.edgedriver().setup();
        } catch (Exception e) {
            log.warn("[BrowserManager] WebDriverManager setup failed: {}", e.getMessage());
        }

        EdgeOptions options = new EdgeOptions();
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--user-agent=" + EDGE_USER_AGENT);

        if (headless) {
            options.addArguments("--headless");
        }

        return new EdgeDriver(options);
    }

    /**
     * Captures a screenshot and saves it to the screenshots directory.
     * Only captures if screenshots are enabled.
     * 
     * @param testName Name of the test for filename
     * @return Path to the saved screenshot, or null if capture failed
     */
    public String captureScreenshot(String testName) {
        if (!screenshotsEnabled) {
            log.debug("[BrowserManager] Screenshots disabled, skipping capture");
            return null;
        }

        WebDriver driver = webDriver.get();
        if (driver == null) {
            log.warn("[BrowserManager] No WebDriver instance for current thread, cannot capture screenshot");
            return null;
        }

        try {
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            File sourceFile = screenshot.getScreenshotAs(OutputType.FILE);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String threadName = Thread.currentThread().getName().replace("-", "_");
            String fileName = String.format("%s_%s_%s.png", 
                    testName.replaceAll("[^a-zA-Z0-9]", "_"), 
                    threadName, 
                    timestamp);
            
            Path targetPath = screenshotDir.resolve(fileName);
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            String screenshotPath = targetPath.toAbsolutePath().toString();
            screenshotPaths.put(Thread.currentThread().getId(), screenshotPath);
            
            log.info("[BrowserManager] Screenshot captured: {}", screenshotPath);
            return screenshotPath;

        } catch (Exception e) {
            log.error("[BrowserManager] Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures a screenshot on test failure.
     * 
     * @param testName Name of the test
     * @param errorMessage Error message that caused the failure
     * @return Path to the saved screenshot
     */
    public String captureFailureScreenshot(String testName, String errorMessage) {
        log.info("[BrowserManager] Capturing failure screenshot for test: {}", testName);
        String screenshotPath = captureScreenshot(testName + "_FAILED");
        
        if (screenshotPath != null) {
            log.error("[BrowserManager] Failure screenshot saved: {} | Error: {}", 
                    screenshotPath, errorMessage);
        }
        
        return screenshotPath;
    }

    /**
     * Returns the path of the last screenshot captured by the current thread.
     */
    public String getLastScreenshotPath() {
        return screenshotPaths.get(Thread.currentThread().getId());
    }

    /**
     * Quits and closes the WebDriver for the current thread.
     */
    public void quitDriver() {
        WebDriver driver = webDriver.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("[BrowserManager] WebDriver quit for thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("[BrowserManager] Error quitting WebDriver: {}", e.getMessage());
            } finally {
                webDriver.remove();
                webDriverWait.remove();
            }
        }
    }

    /**
     * Quits all WebDriver instances (for graceful shutdown).
     */
    public void quitAll() {
        webDriver.get().quit();
        webDriver.remove();
        webDriverWait.remove();
        log.info("[BrowserManager] All WebDrivers quit");
    }

    /**
     * Gets the current browser name.
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * Checks if screenshots are enabled.
     */
    public boolean isScreenshotsEnabled() {
        return screenshotsEnabled;
    }

    /**
     * Gets the screenshots directory path.
     */
    public String getScreenshotsDirectory() {
        return screenshotsDirectory;
    }

    /**
     * Navigates to the specified URL.
     */
    public void navigateTo(String url) {
        getDriver().get(url);
        log.debug("[BrowserManager] Navigated to: {}", url);
    }

    /**
     * Gets the page title.
     */
    public String getPageTitle() {
        return getDriver().getTitle();
    }

    /**
     * Gets the current URL.
     */
    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    /**
     * Refreshes the current page.
     */
    public void refresh() {
        getDriver().navigate().refresh();
    }

    /**
     * Goes back in browser history.
     */
    public void goBack() {
        getDriver().navigate().back();
    }

    /**
     * Goes forward in browser history.
     */
    public void goForward() {
        getDriver().navigate().forward();
    }
}

