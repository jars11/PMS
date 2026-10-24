package com.pms.api;

import com.pms.utils.Rol;

/**
 * Login response for the PoC: the resolved role plus a fake token placeholder.
 * No real session/JWT is issued yet; the token is ignored by the PoC frontend
 * which keeps the role in local storage.
 */
public record LoginResponse(String rol, boolean admin, String token) {

    public static LoginResponse de(Rol rol) {
        return new LoginResponse(rol.name(), rol == Rol.ADMIN, "poc-token-" + rol.name().toLowerCase());
    }
}
