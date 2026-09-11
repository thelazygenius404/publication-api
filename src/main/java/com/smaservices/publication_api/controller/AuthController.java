package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.auth.AuthResponse;
import com.smaservices.publication_api.dto.auth.LoginRequest;
import com.smaservices.publication_api.dto.auth.RegisterRequest;
import com.smaservices.publication_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {

        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody
            RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        authService.register(request)
                );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody
            LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}