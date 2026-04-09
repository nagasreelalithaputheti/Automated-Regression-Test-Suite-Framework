package com.example.framework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║         Automation Framework — Spring Boot App           ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  Swagger UI  → http://localhost:8080/swagger-ui.html     ║
 * ║  H2 Console  → http://localhost:8080/h2-console          ║
 * ║  Run Tests   → POST /api/tests/run                       ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Features:
 *  • Selenium WebDriver  — Google search UI automation
 *  • REST-Assured        — JSONPlaceholder API validation
 *  • Parallel Execution  — Thread-pool based runner
 *  • H2 / MySQL          — JDBC result persistence
 *  • HTML Report         — Auto-generated after each run
 */
@SpringBootApplication
public class AutomationFrameworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomationFrameworkApplication.class, args);

        System.out.println("""
                
                ╔══════════════════════════════════════════════════════╗
                ║           Automation Framework — STARTED             ║
                ╠══════════════════════════════════════════════════════╣
                ║  Swagger UI : http://localhost:8080/swagger-ui.html  ║
                ║  H2 Console : http://localhost:8080/h2-console       ║
                ║  Run Tests  : POST http://localhost:8080/api/tests/run║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
