package com.handy.appserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer authLevel = 100; // 기본값: 일반 유저

    public boolean isAdmin() {
        return authLevel >= 300;
    }

    public boolean isSeller() {
        return authLevel >= 200;
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (authLevel == null) {
            authLevel = 100;
        }
    }
} 