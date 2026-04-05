package com.TenaMed.user.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

@Getter
public class AuthenticatedUserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID accountId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public AuthenticatedUserPrincipal(UUID userId,
                                      UUID accountId,
                                      String username,
                                      String password,
                                      Collection<? extends GrantedAuthority> authorities,
                                      boolean enabled) {
        this.userId = userId;
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
