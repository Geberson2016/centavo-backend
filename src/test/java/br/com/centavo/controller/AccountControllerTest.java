package br.com.centavo.controller;

import br.com.centavo.dto.AccountRequest;
import br.com.centavo.dto.AccountResponse;
import br.com.centavo.entity.User;
import br.com.centavo.enums.AccountType;
import br.com.centavo.exception.ApiException;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.service.AccountService;
import br.com.centavo.support.Fixtures;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Além das rotas de conta, cobre o caminho real do JwtAuthFilter (token válido, inválido e de
// usuário removido) e o fallback de erro inesperado.
@WebMvcTest(AccountController.class)
class AccountControllerTest extends ControllerTestSupport {

    private static final String ACCOUNTS = "/api/v1/accounts";

    @MockitoBean
    private AccountService accountService;

    // Sem token, o Spring responde 403 sem corpo (o BFF traduz para 401 UNAUTHENTICATED).
    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get(ACCOUNTS))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verifyNoInteractions(accountService);
    }

    @Test
    void authenticatesWithValidBearerToken() throws Exception {
        User ana = Fixtures.user(1L, "ana@centavo.test");
        when(jwtService.validateToken("valid-token")).thenReturn("ana@centavo.test");
        when(userDetailsService.loadUserByUsername("ana@centavo.test")).thenReturn(ana);
        when(accountService.findAll())
                .thenReturn(List.of(new AccountResponse(10L, "Checking", AccountType.CHECKING, 1L)));

        mockMvc.perform(get(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("CHECKING"));
    }

    @Test
    void rejectsInvalidOrExpiredToken() throws Exception {
        when(jwtService.validateToken("forged-token")).thenThrow(new JWTVerificationException("invalid"));

        mockMvc.perform(get(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, "Bearer forged-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsTokenOfUserThatNoLongerExists() throws Exception {
        when(jwtService.validateToken("orphan-token")).thenReturn("gone@centavo.test");
        when(userDetailsService.loadUserByUsername("gone@centavo.test"))
                .thenThrow(new UsernameNotFoundException("gone@centavo.test"));

        mockMvc.perform(get(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, "Bearer orphan-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser
    void createReturnsCreatedAccount() throws Exception {
        when(accountService.create(new AccountRequest("Wallet", AccountType.CASH, null)))
                .thenReturn(new AccountResponse(11L, "Wallet", AccountType.CASH, 1L));

        mockMvc.perform(post(ACCOUNTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Wallet", "type": "CASH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post(ACCOUNTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "type": "CASH"}
                                """))
                .andExpect(problem(400, "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        verifyNoInteractions(accountService);
    }

    // Valor fora do enum devolve o campo e os valores aceitos como dado, não como frase.
    @Test
    @WithMockUser
    void createRejectsUnknownAccountTypeListingAcceptedValues() throws Exception {
        mockMvc.perform(post(ACCOUNTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Wallet", "type": "GOLD"}
                                """))
                .andExpect(problem(400, "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("type"))
                .andExpect(jsonPath("$.errors[0].accepted", hasItems("CHECKING", "CREDIT_CARD", "CASH")));
    }

    @Test
    @WithMockUser
    void getReturnsNotFoundProblemForAccountOutsideTheUserScope() throws Exception {
        when(accountService.findById(99L)).thenThrow(new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));

        mockMvc.perform(get(ACCOUNTS + "/99"))
                .andExpect(problem(404, "ACCOUNT_NOT_FOUND"));
    }

    // O fallback não vaza a mensagem interna da exceção.
    @Test
    @WithMockUser
    void unexpectedErrorBecomesInternalErrorWithoutDetails() throws Exception {
        when(accountService.create(any(AccountRequest.class)))
                .thenThrow(new IllegalStateException("connection refused: db-host:5432"));

        mockMvc.perform(post(ACCOUNTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Wallet", "type": "CASH"}
                                """))
                .andExpect(problem(500, "INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }
}
