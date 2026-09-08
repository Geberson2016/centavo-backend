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

        BigDecimal allRevenue = nullToZero(transactionRepository.sumByType(TransactionType.INCOME, userId));
        BigDecimal allExpense = nullToZero(transactionRepository.sumByType(TransactionType.EXPENSE, userId));
        BigDecimal totalBalance = allRevenue.subtract(allExpense);

        BigDecimal totalRevenue = nullToZero(transactionRepository.sumByTypeAndDateBetween(TransactionType.INCOME, firstDayOfMonth, lastDayOfMonth, userId));
        BigDecimal totalExpense = nullToZero(transactionRepository.sumByTypeAndDateBetween(TransactionType.EXPENSE, firstDayOfMonth, lastDayOfMonth, userId));
        BigDecimal creditCardBill = nullToZero(transactionRepository.sumExpensesByAccountTypeAndDateBetween(AccountType.CREDIT_CARD, firstDayOfMonth, lastDayOfMonth, userId));
        BigDecimal monthlySavings = totalRevenue.subtract(totalExpense);

        BigDecimal scheduledRevenue = nullToZero(transactionRepository.sumScheduledByType(TransactionType.INCOME, lastDayOfMonth, userId));
        BigDecimal scheduledExpense = nullToZero(transactionRepository.sumScheduledByType(TransactionType.EXPENSE, lastDayOfMonth, userId));

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