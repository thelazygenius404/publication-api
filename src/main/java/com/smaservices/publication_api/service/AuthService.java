package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.auth.AuthResponse;
import com.smaservices.publication_api.dto.auth.LoginRequest;
import com.smaservices.publication_api.dto.auth.RegisterRequest;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.AuthenticationException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager =
                authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(
            RegisterRequest request) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        if (userRepository.existsByEmail(email)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_REGISTERED",
                    "Un compte existe déjà avec cette adresse email."
            );
        }

        User user = new User();

        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        User saved =
                userRepository.save(user);

        String token =
                jwtService.generateToken(saved);

        return new AuthResponse(token);
    }

    public AuthResponse login(
            LoginRequest request) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Email ou mot de passe incorrect."
            );
        }

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new ApiException(
                                        HttpStatus.UNAUTHORIZED,
                                        "INVALID_CREDENTIALS",
                                        "Email ou mot de passe incorrect."
                                )
                        );

        String token =
                jwtService.generateToken(user);

        return new AuthResponse(token);
    }

    private String normalizeEmail(
            String email) {

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}