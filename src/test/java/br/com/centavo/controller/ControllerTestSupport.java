package br.com.centavo.controller;

import br.com.centavo.config.SecurityConfig;
import br.com.centavo.service.JwtService;
import br.com.centavo.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Base dos testes de fatia web. Importa o SecurityConfig real (rotas públicas, 403 sem token,
// CSRF desligado) em vez da segurança padrão do Boot. O JwtAuthFilter entra na fatia por ser
// um Filter, então as dependências dele viram mocks.
@Import(SecurityConfig.class)
abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UserDetailsServiceImpl userDetailsService;

    // Resposta RFC 7807 com o status e o code estável esperados.
    protected static ResultMatcher problem(int status, String code) {
        return result -> {
            status().is(status).match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.status").value(status).match(result);
            jsonPath("$.code").value(code).match(result);
        };
    }
}
