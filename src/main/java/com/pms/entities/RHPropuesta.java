package com.pms.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoMoneda;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/**
 * Proposed room-stay for one block of the reservation: fixed dates, a concrete
 * room, the night rates, the physical bed/crib allocation and the people
 * categories (couples, individual persons and babies).
 */
@Entity
@Table(name = "rhpropuesta")
public class RHPropuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "habitacion_id")
    private Habitacion habitacion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rhpropuesta_atributos", joinColumns = @JoinColumn(name = "rhpropuesta_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "atributo")
    private Set<AtributoHabitacion> atributosHabitacion = new TreeSet<>();

    private float subtotal;

    @Enumerated(EnumType.STRING)
    private TipoMoneda moneda = TipoMoneda.USD;

    private int camasMatrimoniales;

    private int camasIndividuales;

    private int cunas;

    // Bebes pedidos para el bloque. default 0 para que hbm2ddl update pueda
    // agregar la columna a una tabla existente. Las CUNAS son un recurso de la
    // habitacion: puede haber menos cunas que bebes (un bebe puede dormir con
    // los padres).
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantBebes;

    // Personas pedidas para el bloque. Las CAMAS son el subconjunto de camas de
    // la habitacion que se usan, pero no definen el conteo: una matrimonial
    // puede ser usada por una sola persona y una habitacion puede ofrecer mas
    // camas que personas. default 0 para que hbm2ddl update agregue la columna;
    // las filas viejas caen al calculo por camas.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantPersonas;

    // Desglose de las personas por categoria. Es la fuente nueva del conteo:
    // cada matrimonio/pareja son 2 personas y cada persona individual es 1.
    // Se guarda tambien cantPersonas por compatibilidad con filas viejas.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantMatrimonios;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantPersonasIndividuales;

    protected RHPropuesta() {
    }

    public RHPropuesta(LocalDate fechaInicio, LocalDate fechaFin,
                       Set<AtributoHabitacion> atributosHabitacion,
                       float subtotal, TipoMoneda moneda,
                       int camasMatrimoniales, int camasIndividuales, int cunas) {
        this(fechaInicio, fechaFin, atributosHabitacion, subtotal, moneda,
                camasMatrimoniales, camasIndividuales, cunas,
                camasMatrimoniales, camasIndividuales, 0);
    }

    public RHPropuesta(LocalDate fechaInicio, LocalDate fechaFin,
                       Set<AtributoHabitacion> atributosHabitacion,
                       float subtotal, TipoMoneda moneda,
                       int camasMatrimoniales, int camasIndividuales, int cunas, int cantBebes) {
        this(fechaInicio, fechaFin, atributosHabitacion, subtotal, moneda,
                camasMatrimoniales, camasIndividuales, cunas,
                camasMatrimoniales, camasIndividuales, cantBebes);
    }

    /**
     * Constructor legacy: conserva el total de personas pedido sin desglose.
     * El desglose (parejas / individuales) queda en 0 y se deriva de las camas
     * cuando haga falta.
     */
    public RHPropuesta(LocalDate fechaInicio, LocalDate fechaFin,
                       Set<AtributoHabitacion> atributosHabitacion,
                       float subtotal, TipoMoneda moneda,
                       int camasMatrimoniales, int camasIndividuales, int cunas,
                       int cantBebes, int cantPersonas) {
        inicializar(fechaInicio, fechaFin, atributosHabitacion, subtotal, moneda,
                camasMatrimoniales, camasIndividuales, cunas, 0, 0, cantBebes,
                cantPersonas);
    }

    /**
     * Constructor nuevo: las camas y cunas son los recursos usados y el conteo
     * de personas viene desglosado en parejas/matrimonios + personas
     * individuales. El total de personas se deriva de ese desglose.
     */
    public RHPropuesta(LocalDate fechaInicio, LocalDate fechaFin,
                       Set<AtributoHabitacion> atributosHabitacion,
                       float subtotal, TipoMoneda moneda,
                       int camasMatrimoniales, int camasIndividuales, int cunas,
                       int cantMatrimonios, int cantPersonasIndividuales, int cantBebes) {
        inicializar(fechaInicio, fechaFin, atributosHabitacion, subtotal, moneda,
                camasMatrimoniales, camasIndividuales, cunas, cantMatrimonios,
                cantPersonasIndividuales, cantBebes,
                personasDeCategorias(cantMatrimonios, cantPersonasIndividuales));
    }

    private void inicializar(LocalDate fechaInicio, LocalDate fechaFin,
                             Set<AtributoHabitacion> atributosHabitacion,
                             float subtotal, TipoMoneda moneda,
                             int camasMatrimoniales, int camasIndividuales, int cunas,
                             int cantMatrimonios, int cantPersonasIndividuales,
                             int cantBebes, int cantPersonas) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.atributosHabitacion = new TreeSet<>(atributosHabitacion != null ? atributosHabitacion : Set.of());
        this.subtotal = subtotal;
        this.moneda = moneda;
        this.camasMatrimoniales = camasMatrimoniales;
        this.camasIndividuales = camasIndividuales;
        this.cunas = cunas;
        this.cantBebes = cantBebes;
        this.cantPersonas = cantPersonas;
        this.cantMatrimonios = cantMatrimonios;
        this.cantPersonasIndividuales = cantPersonasIndividuales;
    }

    /** Total de personas a partir del desglose por categoria. */
    public static int personasDeCategorias(int cantMatrimonios, int cantPersonasIndividuales) {
        return 2 * cantMatrimonios + cantPersonasIndividuales;
    }

    /** Personas implicadas por la distribucion de camas (misma regla del dominio). */
    public static int personasDerivadas(int camasMatrimoniales, int camasIndividuales) {
        return 2 * camasMatrimoniales + camasIndividuales;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public void setHabitacion(Habitacion habitacion) {
        this.habitacion = habitacion;
    }

    public Set<AtributoHabitacion> getAtributosHabitacion() {
        return atributosHabitacion;
    }

    public float getSubtotal() {
        return subtotal;
    }

    public TipoMoneda getMoneda() {
        return moneda;
    }

    public int getCamasMatrimoniales() {
        return camasMatrimoniales;
    }

    public int getCamasIndividuales() {
        return camasIndividuales;
    }

    public int getCunas() {
        return cunas;
    }

    public int getCantBebes() {
        return cantBebes;
    }

    public void setCantBebes(int cantBebes) {
        this.cantBebes = cantBebes;
    }

    public int getCantPersonas() {
        return cantPersonas;
    }

    public void setCantPersonas(int cantPersonas) {
        this.cantPersonas = cantPersonas;
    }

    public int getCantMatrimonios() {
        return cantMatrimonios;
    }

    public void setCantMatrimonios(int cantMatrimonios) {
        this.cantMatrimonios = cantMatrimonios;
    }

    public int getCantPersonasIndividuales() {
        return cantPersonasIndividuales;
    }

    public void setCantPersonasIndividuales(int cantPersonasIndividuales) {
        this.cantPersonasIndividuales = cantPersonasIndividuales;
    }
}