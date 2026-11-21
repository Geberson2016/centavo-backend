package br.com.centavo.controller;

import br.com.centavo.dto.AuthRequest;
import br.com.centavo.dto.UserRegisterResponse;
import br.com.centavo.dto.UserRequest;
import br.com.centavo.dto.UserResponse;
import br.com.centavo.exception.ApiException;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.service.AuthService;
import br.com.centavo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cadastro e login são públicos: nenhum teste aqui manda token.
@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {

    private static final String REGISTER = "/api/v1/users/register";
    private static final String LOGIN = "/api/v1/users/login";

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerCreatesUser() throws Exception {
        when(userService.createUser(any(UserRequest.class))).thenReturn(new UserRegisterResponse("Ana"));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@centavo.test", "password": "secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ana"));
    }

    @Test
    void registerReportsEveryInvalidField() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email", "password": "123"}
                                """))
                .andExpect(problem(400, "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "email", "password")));

        verifyNoInteractions(userService);
    }

    @Test
    void registerReturnsConflictForEmailAlreadyRegistered() throws Exception {
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@centavo.test", "password": "secret123"}
                                """))
                .andExpect(problem(409, "EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void malformedJsonIsAValidationError() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(problem(400, "VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void loginReturnsUserAndToken() throws Exception {
        when(authService.login(new AuthRequest("ana@centavo.test", "secret123")))
                .thenReturn(new UserResponse(1L, "Ana", "ana@centavo.test", "jwt"));

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@centavo.test", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new ApiException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@centavo.test", "password": "wrong-password"}
                                """))
                .andExpect(problem(401, "INVALID_CREDENTIALS"));
    }
}
