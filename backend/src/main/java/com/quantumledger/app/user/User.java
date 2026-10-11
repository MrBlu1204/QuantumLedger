package com.quantumledger.app.user;



import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long userId;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email_id", nullable = false)
    private String emailId;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    protected User() { }

    public User(String title, String firstName, String lastName, String emailId, String countryCode, String phone, String passwordHash) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = (firstName + " " + (lastName == null ? "" : lastName)).trim();
        this.emailId = emailId;
        this.countryCode = countryCode;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void onCreate() {
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }
}
