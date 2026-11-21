package br.com.centavo.service;

import br.com.centavo.dto.AuthRequest;
import br.com.centavo.dto.UserResponse;
import br.com.centavo.entity.User;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.repository.UserRepository;
import br.com.centavo.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static br.com.centavo.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final AuthRequest REQUEST = new AuthRequest("ana@centavo.test", "secret123");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsUserAndTokenForValidCredentials() {
        User ana = Fixtures.user(1L, "ana@centavo.test");
        when(userRepository.findByEmail("ana@centavo.test")).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("secret123", ana.getPassword())).thenReturn(true);
        when(jwtService.generateToken(ana)).thenReturn("jwt");

        UserResponse response = authService.login(REQUEST);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ana@centavo.test");
        assertThat(response.token()).isEqualTo("jwt");
    }

    // E-mail desconhecido e senha errada dão o mesmo erro, para não revelar quem tem conta.
    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("ana@centavo.test")).thenReturn(Optional.empty());

        assertApiError(() -> authService.login(REQUEST), ErrorCode.INVALID_CREDENTIALS);

        verifyNoInteractions(jwtService);
    }

    @Test
    void loginRejectsWrongPassword() {
        User ana = Fixtures.user(1L, "ana@centavo.test");
        when(userRepository.findByEmail("ana@centavo.test")).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("secret123", ana.getPassword())).thenReturn(false);

        assertApiError(() -> authService.login(REQUEST), ErrorCode.INVALID_CREDENTIALS);

        verifyNoInteractions(jwtService);
    }
}
