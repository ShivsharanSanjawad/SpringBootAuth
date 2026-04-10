package com.shivsharan.backend.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import io.micrometer.common.lang.NonNull;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @NonNull
    @Size(min=8, message="Password minimum size should be 8")
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 2048)
    private String profileImagePath;

    @Column
    private Integer rating = 0 ;

    @Column
    private Integer streak = 0 ;

    @Column
    private LocalDateTime createdAt ;

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now() ;
    }

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private College college;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private AcessLevel role;

    private boolean isVerified ;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.role = AcessLevel.USER;
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = AcessLevel.USER;
    }
}
