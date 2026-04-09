package com.example.framework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * View controller for serving HTML pages (dashboard, report, home)
 */
@Controller
public class ViewController {

    /**
     * GET / - Serve the main index/home page
     */
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    /**
     * GET /dashboard - Serve the dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/dashboard.html";
    }

    /**
     * GET /report - Serve the report page
     */
    @GetMapping("/report")
    public String report() {
        return "forward:/report.html";
    }
}
