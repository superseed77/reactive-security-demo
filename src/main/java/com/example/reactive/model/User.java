package com.example.reactive.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User implements UserDetails {
    @Id
    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    // Stored as comma-separated string via converters (see ConvertersConfig)
    private Set<String> roles;

    @Column("enabled")
    private boolean enabled;
    @Column("account_non_expired")
    private boolean accountNonExpired;
    @Column("account_non_locked")
    private boolean accountNonLocked;
    @Column("credentials_non_expired")
    private boolean credentialsNonExpired;

    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles == null ? java.util.List.of() :
                roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
    }
}
