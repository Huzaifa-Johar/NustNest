package com.nustnest.backend.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table (name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class User  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false,updatable = false)
    private String fullName;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String profileImagePath;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime tokenExpiresat;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static User create(String fullName, String email, String password){

        User user = new User();
        user.fullName = fullName;
        user.email = email;
        user.password = password;
        user.emailVerified = false;

        return user;
    }

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }


}
