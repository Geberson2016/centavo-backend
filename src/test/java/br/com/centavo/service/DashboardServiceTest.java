package br.com.centavo.service;

import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.repository.TransactionRepository;
import br.com.centavo.support.Fixtures;
import br.com.centavo.util.AuthUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthUtils authUtils;

    @InjectMocks
    private DashboardService dashboardService;

    // Os stubs usam os argumentos exatos: o Mockito estrito falha se o service consultar
    // outro usuário ou outro intervalo de datas.
    @Test
    void summaryCombinesTotalsAndTreatsMissingSumsAsZero() {
        when(authUtils.getAuthenticatedUser()).thenReturn(Fixtures.user(1L, "ana@centavo.test"));
        LocalDate today = LocalDate.now();
        LocalDate first = today.withDayOfMonth(1);
        LocalDate last = today.with(TemporalAdjusters.lastDayOfMonth());

        when(transactionRepository.sumByType(TransactionType.INCOME, 1L)).thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE, 1L)).thenReturn(new BigDecimal("420.50"));
        when(transactionRepository.sumByTypeAndDateBetween(TransactionType.INCOME, first, last, 1L))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumByTypeAndDateBetween(TransactionType.EXPENSE, first, last, 1L))
                .thenReturn(new BigDecimal("120.50"));
        // SUM sem linhas volta null do banco.
        when(transactionRepository.sumExpensesByAccountTypeAndDateBetween(AccountType.CREDIT_CARD, first, last, 1L))
                .thenReturn(null);
        when(transactionRepository.sumScheduledByType(TransactionType.INCOME, last, 1L)).thenReturn(null);
        when(transactionRepository.sumScheduledByType(TransactionType.EXPENSE, last, 1L))
                .thenReturn(new BigDecimal("300.00"));

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.totalBalance()).isEqualByComparingTo("4579.50");
        assertThat(summary.totalRevenue()).isEqualByComparingTo("5000.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("120.50");
        assertThat(summary.monthlySavings()).isEqualByComparingTo("4879.50");
        assertThat(summary.creditCardBill()).isEqualByComparingTo("0");
        assertThat(summary.scheduledRevenue()).isEqualByComparingTo("0");
        assertThat(summary.scheduledExpense()).isEqualByComparingTo("300.00");
    }
}
