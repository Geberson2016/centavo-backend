package br.com.centavo.repository;

import br.com.centavo.dto.AccountSummaryProjection;
import br.com.centavo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT a.id AS accountId,
               a.name AS accountName,
               a.type AS accountType,
               COALESCE(SUM(t.value), 0) AS total
        FROM Account a
        LEFT JOIN a.transactions t
        WHERE a.user.id = :userId
        GROUP BY a.id, a.name, a.type
    """)
    List<AccountSummaryProjection> findAllWithTotal(@Param("userId") Long userId);
}
