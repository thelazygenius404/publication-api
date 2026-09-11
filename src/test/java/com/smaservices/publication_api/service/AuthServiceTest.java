package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.auth.AuthResponse;
import com.smaservices.publication_api.dto.auth.LoginRequest;
import com.smaservices.publication_api.dto.auth.RegisterRequest;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService
        );
    }

    @Test
    void register_shouldNormalizeEmailHashPasswordSaveUserAndReturnToken() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("  Test.User@Example.COM  ");
        request.setPassword("StrongPassword123!");

        when(
                userRepository.existsByEmail(
                        "test.user@example.com"
                )
        ).thenReturn(false);

        when(
                passwordEncoder.encode(
                        "StrongPassword123!"
                )
        ).thenReturn("hashed-password");

        when(
                userRepository.save(any(User.class))
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(
                jwtService.generateToken(
                        any(User.class)
                )
        ).thenReturn("jwt-token");

        AuthResponse response =
                authService.register(request);

        assertNotNull(response);
        assertEquals(
                "jwt-token",
                response.accessToken()
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(
                userCaptor.capture()
        );

        User savedUser =
                userCaptor.getValue();

        assertEquals(
                "test.user@example.com",
                savedUser.getEmail()
        );

        assertEquals(
                "hashed-password",
                savedUser.getPasswordHash()
        );

        verify(passwordEncoder).encode(
                "StrongPassword123!"
        );

        verify(jwtService).generateToken(
                savedUser
        );
    }

    @Test
    void register_shouldRejectDuplicateEmail() {

        RegisterRequest request =
                new RegisterRequest();

        request.setEmail(
                "existing@example.com"
        );

        request.setPassword(
                "StrongPassword123!"
        );

        when(
                userRepository.existsByEmail(
                        "existing@example.com"
                )
        ).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.register(
                                request
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatus()
        );

        assertEquals(
                "EMAIL_ALREADY_REGISTERED",
                exception.getCode()
        );

        verify(
                userRepository,
                never()
        ).save(any());

        verify(
                passwordEncoder,
                never()
        ).encode(anyString());

        verify(
                jwtService,
                never()
        ).generateToken(any());
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "  USER@EXAMPLE.COM "
        );

        request.setPassword(
                "StrongPassword123!"
        );

        User user = new User();

        user.setEmail(
                "user@example.com"
        );

        user.setPasswordHash(
                "hashed-password"
        );

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                jwtService.generateToken(user)
        ).thenReturn("jwt-token");

        AuthResponse response =
                authService.login(request);

        assertEquals(
                "jwt-token",
                response.accessToken()
        );

        verify(
                authenticationManager
        ).authenticate(
                argThat(authentication ->
                        authentication
                                instanceof UsernamePasswordAuthenticationToken
                                &&
                                authentication
                                        .getName()
                                        .equals(
                                                "user@example.com"
                                        )
                                &&
                                authentication
                                        .getCredentials()
                                        .equals(
                                                "StrongPassword123!"
                                        )
                )
        );

        verify(
                userRepository
        ).findByEmail(
                "user@example.com"
        );

        verify(
                jwtService
        ).generateToken(user);
    }

    @Test
    void login_shouldReturnUnauthorizedWhenPasswordIsWrong() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "user@example.com"
        );

        request.setPassword(
                "WrongPassword123!"
        );

        when(
                authenticationManager.authenticate(
                        any(
                                UsernamePasswordAuthenticationToken.class
                        )
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Bad credentials"
                )
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.login(
                                request
                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        assertEquals(
                "INVALID_CREDENTIALS",
                exception.getCode()
        );

        verify(
                userRepository,
                never()
        ).findByEmail(anyString());

        verify(
                jwtService,
                never()
        ).generateToken(any());
    }

    @Test
    void login_shouldReturnUnauthorizedWhenUserCannotBeFoundAfterAuthentication() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "missing@example.com"
        );

        request.setPassword(
                "StrongPassword123!"
        );

        when(
                userRepository.findByEmail(
                        "missing@example.com"
                )
        ).thenReturn(
                Optional.empty()
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> authService.login(
                                request
                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        assertEquals(
                "INVALID_CREDENTIALS",
                exception.getCode()
        );

        verify(
                jwtService,
                never()
        ).generateToken(any());
    }
}