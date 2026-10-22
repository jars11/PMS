package com.pms.ui;

import java.util.function.ToIntFunction;

/**
 * Formato puro de la etiqueta de una reserva en el calendario: nombre del
 * titular, separador y conteo de personas (con bebes aparte). No conoce Swing:
 * el ancho de cada texto se inyecta como funcion para poder probarlo headless.
 *
 * <p>Reglas de conteo: una sola persona se representa solo con el simbolo; dos
 * o mas llevan el numero delante. Los bebes se agregan con {@code +} y la misma
 * regla (un solo bebe = solo simbolo).
 */
public final class EtiquetaReserva {

    /** Separador entre el nombre y el conteo. */
    public static final String SEPARADOR = " \u2014 ";

    /** Simbolo de persona (U+1F464). */
    public static final String ICONO_PERSONA = "\uD83D\uDC64";

    /** Simbolo de bebe (U+1F476). */
    public static final String ICONO_BEBE = "\uD83D\uDC76";

    /** Puntos suspensivos del recorte. */
    public static final String PUNTOS = "\u2026";

    private EtiquetaReserva() {
    }

    /**
     * Conteo legible: {@code ""} sin personas ni bebes, {@code "👤"} para una
     * persona, {@code "3👤"} para varias, y {@code " + 👶"} / {@code " + 2👶"}
     * cuando hay bebes.
     */
    public static String conteo(int personas, int bebes) {
        int totalPersonas = Math.max(0, personas);
        int totalBebes = Math.max(0, bebes);
        StringBuilder texto = new StringBuilder();
        if (totalPersonas > 0) {
            texto.append(totalPersonas == 1 ? ICONO_PERSONA : totalPersonas + ICONO_PERSONA);
        }
        if (totalBebes > 0) {
            if (!texto.isEmpty()) {
                texto.append(" + ");
            }
            texto.append(totalBebes == 1 ? ICONO_BEBE : totalBebes + ICONO_BEBE);
        }
        return texto.toString();
    }

    /** Etiqueta completa: {@code "Nombre — 2👤 + 👶"}; omite las partes vacias. */
    public static String completa(String titular, int personas, int bebes) {
        String nombre = titular == null ? "" : titular.trim();
        String conteo = conteo(personas, bebes);
        if (nombre.isEmpty()) {
            return conteo;
        }
        if (conteo.isEmpty()) {
            return nombre;
        }
        return nombre + SEPARADOR + conteo;
    }

    /**
     * Recorta {@code texto} al ancho indicado conservando SIEMPRE el principio y
     * agregando puntos suspensivos. Devuelve {@code ""} cuando ni los puntos
     * entran. No parte pares sustitutos (emoji).
     */
    public static String truncar(String texto, int maxAncho, ToIntFunction<String> anchoDe) {
        if (texto == null || texto.isEmpty() || maxAncho <= 0 || anchoDe == null) {
            return "";
        }
        if (anchoDe.applyAsInt(texto) <= maxAncho) {
            return texto;
        }
        if (anchoDe.applyAsInt(PUNTOS) > maxAncho) {
            return "";
        }
        String recorte = texto;
        while (!recorte.isEmpty()
                && anchoDe.applyAsInt(recorte.stripTrailing() + PUNTOS) > maxAncho) {
            recorte = recorte.substring(0, recorte.offsetByCodePoints(recorte.length(), -1));
        }
        String limpio = recorte.stripTrailing();
        return limpio.isEmpty() ? "" : limpio + PUNTOS;
    }
}
