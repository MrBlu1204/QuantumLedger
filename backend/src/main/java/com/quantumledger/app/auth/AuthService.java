package com.quantumledger.app.auth;

import com.quantumledger.app.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, UserCredentialRepository userCredentialRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
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
}
