package com.pms.controllers;

import com.pms.entities.Habitacion;
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.utils.AtributoHabitacion;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rooms of the hotel. Singleton and stateless: it always reads from the
 * repository, mirroring the C++ ControladorHabitaciones behavior.
 */
public class ControladorHabitaciones {

    private static final ControladorHabitaciones INSTANCIA = new ControladorHabitaciones();

    private final RepositorioHabitaciones repositorio = new RepositorioHabitaciones();

    private ControladorHabitaciones() {
    }

    public static ControladorHabitaciones getInstance() {
        return INSTANCIA;
    }

    /**
     * Registers a new room with its beds, attributes and nightly rate.
     * Valida los campos y pre-chequea el numero antes de persistir; la
     * constraint unica de la tabla queda solo como respaldo.
     */
    public void altaHabitacion(int numero, Set<AtributoHabitacion> atributos,
                               int cantidadMatrimoniales, int cantidadIndividuales,
                               int cantidadCunas, int colchones, float tarifaPorNoche) {
        Habitacion.validarCampos(numero, cantidadMatrimoniales, cantidadIndividuales,
                cantidadCunas, colchones, tarifaPorNoche);
        if (existeNumero(numero)) {
            throw new IllegalStateException("numero duplicado: " + numero);
        }
        Habitacion habitacion = new Habitacion(numero, atributos, cantidadMatrimoniales,
                cantidadIndividuales, cantidadCunas, colchones);
        habitacion.setTarifaPorNoche(tarifaPorNoche);
        habitacion.setPrioridad(siguientePrioridad());
        repositorio.guardar(habitacion);
    }

    /**
     * Prioridad para una habitacion nueva: una mas que la maxima actual, para
     * que el alta quede al final de la lista. Sin habitaciones, arranca en 1.
     */
    private int siguientePrioridad() {
        return repositorio.findAll().stream()
                .mapToInt(Habitacion::getPrioridad)
                .max()
                .orElse(0) + 1;
    }

    /** Room by id. Throws IllegalArgumentException when it does not exist. */
    public Habitacion getHabitacion(long id) {
        Habitacion habitacion = repositorio.findById(id);
        if (habitacion == null) {
            throw new IllegalArgumentException("habitacion inexistente: " + id);
        }
        return habitacion;
    }

    /**
     * Controlled update of an existing room. Rechaza un numero ya usado por
     * otra habitacion y luego aplica actualizar(...) (validado) + merge.
     */
    public Habitacion actualizarHabitacion(long id, int numero, Set<AtributoHabitacion> atributos,
                                           int cantidadMatrimoniales, int cantidadIndividuales,
                                           int cantidadCunas, int colchones, float tarifaPorNoche) {
        Habitacion habitacion = getHabitacion(id);
        if (!habitacion.getNumero().equals(numero) && existeNumero(numero)) {
            throw new IllegalStateException("numero duplicado: " + numero);
        }
        habitacion.actualizar(numero, atributos, cantidadMatrimoniales, cantidadIndividuales,
                cantidadCunas, colchones, tarifaPorNoche);
        return repositorio.actualizar(habitacion);
    }

    /**
     * Operational cleaning toggle: loads the room and flips its {@code limpia}
     * flag (dedicated setter, no structural re-validation). Returns the updated
     * managed room.
     */
    public Habitacion marcarLimpia(long id, boolean limpia) {
        Habitacion habitacion = getHabitacion(id);
        habitacion.setLimpia(limpia);
        return repositorio.actualizar(habitacion);
    }

    /** Deletes a room unless an RHPropuesta still references it. */
    public boolean eliminarHabitacion(long id) {
        if (repositorio.estaEnUso(id)) {
            throw new IllegalStateException("habitacion en uso: " + id);
        }
        return repositorio.eliminar(id);
    }

    /** True when a room with the given numero already exists. */
    public boolean existeNumero(int numero) {
        return repositorio.existeNumero(numero);
    }

    /** All rooms, ordered by room number (stable instead of C++ pointer order). */
    public List<Habitacion> getHabitaciones() {
        List<Habitacion> todas = repositorio.findAll();
        todas.sort(Comparator.comparing(Habitacion::getNumero));
        return todas;
    }

    /**
     * All rooms in assignment-priority order: ascending {@code prioridad} (1 is
     * the highest priority), then ascending room number as a stable fallback.
     */
    public List<Habitacion> getHabitacionesPorPrioridad() {
        List<Habitacion> todas = repositorio.findAll();
        todas.sort(Comparator.comparingInt(Habitacion::getPrioridad)
                .thenComparing(Habitacion::getNumero));
        return todas;
    }

    /**
     * Applies a new order to the rooms: the first id gets prioridad 1 (top of
     * the list), the second prioridad 2, and so on. Each room is persisted and
     * the result is returned in priority order. A missing id surfaces as
     * IllegalArgumentException from {@link #getHabitacion(long)}.
     */
    public List<Habitacion> reordenar(List<Long> idsOrdenados) {
        List<Long> ids = idsOrdenados == null ? List.of() : idsOrdenados;
        for (int i = 0; i < ids.size(); i++) {
            Habitacion habitacion = getHabitacion(ids.get(i));
            habitacion.setPrioridad(i + 1);
            repositorio.actualizar(habitacion);
        }
        return getHabitacionesPorPrioridad();
    }

    /** Rooms that have ALL the requested attributes (deduplicated by room number). */
    public Set<Habitacion> buscarPorAtributos(Set<AtributoHabitacion> atributos) {
        return new LinkedHashSet<>(repositorio.buscarPorAtributos(atributos));
    }

    /**
     * True when the room is free on every night of [desde, hasta)
     * (inclusive from, exclusive to). Only ACTIVE reservations block nights,
     * exactly like the C++ habitacionLibreEnRango (which also delegated to the
     * reservations controller).
     */
    public boolean habitacionLibreEnRango(Habitacion habitacion, LocalDate desde, LocalDate hasta) {
        return !ControladorReservas.getInstance().isHabitacionOcupada(habitacion.getId(), desde, hasta);
    }

    /** Rooms free on the given single night. */
    public Set<Habitacion> habitacionesLibresEnNoche(LocalDate noche) {
        LocalDate siguiente = noche.plusDays(1);
        Set<Habitacion> libres = new LinkedHashSet<>();
        for (Habitacion h : getHabitaciones()) {
            if (habitacionLibreEnRango(h, noche, siguiente)) {
                libres.add(h);
            }
        }
        return libres;
    }
}