package com.pms.api;

import com.pms.entities.TitularReserva;

import java.util.List;

/**
 * Serializable read view of a reservation holder (titular), for the create/edit
 * form's titular selector.
 */
public record TitularVistaDto(long id, String nombre, String email) {

    public static TitularVistaDto de(TitularReserva t) {
        String nombre = (String.join(" ", t.getNombres()) + " " + String.join(" ", t.getApellidos())).trim();
        return new TitularVistaDto(t.getId(), nombre, t.getEmail());
    }
}
