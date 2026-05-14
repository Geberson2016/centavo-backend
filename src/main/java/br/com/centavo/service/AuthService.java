package br.com.centavo.service;

import br.com.centavo.dto.AuthRequest;
import br.com.centavo.dto.UserResponse;
import br.com.centavo.entity.User;
import br.com.centavo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("E-mail ou senha inválidos");
        }

        String token = jwtService.generateToken(user);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), token);
    }
}