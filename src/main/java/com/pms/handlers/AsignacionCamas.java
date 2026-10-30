package com.pms.handlers;

import com.pms.entities.Habitacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Asigna personas (parejas, personas individuales y bebes) a los recursos
 * fisicos de una habitacion (camas matrimoniales, camas individuales, colchones
 * y cunas). Es logica pura, sin Swing ni persistencia, para poder probarla y
 * reutilizarla desde el dialogo, el buscador y el controlador.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Cada pareja ocupa idealmente una cama matrimonial; las personas
 *       individuales, una cama individual.</li>
 *   <li>Si sobran parejas, primero usan pares de camas individuales; si queda
 *       una cama individual suelta pueden combinarla con un colchon; despues,
 *       colchones de a dos.</li>
 *   <li>Si sobran personas individuales, primero usan camas matrimoniales
 *       libres (dos personas por cama) y despues colchones (uno por persona).</li>
 *   <li>Las cunas son un recurso: puede haber menos cunas que bebes. Los bebes
 *       sin cuna no bloquean la reserva, solo generan un aviso.</li>
 *   <li>Los bebes no consumen capacidad de camas.</li>
 * </ul>
 *
 * <p>La asignacion resultante es determinista. El orden de prioridad es:
 * parejas a matrimoniales, individuales a individuales, parejas a individuales,
 * individuales a matrimoniales, parejas a colchones y, por ultimo, individuales
 * a colchones.
 */
public final class AsignacionCamas {

    private AsignacionCamas() {
    }

    /** Resultado fisico de la asignacion mas los avisos para el dialogo. */
    public record Asignacion(int camasMatrimoniales,
                             int camasIndividuales,
                             int camasIndividualesReales,
                             int colchones,
                             int cunas,
                             boolean capacidadSuficiente,
                             int parejasEnCamasIndividuales,
                             int parejasEnMixto,
                             int parejasEnColchones,
                             int individualesEnCamasMatrimoniales,
                             int individualesEnColchones,
                             int bebesSinCuna) {

        /** Aviso bajo el campo de parejas; vacio cuando no hay fallback. */
        public String avisoMatrimonios() {
            if (parejasEnCamasIndividuales == 0 && parejasEnMixto == 0
                    && parejasEnColchones == 0) {
                return "";
            }
            List<String> partes = new ArrayList<>();
            if (parejasEnCamasIndividuales > 0) {
                partes.add(parejasEnCamasIndividuales + " en camas individuales");
            }
            if (parejasEnMixto > 0) {
                partes.add(parejasEnMixto + " con cama individual + colchón");
            }
            if (parejasEnColchones > 0) {
                partes.add(parejasEnColchones + " en colchones");
            }
            return "Hay más parejas que camas matrimoniales: "
                    + String.join(", ", partes) + ".";
        }

        /** Aviso bajo el campo de personas individuales; vacio cuando no hay fallback. */
        public String avisoPersonasIndividuales() {
            if (individualesEnCamasMatrimoniales == 0 && individualesEnColchones == 0) {
                return "";
            }
            List<String> partes = new ArrayList<>();
            if (individualesEnCamasMatrimoniales > 0) {
                partes.add(individualesEnCamasMatrimoniales
                        + " en camas matrimoniales (2 por cama)");
            }
            if (individualesEnColchones > 0) {
                partes.add(individualesEnColchones + " en colchones");
            }
            return "Faltan camas individuales: se ubicarán "
                    + String.join(" y ", partes) + ".";
        }

        /** Aviso bajo el campo de bebes; vacio cuando hay cunas suficientes. */
        public String avisoBebes() {
            if (bebesSinCuna == 0) {
                return "";
            }
            return "Hay más bebés que cunas disponibles: los bebés sin cuna dormirán "
                    + "con los padres.";
        }

        /** Aviso general cuando no entra tanta gente en la habitacion. */
        public String avisoCapacidad() {
            return capacidadSuficiente ? ""
                    : "No hay capacidad suficiente en esta habitación para esa cantidad "
                    + "de personas.";
        }
    }

    /**
     * Calcula la asignacion usando todas las cunas disponibles hasta la
     * cantidad de bebes (dialogo manual).
     */
    public static Asignacion calcular(Habitacion habitacion, int parejas,
                                      int personasIndividuales, int bebes,
                                      boolean permitirColchones) {
        return calcular(habitacion, parejas, personasIndividuales, bebes,
                bebes, permitirColchones);
    }

    /**
     * Calcula la asignacion para una habitacion.
     *
     * @param cunasPedidas cunas solicitadas; se recortan contra los bebes y las
     *                     cunas reales. Puede ser menor que los bebes.
     * @param permitirColchones true cuando se pueden usar los colchones de la
     *                          habitacion (el dialogo manual los usa siempre).
     */
    public static Asignacion calcular(Habitacion habitacion, int parejas,
                                      int personasIndividuales, int bebes,
                                      int cunasPedidas, boolean permitirColchones) {
        if (habitacion == null) {
            throw new IllegalArgumentException("habitacion nula");
        }
        int parejasRestantes = Math.max(0, parejas);
        int individualesRestantes = Math.max(0, personasIndividuales);
        int bebesPedidos = Math.max(0, bebes);

        int matDisponibles = Math.max(0, habitacion.getCantidadMatrimoniales());
        int indDisponibles = Math.max(0, habitacion.getCantidadIndividuales());
        int colchonesDisponibles = permitirColchones
                ? Math.max(0, habitacion.getColchones()) : 0;
        int cunasDisponibles = Math.max(0, habitacion.getCantidadCunas());

        int matUsadas = 0;
        int indUsadas = 0;
        int colchonesUsados = 0;

        // 1. Parejas a camas matrimoniales.
        int parejasEnMatrimoniales = Math.min(parejasRestantes, matDisponibles);
        matUsadas += parejasEnMatrimoniales;
        parejasRestantes -= parejasEnMatrimoniales;

        // 2. Personas individuales a camas individuales.
        int individualesEnIndividuales = Math.min(individualesRestantes, indDisponibles);
        indUsadas += individualesEnIndividuales;
        individualesRestantes -= individualesEnIndividuales;

        // 3. Parejas restantes a pares de camas individuales.
        int indLibres = indDisponibles - indUsadas;
        int parejasEnCamasIndividuales = Math.min(parejasRestantes, indLibres / 2);
        indUsadas += 2 * parejasEnCamasIndividuales;
        indLibres -= 2 * parejasEnCamasIndividuales;
        parejasRestantes -= parejasEnCamasIndividuales;

        // 4. Personas individuales restantes a matrimoniales libres (2 por cama).
        int individualesEnCamasMatrimoniales = 0;
        int matLibres = matDisponibles - matUsadas;
        if (individualesRestantes > 0 && matLibres > 0) {
            int camasNecesarias = (individualesRestantes + 1) / 2;
            int camasUsar = Math.min(camasNecesarias, matLibres);
            int capacidadEnCamas = 2 * camasUsar;
            individualesEnCamasMatrimoniales =
                    Math.min(individualesRestantes, capacidadEnCamas);
            matUsadas += camasUsar;
            individualesRestantes -= individualesEnCamasMatrimoniales;
        }

        // 5. Parejas restantes: una cama individual suelta + un colchon, o
        //    directamente colchones de a dos.
        int colchonesLibres = colchonesDisponibles - colchonesUsados;
        int parejasEnMixto = 0;
        if (parejasRestantes > 0 && indLibres > 0 && colchonesLibres > 0) {
            indUsadas += 1;
            indLibres -= 1;
            colchonesUsados += 1;
            colchonesLibres -= 1;
            parejasRestantes -= 1;
            parejasEnMixto = 1;
        }
        int parejasEnColchones = Math.min(parejasRestantes, colchonesLibres / 2);
        colchonesUsados += 2 * parejasEnColchones;
        colchonesLibres -= 2 * parejasEnColchones;
        parejasRestantes -= parejasEnColchones;

        // 6. Personas individuales restantes a colchones (uno por persona).
        int individualesEnColchones = Math.min(individualesRestantes, colchonesLibres);
        colchonesUsados += individualesEnColchones;
        colchonesLibres -= individualesEnColchones;
        individualesRestantes -= individualesEnColchones;

        int cunasUsadas = Math.min(Math.min(Math.max(0, cunasPedidas), bebesPedidos),
                cunasDisponibles);
        int bebesSinCuna = bebesPedidos - cunasUsadas;
        boolean capacidadSuficiente =
                parejasRestantes == 0 && individualesRestantes == 0;

        return new Asignacion(matUsadas, indUsadas + colchonesUsados, indUsadas,
                colchonesUsados, cunasUsadas, capacidadSuficiente,
                parejasEnCamasIndividuales, parejasEnMixto, parejasEnColchones,
                individualesEnCamasMatrimoniales, individualesEnColchones,
                bebesSinCuna);
    }
}
