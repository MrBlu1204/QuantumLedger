package com.quantumledger.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailId(String emailId);
    boolean existsByCountryCodeAndPhone(String countryCode, String phone);
}
