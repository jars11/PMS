package com.pms.api;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorReservas;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.handlers.InteraccionUsuario;
import com.pms.handlers.ManejadorBusquedaHuecos;
import com.pms.persistence.RepositorioPersonas;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.Fechas;
import com.pms.utils.TipoMoneda;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * "Propuesta de estadía" REST endpoints. Both operations delegate to the same
 * business logic the Swing panel ({@code PanelPropuestaEstadia}) uses, without
 * reimplementing it:
 *
 * <ul>
 *   <li>{@code POST /api/propuestas/buscar} arma un {@link DTRHSolicitada} por
 *       bloque y corre {@link ManejadorBusquedaHuecos#buscarHuecos} con una
 *       {@link InteraccionUsuario} NO interactiva: la búsqueda nunca relaja las
 *       restricciones por su cuenta, así que un pedido sin capacidad devuelve
 *       una lista de propuestas vacía.</li>
 *   <li>{@code POST /api/propuestas/persistir} reconstruye las
 *       {@link RHPropuesta} del bloque elegido (habitación, fechas, atributos,
 *       subtotal = noches * tarifa, moneda y camas) y las persiste con
 *       {@link ControladorReservas#persistirPropuestas}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/propuestas")
public class PropuestasController {

    /** Mismos textos que la validación de la costura de entrada del panel Swing. */
    static final String MENSAJE_SIN_BLOQUES = "Agregá al menos un bloque de estadía.";
    static final String MENSAJE_BLOQUE_VACIO = "Hay un bloque vacío.";
    static final String MENSAJE_FECHAS_OBLIGATORIAS = "Las fechas de check-in y check-out son obligatorias.";
    static final String MENSAJE_CHECKOUT_INVALIDO = "El check-out debe ser posterior al check-in.";
    static final String MENSAJE_CANTIDADES_NEGATIVAS = "Las cantidades no pueden ser negativas.";

    /**
     * La búsqueda web no pregunta nada: rechaza toda relajación (colchones
     * extra, otra distribución de camas, atributos parciales u otras fechas) y
     * devuelve vacío. Así el resultado es determinista y no esconde un cambio
     * de pedido que el usuario no autorizó.
     */
    private static final InteraccionUsuario INTERACCION_NO_INTERACTIVA = new InteraccionUsuario() {
        @Override
        public boolean confirmar(String pregunta) {
            return false;
        }

        @Override
        public void notificar(String mensaje) {
            // Sin UI: el llamador se entera por la lista de propuestas vacía.
        }
    };

    private final ControladorReservas reservas = ControladorReservas.getInstance();
    private final ControladorHabitaciones habitaciones = ControladorHabitaciones.getInstance();
    private final RepositorioPersonas repositorioPersonas = new RepositorioPersonas();
    private final ManejadorBusquedaHuecos buscador = new ManejadorBusquedaHuecos();

    // ------------------------------------------------------------ contratos

    /** Bloque pedido: fechas, atributos deseados y cantidades de personas/camas. */
    public record BloqueBusqueda(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            List<AtributoHabitacion> atributos,
            int camasMatrimoniales,
            int camasIndividuales,
            int bebes,
            int cunas) {
    }

    public record BusquedaRequest(List<BloqueBusqueda> bloques) {
    }

    /** Bloque de una propuesta encontrada, ya con la habitación asignada. */
    public record BloquePropuesta(
            long habitacionId,
            Integer numero,
            LocalDate checkIn,
            LocalDate checkOut,
            float subtotal,
            TipoMoneda moneda,
            int camasMatrimoniales,
            int camasIndividuales,
            int cunas,
            List<AtributoHabitacion> atributos) {
    }

    /** Una alternativa completa: una habitación (o varias) que cubren los bloques. */
    public record Propuesta(List<BloquePropuesta> bloques) {
    }

    public record BusquedaResponse(List<Propuesta> propuestas) {
    }

    /** Bloque elegido para persistir: la habitación ya viene asignada. */
    public record BloquePersistir(
            long habitacionId,
            LocalDate checkIn,
            LocalDate checkOut,
            int camasMatrimoniales,
            int camasIndividuales,
            int cunas,
            List<AtributoHabitacion> atributos) {
    }

    public record PersistirRequest(Long titularId, List<BloquePersistir> bloques) {
    }

    // -------------------------------------------------------------- buscar

    /**
     * Busca alternativas de habitaciones para los bloques pedidos. Respuesta:
     * {@code {"propuestas":[{"bloques":[{...}]}]}}. Sin capacidad suficiente la
     * lista viene vacía (200), nunca se relaja el pedido.
     */
    @PostMapping("/buscar")
    public BusquedaResponse buscar(@RequestBody(required = false) BusquedaRequest request) {
        List<DTRHSolicitada> solicitudes = aSolicitudes(request == null ? null : request.bloques());

        List<Propuesta> propuestas = new ArrayList<>();
        for (Set<RHPropuesta> conjunto : buscador.buscarHuecos(solicitudes, INTERACCION_NO_INTERACTIVA)) {
            List<BloquePropuesta> bloques = new ArrayList<>();
            for (RHPropuesta p : conjunto) {
                bloques.add(aBloquePropuesta(p));
            }
            bloques.sort(Comparator.comparing(BloquePropuesta::checkIn)
                    .thenComparing(b -> b.numero() == null ? 0 : b.numero()));
            propuestas.add(new Propuesta(bloques));
        }
        return new BusquedaResponse(propuestas);
    }

    // ------------------------------------------------------------ persistir

    /**
     * Persiste la alternativa elegida como una reserva real y la devuelve con
     * la misma vista plana de {@code GET /api/reservas}.
     */
    @PostMapping("/persistir")
    public ResponseEntity<ReservaVistaDto> persistir(@RequestBody(required = false) PersistirRequest request) {
        List<BloquePersistir> bloques = request == null ? null : request.bloques();
        if (bloques == null || bloques.isEmpty()) {
            throw new IllegalArgumentException(MENSAJE_SIN_BLOQUES);
        }
        TitularReserva titular = titular(request.titularId());

        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        for (BloquePersistir b : bloques) {
            if (b == null) {
                throw new IllegalArgumentException(MENSAJE_BLOQUE_VACIO);
            }
            validarBloque(b.checkIn(), b.checkOut(),
                    b.camasMatrimoniales(), b.camasIndividuales(), b.cunas());
            Habitacion habitacion = habitaciones.getHabitacion(b.habitacionId());
            propuestas.add(aPropuesta(b, habitacion));
        }

        Reserva reserva = reservas.persistirPropuestas(titular, propuestas);
        return ResponseEntity.status(HttpStatus.CREATED).body(aVista(reserva));
    }

    // ------------------------------------------------------------- helpers

    /** Valida la costura de entrada y traduce cada bloque a {@link DTRHSolicitada}. */
    private static List<DTRHSolicitada> aSolicitudes(List<BloqueBusqueda> bloques) {
        if (bloques == null || bloques.isEmpty()) {
            throw new IllegalArgumentException(MENSAJE_SIN_BLOQUES);
        }
        List<DTRHSolicitada> solicitudes = new ArrayList<>();
        for (BloqueBusqueda b : bloques) {
            if (b == null) {
                throw new IllegalArgumentException(MENSAJE_BLOQUE_VACIO);
            }
            validarBloque(b.fechaInicio(), b.fechaFin(),
                    b.camasMatrimoniales(), b.camasIndividuales(), b.bebes(), b.cunas());
            solicitudes.add(new DTRHSolicitada(b.fechaInicio(), b.fechaFin(), atributos(b.atributos()),
                    b.camasMatrimoniales(), b.camasIndividuales(), b.bebes(), b.cunas()));
        }
        return solicitudes;
    }

    /** Reglas de la costura de entrada del panel Swing, con los mismos mensajes. */
    private static void validarBloque(LocalDate checkIn, LocalDate checkOut, int... cantidades) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException(MENSAJE_FECHAS_OBLIGATORIAS);
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(MENSAJE_CHECKOUT_INVALIDO);
        }
        for (int cantidad : cantidades) {
            if (cantidad < 0) {
                throw new IllegalArgumentException(MENSAJE_CANTIDADES_NEGATIVAS);
            }
        }
    }

    private static Set<AtributoHabitacion> atributos(List<AtributoHabitacion> atributos) {
        Set<AtributoHabitacion> conjunto = new TreeSet<>();
        if (atributos != null) {
            for (AtributoHabitacion a : atributos) {
                if (a != null) {
                    conjunto.add(a);
                }
            }
        }
        return conjunto;
    }

    private static BloquePropuesta aBloquePropuesta(RHPropuesta p) {
        Habitacion habitacion = p.getHabitacion();
        return new BloquePropuesta(
                habitacion == null || habitacion.getId() == null ? 0L : habitacion.getId(),
                habitacion == null ? null : habitacion.getNumero(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getSubtotal(),
                p.getMoneda(),
                p.getCamasMatrimoniales(),
                p.getCamasIndividuales(),
                p.getCunas(),
                new ArrayList<>(new TreeSet<>(p.getAtributosHabitacion())));
    }

    /**
     * Reconstruye la propuesta que se persiste: fechas y camas del bloque
     * elegido, subtotal = noches * tarifa de la habitación y moneda USD (la
     * misma convención de {@code persistirPropuestas} y del buscador). Los
     * atributos del pedido mandan; si el bloque no trae, se usan los de la
     * habitación.
     */
    private static RHPropuesta aPropuesta(BloquePersistir b, Habitacion habitacion) {
        long noches = Fechas.diasEntre(b.checkIn(), b.checkOut());
        Set<AtributoHabitacion> atributosPropuesta = b.atributos() == null
                ? new TreeSet<>(habitacion.getAtributos())
                : atributos(b.atributos());
        RHPropuesta propuesta = new RHPropuesta(b.checkIn(), b.checkOut(), atributosPropuesta,
                noches * habitacion.getTarifaPorNoche(), TipoMoneda.USD,
                b.camasMatrimoniales(), b.camasIndividuales(), b.cunas());
        propuesta.setHabitacion(habitacion);
        return propuesta;
    }

    /** Titular por id; mismo contrato de errores que {@code ReservasController}. */
    private TitularReserva titular(Long titularId) {
        if (titularId == null) {
            throw new IllegalArgumentException("titularId es obligatorio");
        }
        TitularReserva t = repositorioPersonas.findTitularById(titularId);
        if (t == null) {
            throw new IllegalArgumentException("titular no existe: " + titularId);
        }
        return t;
    }

    /** Vista plana de la reserva creada, igual a la del listado de reservas. */
    private ReservaVistaDto aVista(Reserva r) {
        return reservas.listarReservas().stream()
                .filter(v -> r.getId() != null && v.id() == r.getId())
                .map(v -> ReservaVistaDto.de(v, r.isLateCheckout()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("reserva no reflejada en la vista"));
    }
}
