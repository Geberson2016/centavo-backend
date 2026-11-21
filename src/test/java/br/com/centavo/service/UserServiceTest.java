package br.com.centavo.service;

import br.com.centavo.dto.UserRegisterResponse;
import br.com.centavo.dto.UserRequest;
import br.com.centavo.entity.User;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static br.com.centavo.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UserRequest REQUEST =
            new UserRequest("Ana", "ana@centavo.test", "11999990000", "secret123");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserStoresOnlyTheEncodedPassword() {
        when(userRepository.existsByEmail("ana@centavo.test")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegisterResponse response = userService.createUser(REQUEST);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@centavo.test");
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
        assertThat(response.name()).isEqualTo("Ana");
    }

    @Test
    void createUserRejectsEmailAlreadyRegistered() {
        when(userRepository.existsByEmail("ana@centavo.test")).thenReturn(true);

        assertApiError(() -> userService.createUser(REQUEST), ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
