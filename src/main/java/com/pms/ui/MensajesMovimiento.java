package com.pms.ui;

import com.pms.utils.MotivoRechazo;

/**
 * User-facing copy for a rejected reservation move. The headline is fixed by the
 * spec; {@link #detalle} maps every reason to a concrete, actionable line.
 */
public final class MensajesMovimiento {

    /** Exact rejection headline required by the spec. */
    public static final String RECHAZO_MOVIMIENTO = "No es posible mover a la habitación seleccionada";

    private MensajesMovimiento() {
    }

    /** Concrete detail line for the given reason; empty when there is no reason. */
    public static String detalle(MotivoRechazo motivo) {
        if (motivo == null) {
            return "";
        }
        return switch (motivo) {
            case ATRIBUTOS_FALTANTES ->
                    "La habitación destino no tiene los atributos requeridos por la reserva.";
            case DISTRIBUCION_CAMAS ->
                    "La habitación destino no tiene suficientes camas de los tipos necesarios.";
            case CAPACIDAD_INSUFICIENTE ->
                    "La habitación destino no tiene capacidad suficiente para los huéspedes.";
            case NO_DISPONIBLE ->
                    "La habitación destino no está libre para esas fechas.";
            case SOLAPE_LATE_CHECKOUT ->
                    "La habitación destino recibe un check-in el día de salida y esta reserva tiene late check-out.";
            case LIMPIEZA_LATE_CHECKOUT ->
                    "La habitación destino tuvo una salida con late check-out y todavía no está limpia.";
        };
    }
}
