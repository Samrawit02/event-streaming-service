package com.challenge.eventstream.controller;

import com.challenge.eventstream.dto.AuthResponse;
import com.challenge.eventstream.dto.LoginRequest;
import com.challenge.eventstream.dto.RegisterRequest;
import com.challenge.eventstream.model.User;
import com.challenge.eventstream.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticate user and return JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Register a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "message", "User registered successfully"
        ));
    }
}
