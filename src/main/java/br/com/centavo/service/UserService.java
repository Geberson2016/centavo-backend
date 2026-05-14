package br.com.centavo.service;

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
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse createUser(UserRequest request) {
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
        String token = jwtService.generateToken(saved);

        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), token);
    }
}