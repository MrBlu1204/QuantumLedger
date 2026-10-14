package com.quantumledger.app.auth;

import com.quantumledger.app.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    public record Session(User user, JwtService.AccessToken access, RefreshTokenService.IssuedRefreshToken refresh) {}

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       UserCredentialRepository userCredentialRepository,
                       PasswordEncoder passwordEncoder,
                       UserService userService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        // A real BCrypt hash of a random value nobody knows. Used to keep timing equal.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailId(normalizedEmail);
        Optional<UserCredential> credOpt = userOpt.flatMap(u -> userCredentialRepository.findById(u.getUserId()));

        if(userOpt.isEmpty() || credOpt.isEmpty()) {
            passwordEncoder.matches(rawPassword, dummyHash);
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();

        if(!passwordEncoder.matches(rawPassword, credOpt.get().getPasswordHash()) || user.isDeleted()){
            throw new InvalidCredentialsException();
        }

        if(!"ACTIVE".equals((user.getStatus()))){
            throw new AccountNotActiveException("Account is " + user.getStatus().toLowerCase()) ;
        }

        return user;
    }

    public Session refresh(String rawRefreshToken) {

        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);

        User user;

        try{
            user = userService.getActiveUser(rotation.userId());
        } catch (AccountNotActiveException e){
            refreshTokenService.revokeFamily(rotation.familyId());
            throw e;
        }

        return new Session(user,
                jwtService.createAccessToken(user),
                rotation.newToken());
    }
}
