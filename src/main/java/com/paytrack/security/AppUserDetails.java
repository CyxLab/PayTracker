package com.paytrack.security;

import com.paytrack.model.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AppUserDetails implements UserDetails {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Long userId;
    private final UUID tenantId;
    private final String username;
    private final String password;
    private final String role;
    private final boolean enabled;

    public AppUserDetails(AppUser user) {
        this.userId = user.getId();
        this.tenantId = user.getTenantId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole().name();
        this.enabled = user.isEnabled();
    }

    public Long getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public String getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}