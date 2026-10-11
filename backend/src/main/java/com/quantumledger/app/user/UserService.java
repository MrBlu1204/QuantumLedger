package com.quantumledger.app.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String title, String firstName, String lastName, String emailId, String countryCode, String phone, String rawPassword) {
        String normalizedEmail = emailId.trim().toLowerCase();

        if(userRepository.existsByEmailId(normalizedEmail)){
            throw new DuplicateUserException("Email ID already registered");
        }

        if(userRepository.existsByCountryCodeAndPhone(countryCode, phone)){
            throw new DuplicateUserException("Country Code and Phone already registered");
        }

        User user = new User(
                blankToNull(title),
                firstName,
                blankToNull(lastName),
                normalizedEmail,
                countryCode,
                phone,
                passwordEncoder.encode(rawPassword));

        return userRepository.saveAndFlush(user);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
