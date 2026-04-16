package br.com.centavo.repository;

import br.com.centavo.entity.Transaction;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type AND t.date >= :startDate")
    BigDecimal sumByTypeAndDateAfter(@Param("type") TransactionType type, @Param("startDate") LocalDate startDate);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.account.type = :accountType AND t.type = 'DESPESA'")
    BigDecimal sumExpensesByAccountType(@Param("accountType") AccountType accountType);
}
