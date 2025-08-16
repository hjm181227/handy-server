package com.handy.appserver.entity.user;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private UserRole role;

    @Column(name = "auth_level", nullable = false)
    private Integer authLevel = 100;  // 기본 권한 레벨

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @OneToMany(mappedBy = "seller")
    private List<Product> products = new ArrayList<>();

    public User(String email, String password, String name, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public boolean isSeller() {
        return this.role == UserRole.SELLER;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public void update(String name) {
        this.name = name;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void updateAuthLevel(Integer authLevel) {
        this.authLevel = authLevel;
        // authLevel에 따라 role 자동 업데이트
        if (authLevel >= 300) {
            this.role = UserRole.ADMIN;
        } else if (authLevel >= 200) {
            this.role = UserRole.SELLER;
        } else {
            this.role = UserRole.USER;
        }
    }
}