package com.pms.api;

import com.pms.utils.Rol;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Hard-coded in-memory credential provider for the web PoC. Two users exist:
 * {@code admin/a} (ADMIN) and {@code recepcionista/r} (RECEPCIONISTA). This is
 * intentionally simple and non-cryptographic: it mirrors the old role selector
 * with a real password field, ready to be replaced by a proper identity store.
 */
@Service
public class AuthService {

    private record Usuario(String usuario, String password, Rol rol) {
    }

    private static final Map<String, Usuario> USUARIOS = Map.of(
            "admin", new Usuario("admin", "a", Rol.ADMIN),
            "recepcionista", new Usuario("recepcionista", "r", Rol.RECEPCIONISTA));

    /**
     * Validates credentials and returns the role. Returns {@code null} when the
     * user does not exist or the password does not match (no data leaked about
     * which one failed).
     */
    public Rol autenticar(String usuario, String password) {
        if (usuario == null || password == null) {
            return null;
        }
        Usuario u = USUARIOS.get(usuario.trim());
        if (u == null || !u.password().equals(password)) {
            return null;
        }
        return u.rol();
    }
}
