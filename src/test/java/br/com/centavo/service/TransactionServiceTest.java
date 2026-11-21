package br.com.centavo.service;

import br.com.centavo.dto.TransactionRequest;
import br.com.centavo.dto.TransactionResponse;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.Transaction;
import br.com.centavo.entity.User;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.repository.AccountRepository;
import br.com.centavo.repository.CategoryRepository;
import br.com.centavo.repository.TransactionRepository;
import br.com.centavo.support.Fixtures;
import br.com.centavo.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.com.centavo.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthUtils authUtils;

    @InjectMocks
    private TransactionService transactionService;

    private User ana;

    @BeforeEach
    void setUp() {
        ana = Fixtures.user(1L, "ana@centavo.test");
        when(authUtils.getAuthenticatedUser()).thenReturn(ana);
    }

    @Test
    void createTransactionPersistsWhenAccountAndCategoryBelongToTheUser() {
        Account account = Fixtures.account(10L, AccountType.CHECKING, ana);
        Category category = Fixtures.category(20L, TransactionType.EXPENSE, ana);
        when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(100L);
            return transaction;
        });

        TransactionResponse response = transactionService.createTransaction(expense(10L, 20L));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getAccount()).isSameAs(account);
        assertThat(saved.getValue().getCategory()).isSameAs(category);
        assertThat(response).isEqualTo(new TransactionResponse(
                100L, "Groceries", new BigDecimal("120.50"), DATE, TransactionType.EXPENSE, 10L, 20L));
    }

    @Test
    void createTransactionRejectsAccountOfAnotherUser() {
        when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertApiError(() -> transactionService.createTransaction(expense(10L, 20L)),
                ErrorCode.ACCOUNT_NOT_FOUND);

        verifyNoInteractions(categoryRepository);
        verify(transactionRepository, never()).save(any());
    }

    // Regressão do fix 731aac6: a categoria também é buscada escopada ao usuário.
    @Test
    void createTransactionRejectsCategoryOfAnotherUser() {
        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(Fixtures.account(10L, AccountType.CHECKING, ana)));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

        assertApiError(() -> transactionService.createTransaction(expense(10L, 20L)),
                ErrorCode.CATEGORY_NOT_FOUND);

        verify(categoryRepository, never()).findById(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransactionRejectsCategoryOfTheOppositeType() {
        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(Fixtures.account(10L, AccountType.CHECKING, ana)));
        when(categoryRepository.findByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(Fixtures.category(20L, TransactionType.INCOME, ana)));

        assertApiError(() -> transactionService.createTransaction(expense(10L, 20L)),
                ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void findByIdDoesNotReturnAnotherUsersTransaction() {
        when(transactionRepository.findByIdAndAccountUserId(100L, 1L)).thenReturn(Optional.empty());

        assertApiError(() -> transactionService.findById(100L), ErrorCode.TRANSACTION_NOT_FOUND);
    }

    @Test
    void findRecentLooksBackThirtyDays() {
        LocalDate expectedStart = LocalDate.now().minusDays(30);
        when(transactionRepository.findRecentByUserId(1L, expectedStart)).thenReturn(List.of());

        assertThat(transactionService.findRecent()).isEmpty();
    }

    private static TransactionRequest expense(long accountId, long categoryId) {
        return new TransactionRequest(
                "Groceries", new BigDecimal("120.50"), DATE, accountId, categoryId, TransactionType.EXPENSE);
    }
}
