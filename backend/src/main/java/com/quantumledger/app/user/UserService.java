package com.quantumledger.app.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService{

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

   public UserService(UserRepository userRepository, UserCredentialRepository userCredentialRepository, PasswordEncoder passwordEncoder) {
       this.userRepository = userRepository;
       this.userCredentialRepository = userCredentialRepository;
       this.passwordEncoder = passwordEncoder;
   }

   @Transactional
   public User register(String title, String firstName, String lastName, String email,
                        String countryCode, String phone, String rawPassword) {

       String normalizedEmail = email.trim().toLowerCase();

       if(userRepository.existsByEmailId(normalizedEmail)){
           throw new DuplicateUserException("Email already registered");
       }

       if(userRepository.existsByCountryCodeAndPhone(countryCode, phone)){
           throw new DuplicateUserException("Phone already registered");
       }

       User user = userRepository.saveAndFlush(new User(
               blankToNull(title),
               firstName,
               blankToNull(lastName),
               normalizedEmail,
               countryCode,
               phone));

       userCredentialRepository.save(new UserCredential(user.getUserId(),
               passwordEncoder.encode(rawPassword)));

       return user;
   }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
