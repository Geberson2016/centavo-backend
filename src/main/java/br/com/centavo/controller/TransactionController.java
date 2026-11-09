package br.com.centavo.controller;

import br.com.centavo.api.TransactionsApi;
import br.com.centavo.dto.RecentTransactionResponse;
import br.com.centavo.dto.TransactionRequest;
import br.com.centavo.dto.TransactionResponse;
import br.com.centavo.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Paths, validação e content types vêm da interface gerada a partir do openapi.yaml.
@RestController
public class TransactionController implements TransactionsApi {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<TransactionResponse>> listTransactions() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @Override
    public ResponseEntity<TransactionResponse> getTransaction(Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @Override
    public ResponseEntity<List<RecentTransactionResponse>> listRecentTransactions() {
        return ResponseEntity.ok(transactionService.findRecent());
    }
}
