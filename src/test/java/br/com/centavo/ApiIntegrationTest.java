package br.com.centavo;

import br.com.centavo.dto.AccountSummaryResponse;
import br.com.centavo.dto.DashboardSummaryResponse;
import br.com.centavo.dto.RecentTransactionResponse;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pilha inteira: HTTP (MockMvc) → filtro JWT real → services → JPA → Postgres (Testcontainers),
// com o schema das migrações. Os dados não são desfeitos entre os testes, então cada teste cria
// usuários com e-mail único e só olha os dados deles.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndDashboardReflectTheUserTransactions() throws Exception {
        String token = registerAndLogin("ana");
        long checking = createAccount(token, "Checking", "CHECKING");
        long card = createAccount(token, "Card", "CREDIT_CARD");
        long salary = createCategory(token, "Salary", "INCOME", "FIXED");
        long food = createCategory(token, "Food", "EXPENSE", "VARIABLE");

        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.with(TemporalAdjusters.firstDayOfNextMonth());
        createTransaction(token, "Salary", "5000.00", today, checking, salary, "INCOME")
                .andExpect(status().isCreated());
        createTransaction(token, "Groceries", "120.50", today, card, food, "EXPENSE")
                .andExpect(status().isCreated());
        createTransaction(token, "Rent", "300.00", nextMonth, checking, food, "EXPENSE")
                .andExpect(status().isCreated());

        DashboardSummaryResponse summary = read(
                authorized(get("/api/v1/dashboard/summary"), token), DashboardSummaryResponse.class);
        assertThat(summary.totalBalance()).isEqualByComparingTo("4579.50");
        assertThat(summary.totalRevenue()).isEqualByComparingTo("5000.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("120.50");
        assertThat(summary.monthlySavings()).isEqualByComparingTo("4879.50");
        assertThat(summary.creditCardBill()).isEqualByComparingTo("120.50");
        assertThat(summary.scheduledRevenue()).isEqualByComparingTo("0");
        assertThat(summary.scheduledExpense()).isEqualByComparingTo("300.00");

        Map<String, AccountSummaryResponse> accounts = read(
                authorized(get("/api/v1/accounts/accountSummary"), token),
                new TypeReference<List<AccountSummaryResponse>>() { })
                .stream()
                .collect(Collectors.toMap(AccountSummaryResponse::accountName, Function.identity()));
        assertThat(accounts.get("Checking").totalRevenue()).isEqualByComparingTo("5000.00");
        assertThat(accounts.get("Checking").totalExpense()).isEqualByComparingTo("300.00");
        assertThat(accounts.get("Card").totalExpense()).isEqualByComparingTo("120.50");

        List<RecentTransactionResponse> recent = read(
                authorized(get("/api/v1/transactions/recent"), token),
                new TypeReference<List<RecentTransactionResponse>>() { });
        assertThat(recent).extracting(RecentTransactionResponse::description)
                .containsExactly("Rent", "Groceries", "Salary");
        assertThat(recent.getFirst().categoryName()).isEqualTo("Food");
    }

    // Invariante de segurança: um usuário não lê nem usa dados de outro.
    @Test
    void usersCannotReadOrUseEachOtherData() throws Exception {
        String ana = registerAndLogin("ana");
        long anaAccount = createAccount(ana, "Ana checking", "CHECKING");
        long anaCategory = createCategory(ana, "Ana food", "EXPENSE", "VARIABLE");
        long anaTransaction = id(createTransaction(
                ana, "Ana lunch", "30.00", LocalDate.now(), anaAccount, anaCategory, "EXPENSE"));

        String bob = registerAndLogin("bob");
        long bobAccount = createAccount(bob, "Bob checking", "CHECKING");
        long bobCategory = createCategory(bob, "Bob food", "EXPENSE", "VARIABLE");

        expectProblem(authorized(get("/api/v1/accounts/" + anaAccount), bob), 404, "ACCOUNT_NOT_FOUND");
        expectProblem(authorized(get("/api/v1/categories/" + anaCategory), bob), 404, "CATEGORY_NOT_FOUND");
        expectProblem(authorized(get("/api/v1/transactions/" + anaTransaction), bob), 404, "TRANSACTION_NOT_FOUND");

        mockMvc.perform(authorized(get("/api/v1/accounts"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(bobAccount));

        createTransaction(bob, "Sneaky", "1.00", LocalDate.now(), anaAccount, bobCategory, "EXPENSE")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        createTransaction(bob, "Sneaky", "1.00", LocalDate.now(), bobAccount, anaCategory, "EXPENSE")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void transactionWithCategoryOfTheOppositeTypeIsRejected() throws Exception {
        String token = registerAndLogin("ana");
        long account = createAccount(token, "Checking", "CHECKING");
        long salary = createCategory(token, "Salary", "INCOME", "FIXED");

        createTransaction(token, "Groceries", "50.00", LocalDate.now(), account, salary, "EXPENSE")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CATEGORY_TYPE_MISMATCH"));
    }

    @Test
    void registeringTheSameEmailTwiceIsAConflict() throws Exception {
        String email = uniqueEmail("ana");
        register(email).andExpect(status().isCreated());

        register(email)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        String email = uniqueEmail("ana");
        register(email).andExpect(status().isCreated());

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // Sem token, token forjado ou expirado: 403 sem corpo (o BFF traduz para 401).
    @Test
    void protectedRoutesRejectMissingForgedAndExpiredTokens() throws Exception {
        String email = uniqueEmail("ana");
        register(email).andExpect(status().isCreated());
        String forged = JWT.create().withIssuer("centavo-api").withSubject(email)
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("not-the-server-secret"));
        String expired = JWT.create().withIssuer("centavo-api").withSubject(email)
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256("test-only-secret-do-not-use-elsewhere"));

        mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isForbidden());
        mockMvc.perform(authorized(get("/api/v1/accounts"), forged)).andExpect(status().isForbidden());
        mockMvc.perform(authorized(get("/api/v1/accounts"), expired)).andExpect(status().isForbidden());
    }

    private String registerAndLogin(String name) throws Exception {
        String email = uniqueEmail(name);
        register(email).andExpect(status().isCreated());
        JsonNode body = objectMapper.readTree(login(email, "secret123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Test user", "email", email, "password", "secret123"))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private long createAccount(String token, String name, String type) throws Exception {
        return id(mockMvc.perform(authorized(post("/api/v1/accounts"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", name, "type", type)))));
    }

    private long createCategory(String token, String name, String type, String budgetType) throws Exception {
        return id(mockMvc.perform(authorized(post("/api/v1/categories"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", name, "type", type, "budgetType", budgetType)))));
    }

    private ResultActions createTransaction(String token, String description, String value, LocalDate date,
                                            long accountId, long categoryId, String type) throws Exception {
        return mockMvc.perform(authorized(post("/api/v1/transactions"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "description", description,
                        "value", new BigDecimal(value),
                        "date", date.toString(),
                        "accountId", accountId,
                        "categoryId", categoryId,
                        "type", type))));
    }

    private void expectProblem(RequestBuilder request, int status, String code) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    private long id(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private <T> T read(RequestBuilder request, Class<T> type) throws Exception {
        return objectMapper.readValue(body(request), type);
    }

    private <T> T read(RequestBuilder request, TypeReference<T> type) throws Exception {
        return objectMapper.readValue(body(request), type);
    }

    private String body(RequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request,
                                                            String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private static String uniqueEmail(String name) {
        return name + "-" + UUID.randomUUID() + "@centavo.test";
    }
}
