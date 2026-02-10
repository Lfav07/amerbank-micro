package com.amerbank.auth_server.model;

import com.amerbank.auth_server.dto.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(
            name = "users_seq",
            sequenceName = "users_id_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Builder
    public User(Long id, String email, String password, boolean active, Set<Role> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.active = active;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }
}
