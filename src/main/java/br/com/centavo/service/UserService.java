package br.com.centavo.service;

import br.com.centavo.dto.UserRegisterResponse;
import br.com.centavo.dto.UserRequest;
import br.com.centavo.dto.UserResponse;
import br.com.centavo.entity.User;
import br.com.centavo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserRegisterResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("E-mail já cadastrado");
        }

        User user = new User(
                request.name(),
                request.email(),
                request.phone(),
                passwordEncoder.encode(request.password())
        );

        User saved = userRepository.save(user);

        return new UserRegisterResponse(saved.getName());
    }
}