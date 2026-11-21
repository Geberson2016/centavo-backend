package br.com.centavo.controller;

import br.com.centavo.dto.RecentTransactionResponse;
import br.com.centavo.dto.TransactionRequest;
import br.com.centavo.dto.TransactionResponse;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.exception.ApiException;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest extends ControllerTestSupport {

    private static final String TRANSACTIONS = "/api/v1/transactions";
    private static final String VALID_BODY = """
            {"description": "Groceries", "value": 120.50, "date": "2026-11-10",
             "accountId": 10, "categoryId": 20, "type": "EXPENSE"}
            """;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(TRANSACTIONS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(transactionService);
    }

    @Test
    @WithMockUser
    void createReturnsCreatedTransaction() throws Exception {
        when(transactionService.createTransaction(new TransactionRequest(
                "Groceries", new BigDecimal("120.50"), LocalDate.of(2026, 11, 10), 10L, 20L,
                TransactionType.EXPENSE)))
                .thenReturn(new TransactionResponse(100L, "Groceries", new BigDecimal("120.50"),
                        LocalDate.of(2026, 11, 10), TransactionType.EXPENSE, 10L, 20L));

        mockMvc.perform(post(TRANSACTIONS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.value").value(120.50))
                .andExpect(jsonPath("$.date").value("2026-11-10"));
    }

    @Test
    @WithMockUser
    void createRejectsNonPositiveValue() throws Exception {
        mockMvc.perform(post(TRANSACTIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("120.50", "-1")))
                .andExpect(problem(400, "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("value"));

        verifyNoInteractions(transactionService);
    }

    @Test
    @WithMockUser
    void createReturnsUnprocessableForCategoryOfTheOppositeType() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenThrow(new ApiException(ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH));

        mockMvc.perform(post(TRANSACTIONS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(problem(422, "TRANSACTION_CATEGORY_TYPE_MISMATCH"));
    }

    @Test
    @WithMockUser
    void createReturnsNotFoundForCategoryOutsideTheUserScope() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(post(TRANSACTIONS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(problem(404, "CATEGORY_NOT_FOUND"));
    }

    // "/recent" não pode ser confundido com "/{id}".
    @Test
    @WithMockUser
    void recentListsLatestTransactions() throws Exception {
        when(transactionService.findRecent()).thenReturn(List.of(new RecentTransactionResponse(
                "Groceries", "Food", LocalDate.of(2026, 11, 10), new BigDecimal("120.50"),
                TransactionType.EXPENSE)));

        mockMvc.perform(get(TRANSACTIONS + "/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    @WithMockUser
    void getReturnsNotFoundProblemForTransactionOutsideTheUserScope() throws Exception {
        when(transactionService.findById(99L)).thenThrow(new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        mockMvc.perform(get(TRANSACTIONS + "/99"))
                .andExpect(problem(404, "TRANSACTION_NOT_FOUND"));
    }
}
