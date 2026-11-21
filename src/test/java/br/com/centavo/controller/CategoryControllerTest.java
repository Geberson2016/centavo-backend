package br.com.centavo.controller;

import br.com.centavo.dto.CategoryRequest;
import br.com.centavo.dto.CategoryResponse;
import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.exception.ApiException;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest extends ControllerTestSupport {

    private static final String CATEGORIES = "/api/v1/categories";

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(CATEGORIES)).andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser
    void listReturnsCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(
                new CategoryResponse(30L, "Salary", TransactionType.INCOME, 1L, BudgetType.FIXED)));

        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[0].budgetType").value("FIXED"));
    }

    @Test
    @WithMockUser
    void createReturnsCreatedCategory() throws Exception {
        when(categoryService.createCategory(
                new CategoryRequest("Groceries", TransactionType.EXPENSE, null, BudgetType.VARIABLE)))
                .thenReturn(new CategoryResponse(31L, "Groceries", TransactionType.EXPENSE, 1L, BudgetType.VARIABLE));

        mockMvc.perform(post(CATEGORIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "type": "EXPENSE", "budgetType": "VARIABLE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(31));
    }

    @Test
    @WithMockUser
    void createRejectsUnknownBudgetTypeListingAcceptedValues() throws Exception {
        mockMvc.perform(post(CATEGORIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Groceries", "type": "EXPENSE", "budgetType": "SOMETIMES"}
                                """))
                .andExpect(problem(400, "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("budgetType"))
                .andExpect(jsonPath("$.errors[0].accepted", containsInAnyOrder("FIXED", "VARIABLE")));

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser
    void getReturnsNotFoundProblemForCategoryOutsideTheUserScope() throws Exception {
        when(categoryService.findById(99L)).thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(get(CATEGORIES + "/99"))
                .andExpect(problem(404, "CATEGORY_NOT_FOUND"));
    }
}
