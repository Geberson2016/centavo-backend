package br.com.centavo.controller;

import br.com.centavo.api.AccountsApi;
import br.com.centavo.dto.AccountRequest;
import br.com.centavo.dto.AccountResponse;
import br.com.centavo.dto.AccountSummaryResponse;
import br.com.centavo.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Paths, validação e content types vêm da interface gerada a partir do openapi.yaml.
@RestController
public class AccountController implements AccountsApi {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<AccountResponse> createAccount(AccountRequest request) {
        AccountResponse response = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        return ResponseEntity.ok(accountService.findAll());
    }

    @Override
    public ResponseEntity<AccountResponse> getAccount(Long id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @Override
    public ResponseEntity<List<AccountSummaryResponse>> getAccountSummary() {
        return ResponseEntity.ok(accountService.findAccountsSummary());
    }
}
