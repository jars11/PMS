package com.pms.api;

import com.pms.entities.TitularReserva;
import com.pms.utils.TipoDocumento;

/**
 * Serializable read view of a reservation holder (titular), for the create/edit
 * form's titular selector and for the document-first autocomplete of the web
 * form: the document is the lookup key and the contact fields are what the form
 * prefills.
 */
public record TitularVistaDto(
        long id,
        String nombre,
        String numeroDocumento,
        TipoDocumento tipoDocumento,
        String celular,
        String telefonoFijo,
        String email) {

    public static TitularVistaDto de(TitularReserva t) {
        String nombre = (String.join(" ", t.getNombres()) + " " + String.join(" ", t.getApellidos())).trim();
        return new TitularVistaDto(t.getId(), nombre, t.getNumeroDocumento(), t.getTipoDocumento(),
                t.getCelular(), t.getTelefonoFijo(), t.getEmail());
    }
}
