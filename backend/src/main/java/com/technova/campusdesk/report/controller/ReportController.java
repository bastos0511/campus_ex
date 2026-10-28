package com.technova.campusdesk.report.controller;

import com.technova.campusdesk.report.dto.SummaryResponse;
import com.technova.campusdesk.security.AuthUser;
import com.technova.campusdesk.report.service.ReportService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Dashboard numbers.
@RestController
@RequestMapping("/api/reports")
public class ReportController {

  private final ReportService reportService;

  // Spring only injects constructor dependencies.
  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  // Counts by status (scoped by role).
  @GetMapping("/summary")
  public SummaryResponse summary(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthUser actor) {
    return reportService.summary(actor);
  }
}
