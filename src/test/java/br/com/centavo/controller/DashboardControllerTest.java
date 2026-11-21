package br.com.centavo.controller;

import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest extends ControllerTestSupport {

    private static final String SUMMARY = "/api/v1/dashboard/summary";

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(SUMMARY)).andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser
    void summaryReturnsEveryIndicator() throws Exception {
        when(dashboardService.getSummary()).thenReturn(new DashboardSummaryResponse(
                new BigDecimal("4579.50"), new BigDecimal("120.50"), new BigDecimal("4879.50"),
                new BigDecimal("120.50"), new BigDecimal("5000.00"), BigDecimal.ZERO,
                new BigDecimal("300.00")));

        mockMvc.perform(get(SUMMARY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(4579.50))
                .andExpect(jsonPath("$.creditCardBill").value(120.50))
                .andExpect(jsonPath("$.monthlySavings").value(4879.50))
                .andExpect(jsonPath("$.totalExpense").value(120.50))
                .andExpect(jsonPath("$.totalRevenue").value(5000.00))
                .andExpect(jsonPath("$.scheduledRevenue").value(0))
                .andExpect(jsonPath("$.scheduledExpense").value(300.00));
    }
}
