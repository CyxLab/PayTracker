package com.paytrack.controller;

import com.paytrack.dto.LoginRequest;
import com.paytrack.dto.LoginResponse;
import com.paytrack.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        // --- LOGS DE DIAGNOSTIC (à retirer une fois le problème résolu) ---
        System.out.println("=== LoginRequest reçu ===");
        System.out.println("username = [" + req.username() + "]");
        System.out.println("password = [" + req.password() + "]");
        System.out.println("password length = " + (req.password() != null ? req.password().length() : 0));
        // -------------------------------------------------------------------

        return ResponseEntity.ok(authService.login(req));
    }

    /**
     * Transforme les erreurs d'authentification (mauvais mot de passe, utilisateur inconnu)
     * en 401 Unauthorized clair, au lieu d'un 400 ou 500 opaque.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthenticationException e) {
        System.out.println("=== AuthenticationException ===");
        System.out.println("Type    : " + e.getClass().getSimpleName());
        System.out.println("Message : " + e.getMessage());

        Map<String, String> body = new HashMap<>();
        body.put("error", "Unauthorized");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Transforme les erreurs de validation (@NotBlank, @Email, etc.)
     * en 400 Bad Request avec la liste précise des champs en cause.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        System.out.println("=== ValidationException ===");
        e.getBindingResult().getFieldErrors().forEach(err ->
                System.out.println("Champ en erreur: " + err.getField()
                        + " | valeur rejetée: [" + err.getRejectedValue() + "]"
                        + " | message: " + err.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad Request");
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage()));
        body.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}