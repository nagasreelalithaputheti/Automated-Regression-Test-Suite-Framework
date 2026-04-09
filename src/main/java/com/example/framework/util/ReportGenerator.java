package com.example.framework.util;

import com.example.framework.model.TestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a styled, self-contained HTML report for a test run.
 *
 * The report includes:
 *  - Run summary card (total / passed / failed / pass rate)
 *  - Color-coded results table (green = PASSED, red = FAILED)
 *  - Duration for every test
 *  - Thread name showing which thread executed each test
 *  - Error message inline for failures
 *
 * Output: reports/report_{runId}_{timestamp}.html
 */
@Slf4j
@Component
public class ReportGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates the HTML report file and returns its path.
     *
     * @param runId   identifier for this test batch
     * @param results list of TestResult objects from the run
     * @return        absolute path to the generated HTML file
     */
    public String generate(String runId, List<TestResult> results) throws IOException {
        Path dir = Paths.get("reports");
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TS);
        String fileName  = "report_" + runId + "_" + timestamp + ".html";
        Path   filePath  = dir.resolve(fileName);

        Files.writeString(filePath, buildHtml(runId, results));
        log.info("[REPORT] Generated: {}", filePath.toAbsolutePath());
        return filePath.toAbsolutePath().toString();
    }

    // ─────────────────────────────────────────────────────────────
    // HTML Builder
    // ─────────────────────────────────────────────────────────────

    private String buildHtml(String runId, List<TestResult> results) {
        long total  = results.size();
        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = total - passed;
        double rate = total > 0 ? (double) passed / total * 100 : 0;

        StringBuilder rows = new StringBuilder();
        for (TestResult r : results) {
            boolean ok = "PASSED".equals(r.getStatus());
            rows.append("<tr class='").append(ok ? "pass" : "fail").append("'>")
                .append("<td>").append(esc(r.getTestName())).append("</td>")
                .append("<td><span class='badge ").append(r.getTestType()).append("'>")
                     .append(esc(r.getTestType())).append("</span></td>")
                .append("<td><span class='status ").append(ok ? "p" : "f").append("'>")
                     .append(ok ? "✔ PASSED" : "✘ FAILED").append("</span></td>")
                .append("<td>").append(r.getDurationMs()).append(" ms</td>")
                .append("<td class='thread'>").append(esc(r.getThreadName())).append("</td>")
                .append("<td class='err'>").append(r.getErrorMessage() != null
                        ? "<code>" + esc(r.getErrorMessage()) + "</code>" : "—").append("</td>")
                .append("</tr>\n");
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Test Report — %s</title>
            <style>
              @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Syne:wght@700;800&display=swap');
              *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
              :root {
                --bg:       #0d0f14;
                --surface:  #151820;
                --border:   #252933;
                --accent:   #00e5c8;
                --pass:     #00e5a0;
                --fail:     #ff4f6b;
                --text:     #e8eaf2;
                --muted:    #6b7080;
                --api-clr:  #6c91ff;
                --sel-clr:  #f5a623;
              }
              body { background: var(--bg); color: var(--text); font-family: 'JetBrains Mono', monospace; min-height: 100vh; }
              
              /* Header */
              .header { background: var(--surface); border-bottom: 1px solid var(--border); padding: 2rem 2.5rem; display: flex; align-items: center; justify-content: space-between; }
              .header h1 { font-family: 'Syne', sans-serif; font-size: 1.6rem; font-weight: 800; color: var(--accent); letter-spacing: -0.5px; }
              .run-id { font-size: 0.75rem; color: var(--muted); margin-top: 0.25rem; }
              .timestamp { font-size: 0.75rem; color: var(--muted); text-align: right; }
              
              /* Summary cards */
              .cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; padding: 1.5rem 2.5rem; }
              .card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 1.25rem 1.5rem; position: relative; overflow: hidden; }
              .card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; }
              .card.total::before  { background: var(--accent); }
              .card.pass-c::before { background: var(--pass); }
              .card.fail-c::before { background: var(--fail); }
              .card.rate-c::before { background: #a78bfa; }
              .card-label { font-size: 0.65rem; text-transform: uppercase; letter-spacing: 1.5px; color: var(--muted); margin-bottom: 0.5rem; }
              .card-value { font-family: 'Syne', sans-serif; font-size: 2.2rem; font-weight: 800; line-height: 1; }
              .card.total  .card-value { color: var(--accent); }
              .card.pass-c .card-value { color: var(--pass); }
              .card.fail-c .card-value { color: var(--fail); }
              .card.rate-c .card-value { color: #a78bfa; }
              
              /* Progress bar */
              .progress-wrap { padding: 0 2.5rem 1.5rem; }
              .progress-bar { height: 6px; background: var(--border); border-radius: 3px; overflow: hidden; }
              .progress-fill { height: 100%; background: linear-gradient(90deg, var(--pass), var(--accent)); border-radius: 3px; transition: width 0.6s ease; }
              .progress-label { font-size: 0.65rem; color: var(--muted); margin-top: 0.4rem; letter-spacing: 0.5px; }
              
              /* Table */
              .table-wrap { padding: 0 2.5rem 2.5rem; }
              .table-title { font-family: 'Syne', sans-serif; font-size: 0.85rem; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; color: var(--muted); margin-bottom: 1rem; }
              table { width: 100%%; border-collapse: collapse; font-size: 0.78rem; }
              thead th { background: var(--surface); border: 1px solid var(--border); padding: 0.7rem 1rem; text-align: left; font-size: 0.65rem; text-transform: uppercase; letter-spacing: 1px; color: var(--muted); }
              tbody tr { border-bottom: 1px solid var(--border); transition: background 0.15s; }
              tbody tr:hover { background: rgba(255,255,255,0.03); }
              td { padding: 0.75rem 1rem; vertical-align: middle; }
              tr.pass { border-left: 3px solid var(--pass); }
              tr.fail { border-left: 3px solid var(--fail); }
              
              .badge { display: inline-block; padding: 0.15rem 0.55rem; border-radius: 4px; font-size: 0.6rem; font-weight: 600; letter-spacing: 0.5px; text-transform: uppercase; }
              .badge.SELENIUM { background: rgba(245,166,35,0.15); color: var(--sel-clr); border: 1px solid rgba(245,166,35,0.3); }
              .badge.API      { background: rgba(108,145,255,0.15); color: var(--api-clr); border: 1px solid rgba(108,145,255,0.3); }
              
              .status { font-weight: 600; }
              .status.p { color: var(--pass); }
              .status.f { color: var(--fail); }
              
              .thread { color: var(--muted); font-size: 0.7rem; }
              .err code { color: var(--fail); font-size: 0.7rem; word-break: break-all; }
              
              footer { text-align: center; padding: 2rem; color: var(--muted); font-size: 0.65rem; letter-spacing: 0.5px; border-top: 1px solid var(--border); }
            </style>
            </head>
            <body>
            
            <div class="header">
              <div>
                <h1>⚡ Test Execution Report</h1>
                <div class="run-id">Run ID: <strong>%s</strong></div>
              </div>
              <div class="timestamp">Generated<br><strong>%s</strong></div>
            </div>
            
            <div class="cards">
              <div class="card total">
                <div class="card-label">Total Tests</div>
                <div class="card-value">%d</div>
              </div>
              <div class="card pass-c">
                <div class="card-label">Passed</div>
                <div class="card-value">%d</div>
              </div>
              <div class="card fail-c">
                <div class="card-label">Failed</div>
                <div class="card-value">%d</div>
              </div>
              <div class="card rate-c">
                <div class="card-label">Pass Rate</div>
                <div class="card-value">%.0f%%</div>
              </div>
            </div>
            
            <div class="progress-wrap">
              <div class="progress-bar">
                <div class="progress-fill" style="width: %.1f%%"></div>
              </div>
              <div class="progress-label">%.1f%% of tests passed in this run</div>
            </div>
            
            <div class="table-wrap">
              <div class="table-title">Test Results</div>
              <table>
                <thead>
                  <tr>
                    <th>Test Name</th>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Duration</th>
                    <th>Thread</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  %s
                </tbody>
              </table>
            </div>
            
            <footer>Automation Framework v1.0.0 &nbsp;·&nbsp; %s</footer>
            </body>
            </html>
            """.formatted(
                runId,                            // title
                runId,                            // run id in header
                LocalDateTime.now().format(DISPLAY),
                total, passed, failed, rate,      // card values
                rate, rate, rate,                 // progress
                rows.toString(),                  // table rows
                LocalDateTime.now().format(DISPLAY) // footer
        );
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
