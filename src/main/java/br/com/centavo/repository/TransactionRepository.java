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

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type AND t.account.user.id = :userId")
    BigDecimal sumByType(@Param("type") TransactionType type, @Param("userId") Long userId);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type AND t.date >= :startDate AND t.account.user.id = :userId")
    BigDecimal sumByTypeAndDateAfter(@Param("type") TransactionType type, @Param("startDate") LocalDate startDate, @Param("userId") Long userId);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.account.type = :accountType AND t.type = 'DESPESA' AND t.account.user.id = :userId")
    BigDecimal sumExpensesByAccountType(@Param("accountType") AccountType accountType, @Param("userId") Long userId);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.account.type = :accountType AND t.type = 'DESPESA' AND t.account.user.id = :userId AND t.date >= :startDate")
    BigDecimal sumExpensesByAccountTypeAndDateAfter(@Param("accountType") AccountType accountType, @Param("startDate") LocalDate startDate, @Param("userId") Long userId);
}
