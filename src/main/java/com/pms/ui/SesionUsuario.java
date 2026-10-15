package com.pms.ui;

import com.pms.utils.Rol;

import java.util.Objects;

/**
 * Sesión de usuario de la GUI: value object inmutable que transporta el rol
 * elegido en el selector de arranque. Las pantallas restringidas leen el rol
 * SOLO desde aquí, de modo que un futuro proveedor de credenciales pueda
 * reemplazar al selector sin tocar esas pantallas.
 */
public final class SesionUsuario {

    private final Rol rol;

    private SesionUsuario(Rol rol) {
        this.rol = Objects.requireNonNull(rol, "rol");
    }

    /** Construye la sesión para el rol indicado. */
    public static SesionUsuario de(Rol rol) {
        return new SesionUsuario(rol);
    }

    public Rol getRol() {
        return rol;
    }

    /** True cuando el rol habilitado es ADMIN. */
    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    /** True cuando el rol habilitado es RECEPCIONISTA. */
    public boolean esRecepcionista() {
        return rol == Rol.RECEPCIONISTA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SesionUsuario)) {
            return false;
        }
        return rol == ((SesionUsuario) o).rol;
    }

    @Override
    public int hashCode() {
        return rol.hashCode();
    }

    @Override
    public String toString() {
        return "SesionUsuario{" + rol + '}';
    }
}
