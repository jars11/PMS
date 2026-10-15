package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Habitacion;
import com.pms.entities.TitularReserva;
import com.pms.persistence.RepositorioPersonas;
import com.pms.utils.AtributoHabitacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Sembrado no destructivo de los datos demo para la GUI. Sólo toca
 * controladores y repositorios (sin dependencias Swing), por lo que es
 * verificable headless bajo H2.
 *
 * <p>Contrato: siembra las seis habitaciones demo y un titular demo SOLO
 * cuando la tabla de habitaciones está vacía. Nunca borra datos existentes
 * (nada de {@code limpiarDatos()}) y es idempotente.
 *
 * <p>{@link #sembrarReservasDemo()} agrega reservas PARCIALES demo (titular +
 * fechas + una habitación, sin pagos ni pasajeros) ancladas a hoy para dejar
 * ~80% de ocupación en la ventana {@code [hoy, hoy+14)}. También es no
 * destructivo e idempotente: si ya existe CUALQUIER reserva no hace nada.
 */
public final class SemillaDemo {

    private SemillaDemo() {
    }

    /** Siembra los datos demo únicamente cuando no hay ninguna habitación. */
    public static void sembrarSiVacio() {
        ControladorHabitaciones controlador = ControladorHabitaciones.getInstance();
        if (!controlador.getHabitaciones().isEmpty()) {
            return;
        }
        controlador.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        controlador.altaHabitacion(102, Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi),
                1, 1, 0, 0, 2200.0f);
        controlador.altaHabitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 0, 1800.0f);
        controlador.altaHabitacion(104, Set.of(AtributoHabitacion.VistaAlMar), 1, 2, 0, 0, 2100.0f);
        controlador.altaHabitacion(201, Set.of(AtributoHabitacion.Balcon), 2, 2, 0, 0, 2600.0f);
        controlador.altaHabitacion(202, Set.of(AtributoHabitacion.DosAmbientes), 2, 2, 0, 0, 2400.0f);

        new RepositorioPersonas().guardarTitular(new TitularReserva(
                List.of("Agustin"), List.of("Rivero"),
                "091000000", "22000000", "agustin@example.com"));
    }

    /** Noches de la ventana demo half-open {@code [hoy, hoy+14)}. */
    private static final int VENTANA_NOCHES = 14;

    /** Fracción objetivo de noches-habitación ocupadas en la ventana. */
    private static final double OBJETIVO_OCUPACION = 0.80;

    /**
     * Siembra reservas PARCIALES demo (titular + fechas + una habitación; sin
     * pagos ni pasajeros) para dejar ~80% de ocupación de noches-habitación en
     * la ventana half-open {@code [hoy, hoy+14)}.
     *
     * <p>Esquema determinista (sin aleatoriedad): ocupa de punta a punta
     * {@code floor(0.80 * n)} habitaciones y agrega UNA estadía parcial contigua
     * al inicio sobre la habitación siguiente. Como el resto del rango de esa
     * habitación (y las habitaciones posteriores) quedan libres, siempre hay al
     * menos un hueco contiguo de 2+ noches para demostrar la asignación
     * automática. El residuo parcial es siempre {@code <= 11} noches, así que el
     * hueco es de al menos 3 noches.
     *
     * <p>Anclado a hoy para que el rango caiga dentro de la ventana por defecto
     * de 14 noches del calendario. Es NO DESTRUCTIVO e idempotente: si ya existe
     * CUALQUIER reserva, no hace nada (no borra ni duplica datos). Con menos de
     * 2 habitaciones no siembra nada (no divide por cero).
     *
     * <p>Reutiliza {@link ControladorReservas#crearReservaPara} (validado y con
     * chequeo de solapamiento por habitación) y reutiliza o registra el titular
     * con {@link ControladorPersonas#altaTitular}.
     */
    public static void sembrarReservasDemo() {
        ControladorReservas controladorReservas = ControladorReservas.getInstance();
        if (!controladorReservas.getReservas().isEmpty()) {
            return;
        }

        // getHabitaciones() ya ordena por número: las primeras N son las más bajas.
        List<Habitacion> habitaciones = ControladorHabitaciones.getInstance().getHabitaciones();
        if (habitaciones.size() < 2) {
            return;
        }

        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(VENTANA_NOCHES);

        TitularReserva titular = titularDemo();

        int totalHabitaciones = habitaciones.size();
        int objetivo = (int) Math.round(OBJETIVO_OCUPACION * totalHabitaciones * VENTANA_NOCHES);
        // Nunca ocupamos TODAS las habitaciones: siempre queda >=1 libre.
        int habitacionesCompletas = Math.min((int) Math.floor(OBJETIVO_OCUPACION * totalHabitaciones),
                totalHabitaciones - 1);
        // Noches del residuo parcial sobre la habitación siguiente. Con 0.80 es
        // <= 11, pero se acota para garantizar el hueco de 2+ noches contiguas.
        int restoNoches = Math.min(objetivo - habitacionesCompletas * VENTANA_NOCHES,
                VENTANA_NOCHES - 2);

        // Habitaciones totalmente ocupadas de punta a punta de la ventana.
        for (int i = 0; i < habitacionesCompletas; i++) {
            controladorReservas.crearReservaPara(titular, habitaciones.get(i).getId(),
                    desde, hasta, 1, 0, 0);
        }

        // Una estadía parcial contigua al inicio sobre la habitación siguiente.
        if (restoNoches > 0) {
            controladorReservas.crearReservaPara(titular, habitaciones.get(habitacionesCompletas).getId(),
                    desde, desde.plusDays(restoNoches), 1, 0, 0);
        }
    }

    /**
     * Titular demo a reutilizar: el primero existente o uno nuevo si la tabla
     * está vacía. Evita duplicar titulares al re-sembrar.
     */
    private static TitularReserva titularDemo() {
        List<TitularReserva> titulares = ControladorPersonas.getInstance().getTitulares();
        if (!titulares.isEmpty()) {
            return titulares.get(0);
        }
        return ControladorPersonas.getInstance().altaTitular(
                List.of("Demo"), List.of("Reservas"), "099000000", "24000000", "demo.reservas@example.com");
    }
}
