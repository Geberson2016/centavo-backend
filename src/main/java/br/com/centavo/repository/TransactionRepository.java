package br.com.centavo.repository;

import br.com.centavo.dto.RecentTransactionProjection;
import br.com.centavo.entity.Transaction;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByAccountUserId(Long userId);

    Optional<Transaction> findByIdAndAccountUserId(Long id, Long userId);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type AND t.account.user.id = :userId")
    BigDecimal sumByType(@Param("type") TransactionType type, @Param("userId") Long userId);

    @Query("""
        SELECT t.description AS description,
               t.category.name AS categoryName,
               t.date AS date,
               t.value AS value,
               t.type AS type
        FROM Transaction t
        WHERE t.account.user.id = :userId
          AND t.date >= :startDate
        ORDER BY t.date DESC, t.id DESC
    """)
    List<RecentTransactionProjection> findRecentByUserId(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type AND t.date >= :startDate AND t.date <= :endDate AND t.account.user.id = :userId")
    BigDecimal sumByTypeAndDateBetween(@Param("type") TransactionType type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("userId") Long userId);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.account.type = :accountType AND t.type = 'DESPESA' AND t.account.user.id = :userId AND t.date >= :startDate AND t.date <= :endDate")
    BigDecimal sumExpensesByAccountTypeAndDateBetween(@Param("accountType") AccountType accountType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("userId") Long userId);

    @Query("SELECT SUM(t.value) FROM Transaction t WHERE t.type = :type AND t.date > :endDate AND t.account.user.id = :userId")
    BigDecimal sumScheduledByType(@Param("type") TransactionType type, @Param("endDate") LocalDate endDate, @Param("userId") Long userId);
}
