package com.pms.controllers;

import com.pms.dtos.DTCapacidadFaltante;
import com.pms.dtos.DTDistribucionCamas;
import com.pms.dtos.DTDistribucionHabs;
import com.pms.dtos.DTPago;
import com.pms.dtos.DTRHPropuesta;
import com.pms.dtos.DTRHSolicitada;
import com.pms.dtos.DTReserva;
import com.pms.dtos.PerfilRelajacion;
import com.pms.entities.ContratoReserva;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.entities.RHSolicitada;
import com.pms.entities.Reserva;
import com.pms.entities.Solicitud;
import com.pms.entities.TitularReserva;
import com.pms.handlers.ManejadorAtributosHabs;
import com.pms.handlers.ManejadorCapacidad;
import com.pms.handlers.ManejadorDisponibilidad;
import com.pms.handlers.ManejadorDistribucionCamas;
import com.pms.handlers.ManejadorDistribucionHabs;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioRHPropuestas;
import com.pms.persistence.RepositorioReservas;
import com.pms.ui.CompatibilidadMovimiento;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.EstadoReserva;
import com.pms.utils.MotivoRechazo;
import com.pms.utils.TipoMoneda;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reservations. Singleton; the pending blocks are transient (the C++
 * RHSolicitada entities become the DTRHSolicitada DTOs), every query reads
 * fresh from the repository.
 */
public class ControladorReservas {

    private static final ControladorReservas INSTANCIA = new ControladorReservas();

    private final RepositorioReservas repositorio = new RepositorioReservas();
    private final RepositorioRHPropuestas repositorioPropuestas = new RepositorioRHPropuestas();

    private final ManejadorCapacidad manejadorCapacidad = new ManejadorCapacidad();
    private final ManejadorDistribucionHabs manejadorDistribucionHabs = new ManejadorDistribucionHabs();
    private final ManejadorDisponibilidad manejadorDisponibilidad = new ManejadorDisponibilidad();
    private final ManejadorDistribucionCamas manejadorDistribucionCamas = new ManejadorDistribucionCamas();

    /** Pending room-stay blocks, cleared after crearReserva (C++ rhsolicitadas). */
    private final Set<DTRHSolicitada> rhsolicitadas = new TreeSet<>();

    private ControladorReservas() {
    }

    public static ControladorReservas getInstance() {
        return INSTANCIA;
    }

    /**
     * Vista de lectura de una reserva para la lista de recepcion: el titular
     * como texto, los numeros de habitacion, el rango de fechas y el estado.
     * Todos los campos vienen de entidades ya fetcheadas (sin lazy access sobre
     * la instancia detached).
     */
    public record ReservaVista(Long id, String titular, Set<Integer> numerosHabitacion,
                               LocalDate checkIn, LocalDate checkOut,
                               EstadoReserva estado, boolean activa) {
    }

    /**
     * Ocupacion nocturna de una habitacion en una ventana de fechas: la
     * habitacion (detached, con numero e id), el conjunto de noches ocupadas
     * por reservas ACTIVAS, el id de la reserva que ocupa cada noche y los
     * bloques completos para la barra contigua.
     */
    public record OcupacionHabitacion(Habitacion habitacion, Set<LocalDate> nochesOcupadas,
                                      Map<LocalDate, Long> reservaPorNoche, List<BloqueNoche> bloques) {

        /**
         * Un bloque de estadia de una reserva ACTIVA tal como lo pinta el
         * calendario: fechas de la propuesta (rango half-open {@code [inicio,fin)})
         * mas el flag {@code lateCheckout} de la reserva due\u00f1a.
         */
        public record BloqueNoche(long reservaId, LocalDate fechaInicio, LocalDate fechaFin,
                                  boolean lateCheckout) {
        }

        /** Compact ctor: normaliza colecciones nulas a vacias. */
        public OcupacionHabitacion {
            reservaPorNoche = reservaPorNoche == null ? Map.of() : reservaPorNoche;
            bloques = bloques == null ? List.of() : bloques;
        }

        /** Back-compat: constructor de 2 argumentos sin ids de reserva ni bloques. */
        public OcupacionHabitacion(Habitacion habitacion, Set<LocalDate> nochesOcupadas) {
            this(habitacion, nochesOcupadas, Map.of(), List.of());
        }

        /** Back-compat: constructor de 3 argumentos sin bloques. */
        public OcupacionHabitacion(Habitacion habitacion, Set<LocalDate> nochesOcupadas,
                                   Map<LocalDate, Long> reservaPorNoche) {
            this(habitacion, nochesOcupadas, reservaPorNoche, List.of());
        }

        /** Id de la reserva que ocupa la noche indicada; null si la noche esta libre. */
        public Long reservaEn(LocalDate noche) {
            return reservaPorNoche.get(noche);
        }
    }

    /** Resultado de un intento de movimiento: {@code movida=false} trae el motivo. */
    public record ResultadoMovimiento(boolean movida, MotivoRechazo motivo) {
    }

    /**
     * Compatibilidad de UNA habitacion destino para el bloque movido: {@code
     * motivo} es {@code null} cuando la habitacion es compatible (misma
     * semantica que {@link CompatibilidadMovimiento#evaluar}).
     */
    public record ResultadoCompatibilidad(long habitacionId, MotivoRechazo motivo) {

        public boolean compatible() {
            return motivo == null;
        }
    }

    /**
     * Vista de edicion de UNA propuesta de una reserva: los datos actuales que
     * el dialogo de edicion usa para prellenar sus campos. La propuesta
     * objetivo se identifica por (habitacion clickeada, noche clickeada), con
     * cobertura half-open {@code [fechaInicio, fechaFin)}.
     */
    public record EdicionReserva(long reservaId, long habitacionId, LocalDate nocheObjetivo,
                                 String titular, LocalDate checkIn, LocalDate checkOut,
                                 int camasMatrimoniales, int camasIndividuales, int cunas,
                                 EstadoReserva estado, boolean lateCheckout,
                                 Set<AtributoHabitacion> atributos) {
    }

    // ---------------------------------------------------------------- mappers

    private static DTRHPropuesta aDTRHPropuesta(RHPropuesta rhp) {
        return new DTRHPropuesta(rhp.getFechaInicio(), rhp.getFechaFin(), rhp.getAtributosHabitacion(),
                rhp.getSubtotal(), rhp.getMoneda(),
                rhp.getCamasMatrimoniales(), rhp.getCamasIndividuales(), rhp.getCunas());
    }

    private static DTReserva aDTReserva(Reserva r) {
        Set<DTRHPropuesta> props = new TreeSet<>();
        for (RHPropuesta rhp : r.getRHPropuestas()) {
            props.add(aDTRHPropuesta(rhp));
        }
        return new DTReserva(r.isActiva(), r.getMontoTotal(), r.getMontoSenia(),
                r.getMoneda(), r.getEstado(), props);
    }

    /**
     * Builds the receptionist list view from a detached, fully-fetched
     * {@link Reserva}: titular = nombres + apellidos; numerosHabitacion = room
     * numbers de las propuestas; checkIn/checkOut = min fechaInicio / max
     * fechaFin. Sin acceso lazy.
     */
    private static ReservaVista aReservaVista(Reserva r) {
        TitularReserva t = r.getTitular();
        String titular = t == null ? "" : nombreTitular(t);

        Set<Integer> numeros = new TreeSet<>();
        LocalDate checkIn = null;
        LocalDate checkOut = null;
        for (RHPropuesta p : r.getRHPropuestas()) {
            if (p.getHabitacion() != null) {
                numeros.add(p.getHabitacion().getNumero());
            }
            if (p.getFechaInicio() != null && (checkIn == null || p.getFechaInicio().isBefore(checkIn))) {
                checkIn = p.getFechaInicio();
            }
            if (p.getFechaFin() != null && (checkOut == null || p.getFechaFin().isAfter(checkOut))) {
                checkOut = p.getFechaFin();
            }
        }
        return new ReservaVista(r.getId(), titular, numeros, checkIn, checkOut, r.getEstado(), r.isActiva());
    }

    // ------------------------------------------------------------- availability

    /**
     * Best proposal for every feasible block; empty proposal list for infeasible
     * blocks. Mirrors the C++ obtenerDisponibilidad chain exactly.
     */
    public DTReserva obtenerDisponibilidad(Set<DTRHSolicitada> bloques) {
        Set<DTRHPropuesta> props = new TreeSet<>();
        float total = 0.0f;

        for (DTRHSolicitada s : bloques) {
            // 1) Capacity per night.
            Set<DTCapacidadFaltante> sinCapacidad = manejadorCapacidad.hayCapacidad(s);
            if (!sinCapacidad.isEmpty()) {
                continue;
            }

            // 2) Room distribution per night.
            Set<DTDistribucionHabs> distHabs = manejadorDistribucionHabs.hayDistribucionHabs(s);
            if (distHabs.isEmpty()) {
                continue;
            }

            // 3) Bed distribution per night.
            Set<DTDistribucionCamas> distCamas = manejadorDistribucionCamas.distribucionCamas(s, distHabs);

            // 4) Proposal.
            Set<DTRHPropuesta> prop = manejadorDisponibilidad.dtrhp(s, distHabs, distCamas);
            for (DTRHPropuesta p : prop) {
                props.add(p);
                total += p.getSubtotal();
            }
        }

        TipoMoneda moneda = props.isEmpty() ? TipoMoneda.USD : props.iterator().next().getMoneda();
        return new DTReserva(false, total, 0.0f, moneda, EstadoReserva.PendienteDePago, props);
    }

    /** Clears pending blocks without creating a reservation (test seam, mirrors
     *  the cleanup the C++ destructor did on rhsolicitadas). */
    public void limpiarBloquesPendientes() {
        rhsolicitadas.clear();
    }

    /** Queues a pending block for the next crearReserva. */
    public void agregarBloqueReserva(LocalDate checkIn, LocalDate checkOut, int cantMatrimonios,
                                     int cantIndividuales, int cantBebes, int cantCunas,
                                     Set<AtributoHabitacion> atributosHabitacion) {
        rhsolicitadas.add(new DTRHSolicitada(checkIn, checkOut, atributosHabitacion,
                cantMatrimonios, cantIndividuales, cantBebes, cantCunas));
    }

    /**
     * Creates an active reservation from the pending blocks and returns the
     * MANAGED titular (so the caller gets the DB id; divergence from the C++
     * void signature, needed because ids come from the database).
     */
    public TitularReserva crearReserva(TitularReserva titular) {
        return JpaUtil.enTransaccion(em -> {
            TitularReserva titularGestionado = em.merge(titular);

            Reserva r = new Reserva(titularGestionado);
            Solicitud solicitud = new Solicitud(titularGestionado);
            float total = 0.0f;

            for (DTRHSolicitada s : rhsolicitadas) {
                if (!manejadorCapacidad.hayCapacidad(s).isEmpty()) {
                    continue;
                }

                Set<DTDistribucionHabs> distHabs = manejadorDistribucionHabs.hayDistribucionHabs(s);
                if (distHabs.isEmpty()) {
                    continue;
                }

                Set<DTDistribucionCamas> distCamas = manejadorDistribucionCamas.distribucionCamas(s, distHabs);

                Habitacion habitacion = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s);
                if (habitacion == null) {
                    continue;
                }

                Set<DTRHPropuesta> props = manejadorDisponibilidad.dtrhp(s, distHabs, distCamas);
                if (props.isEmpty()) {
                    continue;
                }

                DTRHPropuesta prop = props.iterator().next();
                RHPropuesta rp = new RHPropuesta(s.getFechaInicio(), s.getFechaFin(),
                        habitacion.getAtributos(),
                        prop.getSubtotal(), prop.getMoneda(),
                        prop.getCamasMatrimoniales(), prop.getCamasIndividuales(), prop.getCunas());
                rp.setHabitacion(em.merge(habitacion));
                r.addRHPropuesta(rp);
                // Espeja SOLO los bloques consumidos: los saltados por los filtros no se espejan.
                solicitud.addRHSolicitada(RHSolicitada.de(s));
                total += rp.getSubtotal();
            }

            r.setActiva(true);
            r.setMontoTotal(total);
            r.setMontoSenia(0.0f);
            r.setMoneda(TipoMoneda.USD);
            r.setEstado(EstadoReserva.PendienteDePago);
            r.setEfectiva(true);

            persistirReservaYSolicitud(em, r, solicitud, PerfilRelajacion.POR_DEFECTO);

            rhsolicitadas.clear();
            return titularGestionado;
        });
    }

    /**
     * Persiste la {@link Reserva} y su {@link Solicitud} espejo dentro de la EM
     * vigente: primero la reserva (el FK contrato_reserva.reserva_id debe ser
     * satisfacible), luego el contrato con el perfil de relajacion y la
     * solicitud (cascadea sus bloques y el contrato). Nunca abre otra EM.
     */
    private static void persistirReservaYSolicitud(EntityManager em, Reserva r,
                                                   Solicitud solicitud, PerfilRelajacion perfil) {
        em.persist(r);
        solicitud.addContrato(new ContratoReserva(solicitud, r, perfil));
        em.persist(solicitud);
    }

    /**
     * Creates an ACTIVE reservation for an EXPLICIT room and date range in a
     * SINGLE transaction. Los argumentos se validan ANTES de abrir la
     * transaccion; dentro de la misma EM se busca la habitacion, se cuenta el
     * solapamiento con reservas activas y se persiste la reserva con una unica
     * {@link RHPropuesta}. Nunca llama a otro controlador/repositorio (eso
     * abriria una segunda EM).
     *
     * @return la reserva gestionada; queda detached al cerrar la transaccion.
     */
    public Reserva crearReservaPara(TitularReserva titular, long habitacionId,
                                    LocalDate checkIn, LocalDate checkOut,
                                    int camasMatrimoniales, int camasIndividuales, int cunas) {
        return crearReservaPara(titular, habitacionId, checkIn, checkOut,
                camasMatrimoniales, camasIndividuales, cunas, null);
    }

    /**
     * Variante con atributos solicitados: cuando {@code atributos} es no nulo se
     * persisten tal cual en la {@link RHPropuesta}; cuando es nulo se conserva el
     * comportamiento historico (los atributos de la habitacion).
     */
    public Reserva crearReservaPara(TitularReserva titular, long habitacionId,
                                    LocalDate checkIn, LocalDate checkOut,
                                    int camasMatrimoniales, int camasIndividuales, int cunas,
                                    Set<AtributoHabitacion> atributos) {
        return crearReservaPara(titular, habitacionId, checkIn, checkOut,
                camasMatrimoniales, camasIndividuales, cunas, atributos, PerfilRelajacion.POR_DEFECTO);
    }

    /**
     * Variante con perfil de relajacion explicito: el contrato registra el
     * perfil recibido. Las sobrecargas historicas delegan con {@code POR_DEFECTO}.
     */
    public Reserva crearReservaPara(TitularReserva titular, long habitacionId,
                                    LocalDate checkIn, LocalDate checkOut,
                                    int camasMatrimoniales, int camasIndividuales, int cunas,
                                    Set<AtributoHabitacion> atributos, PerfilRelajacion perfil) {
        if (titular == null || checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("fechas/titular invalidos");
        }
        if (camasMatrimoniales < 0 || camasIndividuales < 0 || cunas < 0) {
            throw new IllegalArgumentException("camas negativas");
        }

        return JpaUtil.enTransaccion(em -> {
            Habitacion hab = em.find(Habitacion.class, habitacionId);
            if (hab == null) {
                throw new IllegalArgumentException("habitacion no existe: " + habitacionId);
            }

            verificarGuardaLimpieza(em, habitacionId, checkIn, hab.isLimpia());

            Long solapadas = em.createQuery(
                            "select count(r) from Reserva r join r.rhPropuestas p " +
                                    "where r.activa = true and p.habitacion.id = :hid " +
                                    "and p.fechaInicio < :hasta and :desde < p.fechaFin", Long.class)
                    .setParameter("hid", habitacionId)
                    .setParameter("desde", checkIn)
                    .setParameter("hasta", checkOut)
                    .getSingleResult();
            if (solapadas != null && solapadas > 0) {
                throw new IllegalStateException("habitacion ocupada: " + habitacionId);
            }

            TitularReserva titularGestionado = em.merge(titular);
            Reserva r = new Reserva(titularGestionado);
            long noches = ChronoUnit.DAYS.between(checkIn, checkOut);
            float subtotal = noches * hab.getTarifaPorNoche();

            Set<AtributoHabitacion> atributosPropuesta =
                    atributos != null ? new TreeSet<>(atributos) : hab.getAtributos();
            RHPropuesta rp = new RHPropuesta(checkIn, checkOut, atributosPropuesta, subtotal,
                    TipoMoneda.USD, camasMatrimoniales, camasIndividuales, cunas);
            rp.setHabitacion(hab);
            r.addRHPropuesta(rp);

            r.setActiva(true);
            r.setMontoTotal(subtotal);
            r.setMontoSenia(0.0f);
            r.setMoneda(TipoMoneda.USD);
            r.setEstado(EstadoReserva.PendienteDePago);
            r.setEfectiva(true);

            // La solicitud espeja la peticion explicita como un unico bloque (sin bebes).
            Solicitud solicitud = new Solicitud(titularGestionado);
            solicitud.addRHSolicitada(new RHSolicitada(checkIn, checkOut, atributos,
                    camasMatrimoniales, camasIndividuales, 0, cunas));

            persistirReservaYSolicitud(em, r, solicitud, perfil);
            return r;
        });
    }

    /**
     * Crea una reserva ACTIVA eligiendo la habitacion AUTOMATICAMENTE dentro de
     * UNA unica transaccion (D5). Los argumentos baratos se validan ANTES de
     * abrir la transaccion; dentro de la misma se resuelve la habitacion con
     * {@link ManejadorAtributosHabs#brindarHabitacionesDisponibles} (el mismo
     * idioma que {@link #crearReserva}: capacidad y disponibilidad las verifica
     * el manejador). Si el manejador no encuentra habitacion se lanza
     * {@link IllegalStateException} -> rollback (nada se persiste). Antes de
     * persistir se re-cuenta el solapamiento sobre la habitacion elegida, igual
     * que {@link #crearReservaPara}.
     *
     * @return la reserva gestionada; queda detached al cerrar la transaccion.
     */
    public Reserva crearReservaAutomatica(TitularReserva titular, LocalDate checkIn, LocalDate checkOut,
                                          int camasMatrimoniales, int camasIndividuales, int cantBebes, int cunas,
                                          Set<AtributoHabitacion> atributos) {
        return crearReservaAutomatica(titular, checkIn, checkOut, camasMatrimoniales,
                camasIndividuales, cantBebes, cunas, atributos, PerfilRelajacion.POR_DEFECTO);
    }

    /**
     * Variante con perfil de relajacion explicito. Las sobrecargas historicas
     * delegan con {@code POR_DEFECTO}.
     */
    public Reserva crearReservaAutomatica(TitularReserva titular, LocalDate checkIn, LocalDate checkOut,
                                          int camasMatrimoniales, int camasIndividuales, int cantBebes, int cunas,
                                          Set<AtributoHabitacion> atributos, PerfilRelajacion perfil) {
        if (titular == null || checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("fechas/titular invalidos");
        }
        if (camasMatrimoniales < 0 || camasIndividuales < 0 || cantBebes < 0 || cunas < 0) {
            throw new IllegalArgumentException("camas/pasajeros negativos");
        }

        return JpaUtil.enTransaccion(em -> {
            DTRHSolicitada solicitud = new DTRHSolicitada(checkIn, checkOut, atributos,
                    camasMatrimoniales, camasIndividuales, cantBebes, cunas);

            Habitacion hab = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(solicitud);
            if (hab == null) {
                throw new IllegalStateException("sin habitacion disponible");
            }
            Habitacion habGestionada = em.merge(hab);

            verificarGuardaLimpieza(em, habGestionada.getId(), checkIn, habGestionada.isLimpia());

            Long solapadas = em.createQuery(
                            "select count(r) from Reserva r join r.rhPropuestas p " +
                                    "where r.activa = true and p.habitacion.id = :hid " +
                                    "and p.fechaInicio < :hasta and :desde < p.fechaFin", Long.class)
                    .setParameter("hid", habGestionada.getId())
                    .setParameter("desde", checkIn)
                    .setParameter("hasta", checkOut)
                    .getSingleResult();
            if (solapadas != null && solapadas > 0) {
                throw new IllegalStateException("habitacion ocupada: " + habGestionada.getId());
            }

            TitularReserva titularGestionado = em.merge(titular);
            Reserva r = new Reserva(titularGestionado);
            long noches = ChronoUnit.DAYS.between(checkIn, checkOut);
            float subtotal = noches * habGestionada.getTarifaPorNoche();

            RHPropuesta rp = new RHPropuesta(checkIn, checkOut, habGestionada.getAtributos(), subtotal,
                    TipoMoneda.USD, camasMatrimoniales, camasIndividuales, cunas);
            rp.setHabitacion(habGestionada);
            r.addRHPropuesta(rp);

            r.setActiva(true);
            r.setMontoTotal(subtotal);
            r.setMontoSenia(0.0f);
            r.setMoneda(TipoMoneda.USD);
            r.setEstado(EstadoReserva.PendienteDePago);
            r.setEfectiva(true);

            // La solicitud espeja la peticion (incluye cantBebes), un unico bloque.
            Solicitud solicitudPersistida = new Solicitud(titularGestionado);
            solicitudPersistida.addRHSolicitada(RHSolicitada.de(solicitud));

            persistirReservaYSolicitud(em, r, solicitudPersistida, perfil);
            return r;
        });
    }

    /**
     * Datos actuales de la propuesta de {@code reservaId} que cubre la noche
     * indicada de la habitacion indicada (cobertura half-open). Es una lectura
     * pura: no escribe ni abre transacciones anidadas.
     *
     * @throws IllegalArgumentException si la reserva no existe o ninguna
     *         propuesta cubre {@code (habitacionId, noche)}.
     */
    public EdicionReserva edicionDe(long reservaId, long habitacionId, LocalDate noche) {
        if (noche == null) {
            throw new IllegalArgumentException("noche invalida");
        }

        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.createQuery(
                            "select distinct r from Reserva r left join fetch r.rhPropuestas p " +
                                    "left join fetch p.habitacion where r.id = :id", Reserva.class)
                    .setParameter("id", reservaId)
                    .getResultList().stream().findFirst().orElse(null);
            if (r == null) {
                throw new IllegalArgumentException("reserva no existe: " + reservaId);
            }

            RHPropuesta objetivo = propuestaQueCubre(r, habitacionId, noche);
            TitularReserva t = r.getTitular();
            return new EdicionReserva(r.getId(), habitacionId, noche,
                    t == null ? "" : nombreTitular(t),
                    objetivo.getFechaInicio(), objetivo.getFechaFin(),
                    objetivo.getCamasMatrimoniales(), objetivo.getCamasIndividuales(),
                    objetivo.getCunas(), r.getEstado(), r.isLateCheckout(),
                    new TreeSet<>(objetivo.getAtributosHabitacion()));
        });
    }

    /**
     * Edita UNA propuesta de una reserva en UNA unica transaccion: se localiza
     * la propuesta que cubre {@code (habitacionClickId, nocheClick)}, se
     * REEMPLAZA por una nueva ({@code orphanRemoval} borra la fila vieja), se
     * cuentan los solapamientos de la habitacion nueva EXCLUYENDO esta reserva
     * y se recalculan {@code subtotal}/{@code montoTotal}. Los argumentos
     * baratos se validan ANTES de abrir la transaccion; la ausencia de la
     * propuesta objetivo se detecta dentro de la transaccion y hace rollback
     * (nada se persiste).
     *
     * @return la reserva gestionada; queda detached al cerrar la transaccion.
     */
    public Reserva actualizarReserva(long reservaId, long habitacionClickId, LocalDate nocheClick,
                                     TitularReserva titular, long habitacionNuevaId,
                                     LocalDate checkIn, LocalDate checkOut,
                                     int camasMatrimoniales, int camasIndividuales, int cunas,
                                     EstadoReserva estado) {
        return actualizarReserva(reservaId, habitacionClickId, nocheClick, titular, habitacionNuevaId,
                checkIn, checkOut, camasMatrimoniales, camasIndividuales, cunas, estado, null);
    }

    /**
     * Variante con atributos solicitados: cuando {@code atributos} es no nulo se
     * persisten en la propuesta nueva; cuando es nulo se usan los de la habitacion
     * nueva (comportamiento historico).
     */
    public Reserva actualizarReserva(long reservaId, long habitacionClickId, LocalDate nocheClick,
                                     TitularReserva titular, long habitacionNuevaId,
                                     LocalDate checkIn, LocalDate checkOut,
                                     int camasMatrimoniales, int camasIndividuales, int cunas,
                                     EstadoReserva estado, Set<AtributoHabitacion> atributos) {
        if (titular == null || nocheClick == null || checkIn == null || checkOut == null
                || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("fechas/titular invalidos");
        }
        if (camasMatrimoniales < 0 || camasIndividuales < 0 || cunas < 0) {
            throw new IllegalArgumentException("camas negativas");
        }
        if (estado == null) {
            throw new IllegalArgumentException("estado invalido");
        }

        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.find(Reserva.class, reservaId);
            if (r == null) {
                throw new IllegalArgumentException("reserva no existe: " + reservaId);
            }
            r.setTitular(em.merge(titular));

            Habitacion habNueva = em.find(Habitacion.class, habitacionNuevaId);
            if (habNueva == null) {
                throw new IllegalArgumentException("habitacion no existe: " + habitacionNuevaId);
            }

            RHPropuesta objetivo = propuestaQueCubre(r, habitacionClickId, nocheClick);

            Long solapadas = em.createQuery(
                            "select count(r) from Reserva r join r.rhPropuestas p " +
                                    "where r.activa = true and r.id <> :rid and p.habitacion.id = :hid " +
                                    "and p.fechaInicio < :hasta and :desde < p.fechaFin", Long.class)
                    .setParameter("rid", reservaId)
                    .setParameter("hid", habitacionNuevaId)
                    .setParameter("desde", checkIn)
                    .setParameter("hasta", checkOut)
                    .getSingleResult();
            if (solapadas != null && solapadas > 0) {
                throw new IllegalStateException("habitacion ocupada: " + habitacionNuevaId);
            }

            // Reemplazo de la propuesta objetivo (orphanRemoval borra la fila vieja).
            r.removeRHPropuesta(objetivo.getId().intValue());
            long noches = ChronoUnit.DAYS.between(checkIn, checkOut);
            Set<AtributoHabitacion> atributosPropuesta =
                    atributos != null ? new TreeSet<>(atributos) : habNueva.getAtributos();
            RHPropuesta nueva = new RHPropuesta(checkIn, checkOut, atributosPropuesta,
                    noches * habNueva.getTarifaPorNoche(), TipoMoneda.USD,
                    camasMatrimoniales, camasIndividuales, cunas);
            nueva.setHabitacion(habNueva);
            r.addRHPropuesta(nueva);

            r.setEstado(estado);
            float total = 0.0f;
            for (RHPropuesta p : r.getRHPropuestas()) {
                total += p.getSubtotal();
            }
            r.setMontoTotal(total);
            return r;
        });
    }

    /** Propuesta de {@code r} que cubre la noche indicada de la habitacion indicada. */
    private static RHPropuesta propuestaQueCubre(Reserva r, long habitacionId, LocalDate noche) {
        return r.getRHPropuestas().stream()
                .filter(p -> p.getHabitacion() != null
                        && p.getHabitacion().getId() != null
                        && p.getHabitacion().getId() == habitacionId
                        && !noche.isBefore(p.getFechaInicio())
                        && noche.isBefore(p.getFechaFin()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("sin propuesta en esa noche"));
    }

    /** Nombre para mostrar de un titular: nombres + apellidos. */
    private static String nombreTitular(TitularReserva t) {
        return (String.join(" ", t.getNombres()) + " " + String.join(" ", t.getApellidos())).trim();
    }

    /**
     * Puente del asistente de propuesta: persiste un conjunto de propuestas
     * DETACHED (tal como las devuelve {@code buscarHuecos}) como una
     * {@link Reserva} real, en UNA unica transaccion/EM nueva.
     *
     * <p>Los argumentos se validan ANTES de abrir la transaccion. Dentro de la
     * misma EM se re-liga el titular con {@code em.merge} y cada propuesta se
     * reconstruye copiando sus campos escalares y ligando su habitacion con
     * {@code em.getReference(Habitacion, id)}. Nunca se reutiliza la instancia
     * detached ni su {@code reserva}, y nunca se llama a otro controlador o
     * repositorio (eso abriria una segunda EM).
     *
     * @return la reserva gestionada; queda detached al cerrar la transaccion.
     */
    public Reserva persistirPropuestas(TitularReserva titular, Set<RHPropuesta> propuestas) {
        return persistirPropuestas(titular, propuestas, PerfilRelajacion.POR_DEFECTO);
    }

    /**
     * Variante con perfil de relajacion explicito. Las sobrecargas historicas
     * delegan con {@code POR_DEFECTO}.
     */
    public Reserva persistirPropuestas(TitularReserva titular, Set<RHPropuesta> propuestas,
                                       PerfilRelajacion perfil) {
        if (titular == null || propuestas == null || propuestas.isEmpty()) {
            throw new IllegalArgumentException("propuesta vacia");
        }

        return JpaUtil.enTransaccion(em -> {
            TitularReserva titularGestionado = em.merge(titular);
            Reserva r = new Reserva(titularGestionado);
            Solicitud solicitud = new Solicitud(titularGestionado);
            float total = 0.0f;

            for (RHPropuesta p : propuestas) {
                if (p == null || p.getHabitacion() == null || p.getHabitacion().getId() == null) {
                    throw new IllegalArgumentException("propuesta sin habitacion");
                }
                RHPropuesta nuevo = new RHPropuesta(p.getFechaInicio(), p.getFechaFin(),
                        p.getAtributosHabitacion(), p.getSubtotal(), p.getMoneda(),
                        p.getCamasMatrimoniales(), p.getCamasIndividuales(), p.getCunas());
                nuevo.setHabitacion(em.getReference(Habitacion.class, p.getHabitacion().getId()));
                r.addRHPropuesta(nuevo);
                // Una por propuesta detached; una RHPropuesta no lleva conteo de bebes.
                solicitud.addRHSolicitada(new RHSolicitada(p.getFechaInicio(), p.getFechaFin(),
                        p.getAtributosHabitacion(), p.getCamasMatrimoniales(),
                        p.getCamasIndividuales(), 0, p.getCunas()));
                total += p.getSubtotal();
            }

            r.setActiva(true);
            r.setMontoTotal(total);
            r.setMontoSenia(0.0f);
            r.setMoneda(TipoMoneda.USD);
            r.setEstado(EstadoReserva.PendienteDePago);
            r.setEfectiva(true);

            persistirReservaYSolicitud(em, r, solicitud, perfil);
            return r;
        });
    }

    /**
     * Removes the reservation only when it has no payments (the C++ rule:
     * eliminarReserva clears the room-stays and frees the object only when
     * pagos is empty). Returns true when the reservation was deleted.
     */
    public boolean eliminarReserva(long reservaId) {
        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.find(Reserva.class, reservaId);
            if (r == null) {
                return false;
            }
            Long pagos = em.createQuery("select count(p) from Pago p where p.reserva.id = :rid", Long.class)
                    .setParameter("rid", reservaId)
                    .getSingleResult();
            if (pagos != null && pagos > 0) {
                return false;
            }
            // Los contratos espejo referencian la reserva (FK reserva_id): se borran
            // antes que la reserva para no violar la integridad referencial. La
            // solicitud sobrevive (su pedido sigue siendo valido, sin reserva efectiva).
            em.createQuery("delete from ContratoReserva c where c.reserva.id = :rid")
                    .setParameter("rid", reservaId)
                    .executeUpdate();
            em.remove(r);
            return true;
        });
    }

    // ------------------------------------------------------- move (drag & drop)

    /**
     * Compatibilidad de TODAS las habitaciones para mover el bloque que cubre
     * {@code (habitacionOrigenId, nocheOrigen)} de la reserva. Una sola consulta
     * por habitacion, sin llamar a otros controladores dentro de la transaccion.
     */
    public List<ResultadoCompatibilidad> compatibilidadMovimiento(long reservaId, long habitacionOrigenId,
                                                                  LocalDate nocheOrigen) {
        if (nocheOrigen == null) {
            throw new IllegalArgumentException("noche invalida");
        }
        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.find(Reserva.class, reservaId);
            if (r == null) {
                throw new IllegalArgumentException("reserva no existe: " + reservaId);
            }
            RHPropuesta objetivo = propuestaQueCubre(r, habitacionOrigenId, nocheOrigen);

            List<Habitacion> habitaciones = em.createQuery(
                            "select distinct h from Habitacion h left join fetch h.atributos order by h.numero",
                            Habitacion.class)
                    .getResultList();

            List<ResultadoCompatibilidad> resultado = new ArrayList<>();
            for (Habitacion h : habitaciones) {
                ResultadoCompatibilidad entrada =
                        new ResultadoCompatibilidad(h.getId(), evaluarMovimiento(em, r, objetivo, h));
                resultado.add(entrada);
            }
            return resultado;
        });
    }

    /**
     * Mueve SOLO el bloque (propuesta) que cubre {@code (habitacionOrigenId,
     * nocheOrigen)} a {@code habitacionDestinoId}, conservando las fechas. Corre
     * en UNA transaccion: si la habitacion destino no es compatible, devuelve
     * {@code (false, motivo)} SIN mutar nada (rollback implicito del resto).
     * En exito reemplaza unicamente esa propuesta (atributos y tarifa del
     * destino) y recalcula {@code montoTotal} y el {@code subtotal} movido.
     */
    public ResultadoMovimiento moverReserva(long reservaId, long habitacionOrigenId,
                                            LocalDate nocheOrigen, long habitacionDestinoId) {
        if (nocheOrigen == null) {
            throw new IllegalArgumentException("noche invalida");
        }
        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.find(Reserva.class, reservaId);
            if (r == null) {
                throw new IllegalArgumentException("reserva no existe: " + reservaId);
            }
            RHPropuesta objetivo = propuestaQueCubre(r, habitacionOrigenId, nocheOrigen);

            Habitacion destino = em.find(Habitacion.class, habitacionDestinoId);
            if (destino == null) {
                throw new IllegalArgumentException("habitacion no existe: " + habitacionDestinoId);
            }

            MotivoRechazo motivo = evaluarMovimiento(em, r, objetivo, destino);
            if (motivo != null) {
                return new ResultadoMovimiento(false, motivo);
            }

            LocalDate ini = objetivo.getFechaInicio();
            LocalDate fin = objetivo.getFechaFin();
            int m = objetivo.getCamasMatrimoniales();
            int i = objetivo.getCamasIndividuales();
            int cunas = objetivo.getCunas();
            TipoMoneda moneda = objetivo.getMoneda();

            r.removeRHPropuesta(objetivo.getId().intValue());
            long noches = ChronoUnit.DAYS.between(ini, fin);
            RHPropuesta nueva = new RHPropuesta(ini, fin, destino.getAtributos(),
                    noches * destino.getTarifaPorNoche(), moneda, m, i, cunas);
            nueva.setHabitacion(destino);
            r.addRHPropuesta(nueva);

            float total = 0.0f;
            for (RHPropuesta p : r.getRHPropuestas()) {
                total += p.getSubtotal();
            }
            r.setMontoTotal(total);

            return new ResultadoMovimiento(true, null);
        });
    }

    /**
     * Marca el flag {@code lateCheckout} de una reserva. Toggle operativo con
     * setter dedicado (no forma parte de la validacion estructural).
     */
    public Reserva marcarLateCheckout(long reservaId, boolean lateCheckout) {
        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.find(Reserva.class, reservaId);
            if (r == null) {
                throw new IllegalArgumentException("reserva no existe: " + reservaId);
            }
            r.setLateCheckout(lateCheckout);
            return r;
        });
    }

    /**
     * Arma el {@link CompatibilidadMovimiento.HabitacionDestino} del bloque
     * movido sobre una habitacion candidata y corre el predicado puro. Devuelve
     * {@code null} cuando es compatible.
     */
    private static MotivoRechazo evaluarMovimiento(EntityManager em, Reserva reserva,
                                                   RHPropuesta objetivo, Habitacion destino) {
        LocalDate ini = objetivo.getFechaInicio();
        LocalDate fin = objetivo.getFechaFin();

        boolean libre = estaLibreParaMovimiento(em, destino.getId(), ini, fin, reserva.getId());
        boolean checkInEnFin = existeCheckInEn(em, destino.getId(), fin);
        boolean lateTerminaEnIni = existeLateCheckoutTerminaEn(em, destino.getId(), ini);

        CompatibilidadMovimiento.BloqueMovido movido = new CompatibilidadMovimiento.BloqueMovido(
                objetivo.getAtributosHabitacion(), objetivo.getCamasMatrimoniales(),
                objetivo.getCamasIndividuales(), objetivo.getCunas(), ini, fin, reserva.isLateCheckout());

        CompatibilidadMovimiento.HabitacionDestino dst = new CompatibilidadMovimiento.HabitacionDestino(
                destino.getId(), destino.getAtributos(), destino.getCantidadMatrimoniales(),
                destino.getCantidadIndividuales(), destino.getCantidadCunas(), destino.capacidad(),
                destino.isLimpia(), libre, checkInEnFin, lateTerminaEnIni);

        return CompatibilidadMovimiento.evaluar(movido, dst);
    }

    /** True cuando la habitacion no tiene noches ocupadas por OTRAS reservas activas en el rango. */
    private static boolean estaLibreParaMovimiento(EntityManager em, Long habitacionId,
                                                   LocalDate desde, LocalDate hasta, Long reservaId) {
        Long n = em.createQuery(
                        "select count(r) from Reserva r join r.rhPropuestas p " +
                                "where r.activa = true and r.id <> :rid and p.habitacion.id = :hid " +
                                "and p.fechaInicio < :hasta and :desde < p.fechaFin", Long.class)
                .setParameter("rid", reservaId)
                .setParameter("hid", habitacionId)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getSingleResult();
        return n == null || n == 0;
    }

    /** True cuando alguna reserva activa tiene una propuesta que arranca exactamente en {@code dia}. */
    private static boolean existeCheckInEn(EntityManager em, Long habitacionId, LocalDate dia) {
        Long n = em.createQuery(
                        "select count(r) from Reserva r join r.rhPropuestas p " +
                                "where r.activa = true and p.habitacion.id = :hid and p.fechaInicio = :dia",
                        Long.class)
                .setParameter("hid", habitacionId)
                .setParameter("dia", dia)
                .getSingleResult();
        return n != null && n > 0;
    }

    /** True cuando alguna reserva activa con late check-out tiene una propuesta que termina en {@code dia}. */
    private static boolean existeLateCheckoutTerminaEn(EntityManager em, Long habitacionId, LocalDate dia) {
        Long n = em.createQuery(
                        "select count(r) from Reserva r join r.rhPropuestas p " +
                                "where r.activa = true and r.lateCheckout = true " +
                                "and p.habitacion.id = :hid and p.fechaFin = :dia", Long.class)
                .setParameter("hid", habitacionId)
                .setParameter("dia", dia)
                .getSingleResult();
        return n != null && n > 0;
    }

    /**
     * Guarda rule 2 compartida por los caminos de creacion: rechaza cuando la
     * habitacion tiene una reserva activa con {@code lateCheckout=true} que
     * termina exactamente el dia de check-in pedido y la habitacion no esta
     * limpia. Lanzar dentro de la transaccion garantiza el rollback (nada se
     * persiste).
     */
    private static void verificarGuardaLimpieza(EntityManager em, Long habitacionId,
                                                LocalDate checkIn, boolean limpia) {
        if (existeLateCheckoutTerminaEn(em, habitacionId, checkIn) && !limpia) {
            throw new IllegalStateException("no disponible por limpieza (late checkout): " + habitacionId);
        }
    }

    // ------------------------------------------------------------- read views

    public List<Reserva> getReservas() {
        return repositorio.findAll();
    }

    /** Flattened read view of every reservation for the receptionist list. */
    public List<ReservaVista> listarReservas() {
        List<ReservaVista> vistas = new ArrayList<>();
        for (Reserva r : repositorio.findAllConDetalle()) {
            vistas.add(aReservaVista(r));
        }
        return vistas;
    }

    /**
     * Matriz de ocupacion READ-ONLY para el calendario: una fila por habitacion
     * (todas, ordenadas por numero) con el conjunto de noches ocupadas por
     * reservas ACTIVAS en la ventana [desde, hasta). Una noche esta ocupada
     * iff alguna propuesta activa cumple {@code fechaInicio <= noche < fechaFin}
     * y {@code noche} cae dentro de [desde, hasta). No escribe nada ni abre
     * transacciones anidadas: {@link ControladorHabitaciones#getHabitaciones()}
     * y {@code findActivasEnRango} son dos lecturas secuenciales.
     */
    public List<OcupacionHabitacion> matrizOcupacion(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new IllegalArgumentException("rango de fechas invalido");
        }

        Map<Long, Set<LocalDate>> nochesPorHabitacion = new HashMap<>();
        Map<Long, Map<LocalDate, Long>> reservasPorHabitacion = new HashMap<>();
        Map<Long, List<OcupacionHabitacion.BloqueNoche>> bloquesPorHabitacion = new HashMap<>();
        for (RHPropuesta p : repositorioPropuestas.findActivasEnRango(desde, hasta)) {
            if (p.getHabitacion() == null || p.getHabitacion().getId() == null) {
                continue;
            }
            Long habitacionId = p.getHabitacion().getId();
            Long reservaId = p.getReserva() != null ? p.getReserva().getId() : null;
            Set<LocalDate> noches = nochesPorHabitacion.computeIfAbsent(habitacionId, k -> new TreeSet<>());
            Map<LocalDate, Long> porNoche = reservasPorHabitacion.computeIfAbsent(habitacionId, k -> new HashMap<>());
            for (LocalDate noche = p.getFechaInicio();
                 noche != null && p.getFechaFin() != null && noche.isBefore(p.getFechaFin());
                 noche = noche.plusDays(1)) {
                if (!noche.isBefore(desde) && noche.isBefore(hasta)) {
                    noches.add(noche);
                    if (reservaId != null) {
                        porNoche.put(noche, reservaId);
                    }
                }
            }
            if (reservaId != null && p.getFechaInicio() != null && p.getFechaFin() != null) {
                bloquesPorHabitacion.computeIfAbsent(habitacionId, k -> new ArrayList<>())
                        .add(new OcupacionHabitacion.BloqueNoche(reservaId, p.getFechaInicio(),
                                p.getFechaFin(), p.getReserva().isLateCheckout()));
            }
        }

        List<OcupacionHabitacion> resultado = new ArrayList<>();
        for (Habitacion h : ControladorHabitaciones.getInstance().getHabitaciones()) {
            resultado.add(new OcupacionHabitacion(h,
                    nochesPorHabitacion.getOrDefault(h.getId(), Set.of()),
                    reservasPorHabitacion.getOrDefault(h.getId(), Map.of()),
                    bloquesPorHabitacion.getOrDefault(h.getId(), List.of())));
        }
        return resultado;
    }

    public Set<DTReserva> mostrarReservas() {
        Set<DTReserva> resultado = new TreeSet<>();
        for (Reserva r : repositorio.findAll()) {
            resultado.add(aDTReserva(r));
        }
        return resultado;
    }

    public Set<DTReserva> mostrarReservasPorTitular(long titularId) {
        Set<DTReserva> resultado = new TreeSet<>();
        for (Reserva r : repositorio.findByTitularId(titularId)) {
            resultado.add(aDTReserva(r));
        }
        return resultado;
    }

    public Set<DTPago> mostrarPagos(long reservaId) {
        Set<DTPago> resultado = new TreeSet<>();
        for (com.pms.entities.Pago p : repositorio.findPagosByReservaId(reservaId)) {
            resultado.add(new DTPago(p.getMonto(), p.getMoneda(), p.getFecha()));
        }
        return resultado;
    }

    public Set<DTRHPropuesta> mostrarRHPropuestasPorCheckin(LocalDate checkin) {
        Set<DTRHPropuesta> resultado = new TreeSet<>();
        for (RHPropuesta rhp : repositorioPropuestas.findByFechaInicio(checkin)) {
            resultado.add(aDTRHPropuesta(rhp));
        }
        return resultado;
    }

    public Set<DTRHPropuesta> mostrarRHPropuestasPorCheckout(LocalDate checkout) {
        Set<DTRHPropuesta> resultado = new TreeSet<>();
        for (RHPropuesta rhp : repositorioPropuestas.findByFechaFin(checkout)) {
            resultado.add(aDTRHPropuesta(rhp));
        }
        return resultado;
    }

    /** Proposals whose stay overlaps [desde, hasta) — C++ mostrarRHPropuestasPorRangoFechasEnEstadia. */
    public Set<DTRHPropuesta> mostrarRHPropuestasPorRangoFechasEnEstadia(LocalDate desde, LocalDate hasta) {
        Set<DTRHPropuesta> resultado = new TreeSet<>();
        for (RHPropuesta rhp : repositorioPropuestas.findEnRango(desde, hasta)) {
            resultado.add(aDTRHPropuesta(rhp));
        }
        return resultado;
    }

    public boolean perteneceReservaATitular(long reservaId, long titularId) {
        return repositorio.perteneceReservaATitular(reservaId, titularId);
    }

    /** True when some ACTIVE reservation occupies the room in [desde, hasta). */
    public boolean isHabitacionOcupada(long habitacionId, LocalDate desde, LocalDate hasta) {
        return repositorio.habitacionOcupada(habitacionId, desde, hasta);
    }
}