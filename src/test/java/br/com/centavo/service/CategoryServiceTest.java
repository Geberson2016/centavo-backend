package br.com.centavo.service;

import br.com.centavo.dto.CategoryRequest;
import br.com.centavo.dto.CategoryResponse;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.User;
import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.repository.CategoryRepository;
import br.com.centavo.support.Fixtures;
import br.com.centavo.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.centavo.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthUtils authUtils;

    @InjectMocks
    private CategoryService categoryService;

    private User ana;

    @BeforeEach
    void setUp() {
        ana = Fixtures.user(1L, "ana@centavo.test");
        when(authUtils.getAuthenticatedUser()).thenReturn(ana);
    }

    @Test
    void createCategoryAssignsTheAuthenticatedUserIgnoringRequestUserId() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(30L);
            return category;
        });

        CategoryResponse response = categoryService.createCategory(
                new CategoryRequest("Groceries", TransactionType.EXPENSE, 999L, BudgetType.VARIABLE));

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(ana);
        assertThat(response).isEqualTo(new CategoryResponse(
                30L, "Groceries", TransactionType.EXPENSE, 1L, BudgetType.VARIABLE));
    }

    @Test
    void findByIdDoesNotReturnAnotherUsersCategory() {
        when(categoryRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.empty());

        assertApiError(() -> categoryService.findById(40L), ErrorCode.CATEGORY_NOT_FOUND);
    }
}
