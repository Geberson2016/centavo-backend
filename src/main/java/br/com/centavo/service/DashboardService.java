package br.com.centavo.service;

import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.repository.TransactionRepository;
import br.com.centavo.util.AuthUtils;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

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
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);

        BigDecimal allRevenue = nullToZero(transactionRepository.sumByType(TransactionType.RECEITA, userId));
        BigDecimal allExpense = nullToZero(transactionRepository.sumByType(TransactionType.DESPESA, userId));
        BigDecimal totalBalance = allRevenue.subtract(allExpense);

        BigDecimal totalRevenue = nullToZero(transactionRepository.sumByTypeAndDateAfter(TransactionType.RECEITA, firstDayOfMonth, userId));
        BigDecimal totalExpense = nullToZero(transactionRepository.sumByTypeAndDateAfter(TransactionType.DESPESA, firstDayOfMonth, userId));
        BigDecimal creditCardBill = nullToZero(transactionRepository.sumExpensesByAccountTypeAndDateAfter(AccountType.CARTAO_CREDITO, firstDayOfMonth, userId));
        BigDecimal monthlySavings = totalRevenue.subtract(totalExpense);

        return new DashboardSummaryResponse(totalBalance, creditCardBill, monthlySavings, totalExpense, totalRevenue);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}