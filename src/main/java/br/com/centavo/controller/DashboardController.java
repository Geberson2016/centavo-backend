package br.com.centavo.controller;

import br.com.centavo.api.DashboardApi;
import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// Paths e content types vêm da interface gerada a partir do openapi.yaml.
@RestController
public class DashboardController implements DashboardApi {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
