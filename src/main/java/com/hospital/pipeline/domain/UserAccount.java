package com.hospital.pipeline.domain;

import com.hospital.pipeline.auth.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String username;

    public String passwordHash;

    @Enumerated(EnumType.STRING)
    public Role role;

    public String displayName;
    public String email;

    /** Set for doctor accounts: the doctor whose appointments this login may see. */
    public Long doctorId;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, Role role, String displayName,
                       String email, Long doctorId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.email = email;
        this.doctorId = doctorId;
    }
}
