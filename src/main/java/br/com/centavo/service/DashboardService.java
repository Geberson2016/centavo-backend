package br.com.centavo.service;

import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public DashboardSummaryResponse getSummary() {
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);

        BigDecimal allRevenue = nullToZero(transactionRepository.sumByType(TransactionType.RECEITA));
        BigDecimal allExpense = nullToZero(transactionRepository.sumByType(TransactionType.DESPESA));
        BigDecimal totalBalance = allRevenue.subtract(allExpense);

        BigDecimal creditCardBill = nullToZero(transactionRepository.sumExpensesByAccountType(AccountType.CARTAO_CREDITO));

        BigDecimal monthRevenue = nullToZero(transactionRepository.sumByTypeAndDateAfter(TransactionType.RECEITA, firstDayOfMonth));
        BigDecimal monthExpense = nullToZero(transactionRepository.sumByTypeAndDateAfter(TransactionType.DESPESA, firstDayOfMonth));
        BigDecimal monthlySavings = monthRevenue.subtract(monthExpense);

        return new DashboardSummaryResponse(totalBalance, creditCardBill, monthlySavings);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}