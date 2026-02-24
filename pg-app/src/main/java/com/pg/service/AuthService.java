package com.pg.service;

import com.pg.api.dto.LoginResponse;
import com.pg.entity.AppUser;
import com.pg.entity.AuthToken;
import com.pg.repository.AuthTokenRepository;
import com.pg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final int TOKEN_VALID_HOURS = 8;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, AuthTokenRepository authTokenRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<LoginResponse> login(String username, String password) {
        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return Optional.empty();
        AppUser user = userOpt.get();
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPassword()))
            return Optional.empty();
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthToken at = new AuthToken();
        at.setToken(token);
        at.setUserId(user.getId());
        at.setExpiresAt(Instant.now().plusSeconds(TOKEN_VALID_HOURS * 3600L));
        authTokenRepository.save(at);
        LoginResponse res = new LoginResponse();
        res.setToken(token);
        res.setUserId(user.getUsername());
        res.setUserNm(user.getName() != null ? user.getName() : user.getUsername());
        return Optional.of(res);
    }

    public Optional<AppUser> validateToken(String token) {
        if (token == null || token.isEmpty()) return Optional.empty();
        return authTokenRepository.findByTokenAndExpiresAtAfter(token, Instant.now())
                .flatMap(at -> userRepository.findById(at.getUserId()));
    }
}
