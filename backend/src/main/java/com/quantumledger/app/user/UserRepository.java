package com.quantumledger.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailId(String emailId);
    boolean existsByCountryCodeAndPhone(String countryCode, String phone);
    Optional<User> findByEmailId(String emailId);
    Optional<User> findByCountryCodeAndPhone(String countryCode, String phone);
}
