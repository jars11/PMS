package com.pms.ui;

import com.pms.utils.Rol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del value object de sesión: el rol ADMIN habilita administración y
 * el rol RECEPCIONISTA habilita recepción, nunca ambos a la vez.
 */
class SesionUsuarioTest {

    @Test
    void sesionAdminEsAdminYNoRecepcionista() {
        SesionUsuario sesion = SesionUsuario.de(Rol.ADMIN);

        assertEquals(Rol.ADMIN, sesion.getRol());
        assertTrue(sesion.esAdmin());
        assertFalse(sesion.esRecepcionista());
    }

    @Test
    void sesionRecepcionistaEsRecepcionistaYNoAdmin() {
        SesionUsuario sesion = SesionUsuario.de(Rol.RECEPCIONISTA);

        assertEquals(Rol.RECEPCIONISTA, sesion.getRol());
        assertTrue(sesion.esRecepcionista());
        assertFalse(sesion.esAdmin());
    }
}
