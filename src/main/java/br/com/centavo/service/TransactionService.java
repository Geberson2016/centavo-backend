package br.com.centavo.service;

import br.com.centavo.dto.RecentTransactionResponse;
import br.com.centavo.dto.TransactionRequest;
import br.com.centavo.dto.TransactionResponse;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.Transaction;
import br.com.centavo.entity.User;
import br.com.centavo.exception.ApiException;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.repository.AccountRepository;
import br.com.centavo.repository.CategoryRepository;
import br.com.centavo.repository.TransactionRepository;
import br.com.centavo.util.AuthUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AuthUtils authUtils;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            AuthUtils authUtils
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.authUtils = authUtils;
    }

    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        User user = authUtils.getAuthenticatedUser();

        Account account = accountRepository.findByIdAndUserId(transactionRequest.accountId(), user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));

        Category category = categoryRepository.findById(transactionRequest.categoryId())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getType() != transactionRequest.type()) {
            throw new ApiException(ErrorCode.TRANSACTION_CATEGORY_TYPE_MISMATCH);
        }

        Transaction transaction = new Transaction(
                transactionRequest.description(),
                transactionRequest.value(),
                transactionRequest.date(),
                transactionRequest.type(),
                account,
                category
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        return new TransactionResponse(
                savedTransaction.getId(),
                savedTransaction.getDescription(),
                savedTransaction.getValue(),
                savedTransaction.getDate(),
                savedTransaction.getType(),
                savedTransaction.getAccount().getId(),
                savedTransaction.getCategory().getId()
        );
    }

    public List<TransactionResponse> findAll() {
        User user = authUtils.getAuthenticatedUser();

        return transactionRepository.findAllByAccountUserId(user.getId())
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getDescription(),
                        transaction.getValue(),
                        transaction.getDate(),
                        transaction.getType(),
                        transaction.getAccount().getId(),
                        transaction.getCategory().getId()
                ))
                .toList();
    }

    public TransactionResponse findById(Long id) {
        User user = authUtils.getAuthenticatedUser();

        Transaction transaction = transactionRepository.findByIdAndAccountUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));

        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getValue(),
                transaction.getDate(),
                transaction.getType(),
                transaction.getAccount().getId(),
                transaction.getCategory().getId()
        );
    }

    public List<RecentTransactionResponse> findRecent() {
        User user = authUtils.getAuthenticatedUser();
        LocalDate startDate = LocalDate.now().minusDays(30);

        return transactionRepository.findRecentByUserId(user.getId(), startDate)
                .stream()
                .map(p -> new RecentTransactionResponse(
                        p.getDescription(),
                        p.getCategoryName(),
                        p.getDate(),
                        p.getValue(),
                        p.getType()
                ))
                .toList();
    }
}