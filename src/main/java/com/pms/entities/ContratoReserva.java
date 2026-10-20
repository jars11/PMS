package com.pms.entities;

import com.pms.dtos.PerfilRelajacion;
import com.pms.utils.NivelCapacidad;

import jakarta.persistence.Column;
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
import jakarta.persistence.UniqueConstraint;

/**
 * Association class between {@link Solicitud} and {@link Reserva} recording the
 * relaxation under which the reserva was accepted. This is the ONLY link
 * between both entities: {@code solicitud_id} and {@code reserva_id} are each
 * NOT NULL and UNIQUE (one contract per side).
 */
@Entity
@Table(name = "contrato_reserva",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "solicitud_id"),
                @UniqueConstraint(columnNames = "reserva_id")})
public class ContratoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    // default false para que hbm2ddl update pueda agregar la columna a la tabla existente
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean fechasRelajadas;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean atributosRelajados;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean colchonesExtra;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_capacidad")
    private NivelCapacidad nivelCapacidad = NivelCapacidad.DISTRIBUCION_CAMAS;

    protected ContratoReserva() {
    }

    public ContratoReserva(Solicitud solicitud, Reserva reserva, PerfilRelajacion perfil) {
        this.solicitud = solicitud;
        this.reserva = reserva;
        PerfilRelajacion usado = perfil != null ? perfil : PerfilRelajacion.POR_DEFECTO;
        this.fechasRelajadas = usado.fechasRelajadas();
        this.atributosRelajados = usado.atributosRelajados();
        this.colchonesExtra = usado.colchonesExtra();
        this.nivelCapacidad = usado.nivelCapacidad() != null
                ? usado.nivelCapacidad() : NivelCapacidad.DISTRIBUCION_CAMAS;
    }

    public Long getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public boolean isFechasRelajadas() {
        return fechasRelajadas;
    }

    public boolean isAtributosRelajados() {
        return atributosRelajados;
    }

    public boolean isColchonesExtra() {
        return colchonesExtra;
    }

    public NivelCapacidad getNivelCapacidad() {
        return nivelCapacidad;
    }
}
