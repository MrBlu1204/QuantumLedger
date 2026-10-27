package com.quantumledger.app.user;

import com.quantumledger.app.auth.AccountNotActiveException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService{

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

   public UserService(UserRepository userRepository,
                      UserCredentialRepository userCredentialRepository,
                      PasswordEncoder passwordEncoder,
                      ApplicationEventPublisher eventPublisher) {
       this.userRepository = userRepository;
       this.userCredentialRepository = userCredentialRepository;
       this.passwordEncoder = passwordEncoder;
       this.eventPublisher = eventPublisher;
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

       eventPublisher.publishEvent(new UserRegisteredEvent(user.getUserId()));

       return user;
   }

   @Transactional(readOnly = true)
   public User getActiveUser(Long userId){
       User user = userRepository.findById(userId).orElseThrow(() -> new AccountNotActiveException("Account is not " +
               "available"));

       if(user.isDeleted() || !"ACTIVE".equals(user.getStatus())){
           throw new AccountNotActiveException("Account is not available");
       }
       return user;
   }

   @Transactional(readOnly = true)
   public Optional<User> findByPhone(String countryCode, String phone) {
       return userRepository.findByCountryCodeAndPhone(countryCode, phone);
   }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
