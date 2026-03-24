package br.com.centavo.service;

import br.com.centavo.dto.TransactionRequest;
import br.com.centavo.dto.TransactionResponse;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.Transaction;
import br.com.centavo.entity.User;
import br.com.centavo.repository.AccountRepository;
import br.com.centavo.repository.CategoryRepository;
import br.com.centavo.repository.TransactionRepository;
import br.com.centavo.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        Account account = accountRepository.findById(transactionRequest.accountId())
                .orElseThrow(()-> new RuntimeException("Conta não encontrada"));

        Category category = categoryRepository.findById(transactionRequest.categoryId())
                .orElseThrow(()-> new RuntimeException("Conta não encontrada"));

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

}
