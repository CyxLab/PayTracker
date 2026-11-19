package com.paytrack.service;

import com.paytrack.dto.LoginRequest;
import com.paytrack.dto.LoginResponse;
import com.paytrack.security.AppUserDetails;
import com.paytrack.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        String token = jwtService.issue(principal.getUserId(), principal.getTenantId(), principal.getRole());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs());
    }
}