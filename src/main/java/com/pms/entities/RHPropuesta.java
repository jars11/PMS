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
 * room, the night rates and the bed distribution.
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

    protected RHPropuesta() {
    }

    public RHPropuesta(LocalDate fechaInicio, LocalDate fechaFin,
                       Set<AtributoHabitacion> atributosHabitacion,
                       float subtotal, TipoMoneda moneda,
                       int camasMatrimoniales, int camasIndividuales, int cunas) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.atributosHabitacion = new TreeSet<>(atributosHabitacion != null ? atributosHabitacion : Set.of());
        this.subtotal = subtotal;
        this.moneda = moneda;
        this.camasMatrimoniales = camasMatrimoniales;
        this.camasIndividuales = camasIndividuales;
        this.cunas = cunas;
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
}