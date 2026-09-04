package com.marketpulse.auth.service;

import com.marketpulse.auth.dto.AuthResponse;
import com.marketpulse.auth.dto.LoginRequest;
import com.marketpulse.auth.dto.RegisterRequest;
import com.marketpulse.common.exception.ApiException;
import com.marketpulse.common.exception.DuplicateResourceException;
import com.marketpulse.common.exception.InvalidCredentialsException;
import com.marketpulse.common.security.JwtService;
import com.marketpulse.user.entity.UserAccount;
import com.marketpulse.user.repository.UserRepository;
import com.marketpulse.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Passwords do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token, userService.toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token, userService.toDto(user));
    }
}
