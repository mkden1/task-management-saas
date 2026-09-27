package com.mkden.tasksaas.security;

import com.mkden.tasksaas.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated principal. The organization comes from the database on every
 * request, never from client-supplied input.
 */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final Long organizationId;
    private final String email;
    private final String passwordHash;
    private final User.Role role;
    private final boolean active;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.organizationId = user.getOrganization().getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.active = Boolean.TRUE.equals(user.getIsActive());
    }

    public Long getId() { return id; }

    public Long getOrganizationId() { return organizationId; }

    public User.Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }
}
