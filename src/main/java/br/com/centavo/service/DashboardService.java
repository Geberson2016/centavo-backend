package br.com.centavo.service;

import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.repository.TransactionRepository;
import br.com.centavo.util.AuthUtils;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AuthUtils authUtils;

    public DashboardService(TransactionRepository transactionRepository, AuthUtils authUtils) {
        this.transactionRepository = transactionRepository;
        this.authUtils = authUtils;
    }

    public DashboardSummaryResponse getSummary() {
        Long userId = authUtils.getAuthenticatedUser().getId();
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        // Saldo total histórico (inclui futuros)
        BigDecimal allRevenue = nullToZero(transactionRepository.sumByType(TransactionType.RECEITA, userId));
        BigDecimal allExpense = nullToZero(transactionRepository.sumByType(TransactionType.DESPESA, userId));
        BigDecimal totalBalance = allRevenue.subtract(allExpense);

        // Mês atual — só até hoje
        BigDecimal totalRevenue = nullToZero(transactionRepository.sumByTypeAndDateBetween(TransactionType.RECEITA, firstDayOfMonth, lastDayOfMonth, userId));
        BigDecimal totalExpense = nullToZero(transactionRepository.sumByTypeAndDateBetween(TransactionType.DESPESA, firstDayOfMonth, lastDayOfMonth, userId));
        BigDecimal creditCardBill = nullToZero(transactionRepository.sumExpensesByAccountTypeAndDateBetween(AccountType.CARTAO_CREDITO, firstDayOfMonth, lastDayOfMonth, userId));
        BigDecimal monthlySavings = totalRevenue.subtract(totalExpense);

        // Previsões (datas futuras)
        BigDecimal scheduledRevenue = nullToZero(transactionRepository.sumScheduledByType(TransactionType.RECEITA, lastDayOfMonth, userId));
        BigDecimal scheduledExpense = nullToZero(transactionRepository.sumScheduledByType(TransactionType.DESPESA, lastDayOfMonth, userId));

        return new DashboardSummaryResponse(
                totalBalance, creditCardBill, monthlySavings,
                totalExpense, totalRevenue,
                scheduledRevenue, scheduledExpense
        );
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}