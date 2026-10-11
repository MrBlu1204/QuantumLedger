package com.quantumledger.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

}
