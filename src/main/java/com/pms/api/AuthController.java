package com.pms.api;

import com.pms.utils.Rol;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth REST endpoints for the web PoC.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Request shape: username + password (hard-coded users, see {@link AuthService}). */
    public record LoginRequest(String usuario, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        Rol rol = authService.autenticar(req.usuario(), req.password());
        if (rol == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(LoginResponse.de(rol));
    }
}
