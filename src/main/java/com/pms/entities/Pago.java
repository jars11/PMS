package com.pms.entities;

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

import com.pms.utils.TipoMoneda;

import java.time.LocalDate;

/**
 * Payment of a reservation. In the C++ design the payment also knew its
 * titular; in the JPA mapping the reservation (and its titular) is enough.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private float monto;

    @Enumerated(EnumType.STRING)
    private TipoMoneda moneda;

    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    protected Pago() {
    }

    public Pago(float monto, TipoMoneda moneda, LocalDate fecha, Reserva reserva) {
        this.monto = monto;
        this.moneda = moneda;
        this.fecha = fecha;
        this.reserva = reserva;
    }

    public Long getId() {
        return id;
    }

    public float getMonto() {
        return monto;
    }

    public TipoMoneda getMoneda() {
        return moneda;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }
}