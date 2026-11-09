package br.com.centavo.controller;

import br.com.centavo.api.UsersApi;
import br.com.centavo.dto.AuthRequest;
import br.com.centavo.dto.UserRegisterResponse;
import br.com.centavo.dto.UserRequest;
import br.com.centavo.dto.UserResponse;
import br.com.centavo.service.AuthService;
import br.com.centavo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// Paths, validação e content types vêm da interface gerada a partir do openapi.yaml.
@RestController
public class UserController implements UsersApi {
    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Override
    public ResponseEntity<UserRegisterResponse> register(UserRequest request) {
        UserRegisterResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<UserResponse> login(AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
