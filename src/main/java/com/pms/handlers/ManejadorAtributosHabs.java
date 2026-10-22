package com.pms.handlers;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Room-attribute logic: capacity by attributes and best-room selection.
 * Stateless, same algorithms as the C++ ManejadorAtributosHabs.
 */
public class ManejadorAtributosHabs {

    /** Maximum passengers the room can host: derived from its real beds. */
    public static int capacidadDe(Habitacion h) {
        return h.capacidad();
    }

    /** True when the room has ALL the required attributes. */
    public static boolean tieneAtributos(Habitacion h, Set<AtributoHabitacion> requeridos) {
        return tieneAtributos(h.getAtributos(), requeridos);
    }

    /**
     * True when every required attribute is present in {@code presentes}. Null
     * {@code presentes} counts as empty; null {@code requeridos} counts as none.
     */
    public static boolean tieneAtributos(Set<AtributoHabitacion> presentes,
                                         Set<AtributoHabitacion> requeridos) {
        if (requeridos == null || requeridos.isEmpty()) {
            return true;
        }
        Set<AtributoHabitacion> pres = presentes == null ? Set.of() : presentes;
        return pres.containsAll(requeridos);
    }

    /** Number of required attributes present in the room. */
    public int atributosCoincidentes(Habitacion h, Set<AtributoHabitacion> requeridos) {
        int coincidencias = 0;
        for (AtributoHabitacion a : requeridos) {
            if (h.getAtributos().contains(a)) {
                coincidencias++;
            }
        }
        return coincidencias;
    }

    /**
     * Best room free for the WHOLE block that fits all passengers, or null.
     * Exact attribute match is preferred; tie-break by fewer extra attributes,
     * then lowest room number. Otherwise the room with the most matching
     * attributes wins; tie-break by fewer total attributes, then room number.
     */
    public Habitacion brindarHabitacionesDisponibles(DTRHSolicitada dtrhs) {
        // Los bebes no consumen camas: las cunas son un recurso aparte y pueden
        // ser menos que los bebes (duermen con los padres).
        int passengers = 2 * dtrhs.getCantMatrimonios() + dtrhs.getCantIndividuales();
        Set<AtributoHabitacion> requeridos = dtrhs.getAtributosHabitacion();

        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();

        // Candidates: enough capacity and free every night of the block.
        Set<Habitacion> candidatas = new LinkedHashSet<>();
        for (Habitacion h : ch.getHabitaciones()) {
            if (capacidadDe(h) >= passengers
                    && ch.habitacionLibreEnRango(h, dtrhs.getFechaInicio(), dtrhs.getFechaFin())) {
                candidatas.add(h);
            }
        }

        if (candidatas.isEmpty()) {
            return null;
        }

        Habitacion mejor = null;

        // Exact preference: rooms with ALL required attributes.
        Set<Habitacion> exactas = new LinkedHashSet<>();
        for (Habitacion h : candidatas) {
            if (tieneAtributos(h, requeridos)) {
                exactas.add(h);
            }
        }

        if (!exactas.isEmpty()) {
            for (Habitacion h : exactas) {
                if (mejor == null) {
                    mejor = h;
                    continue;
                }
                int extraH = h.getAtributos().size() - requeridos.size();
                int extraM = mejor.getAtributos().size() - requeridos.size();
                if (extraH < extraM || (extraH == extraM && h.getNumero() < mejor.getNumero())) {
                    mejor = h;
                }
            }
        } else {
            for (Habitacion h : candidatas) {
                if (mejor == null) {
                    mejor = h;
                    continue;
                }
                int coindH = atributosCoincidentes(h, requeridos);
                int coindM = atributosCoincidentes(mejor, requeridos);
                if (coindH > coindM
                        || (coindH == coindM
                        && (h.getAtributos().size() < mejor.getAtributos().size()
                        || (h.getAtributos().size() == mejor.getAtributos().size()
                        && h.getNumero() < mejor.getNumero())))) {
                    mejor = h;
                }
            }
        }

        return mejor;
    }
}