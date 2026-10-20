package com.pms.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.pms.utils.EstadoReserva;
import com.pms.utils.TipoMoneda;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reservation of one or more room-stay proposals (RHPropuesta) for a titular.
 * Business rule (preserved from C++): a reservation can only be deleted when
 * it has no payments.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    /** Default late check-out flag for a new reservation. */
    public static final boolean LATE_CHECKOUT_POR_DEFECTO = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean activa;

    // default false para que hbm2ddl update pueda agregar la columna a la tabla existente
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean lateCheckout = LATE_CHECKOUT_POR_DEFECTO;

    @Enumerated(EnumType.STRING)
    private TipoMoneda moneda = TipoMoneda.USD;

    private float montoTotal;

    private float montoPagado;

    private float montoSenia;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado = EstadoReserva.PendienteDePago;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "titular_id", nullable = false)
    private TitularReserva titular;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RHPropuesta> rhPropuestas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Pago> pagos = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "reserva_pasajero",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "pasajero_id"))
    private Set<Pasajero> pasajeros = new HashSet<>();

    protected Reserva() {
    }

    public Reserva(TitularReserva titular) {
        this.titular = titular;
    }

    public Long getId() {
        return id;
    }

    public boolean isActiva() {
        return activa;
    }

    public boolean isLateCheckout() {
        return lateCheckout;
    }

    public void setLateCheckout(boolean lateCheckout) {
        this.lateCheckout = lateCheckout;
    }

    public TipoMoneda getMoneda() {
        return moneda;
    }

    public float getMontoTotal() {
        return montoTotal;
    }

    public float getMontoPagado() {
        return montoPagado;
    }

    public float getMontoSenia() {
        return montoSenia;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public TitularReserva getTitular() {
        return titular;
    }

    public void setTitular(TitularReserva titular) {
        this.titular = titular;
    }

    public Set<Pago> getPagos() {
        return pagos;
    }

    public Set<RHPropuesta> getRHPropuestas() {
        return rhPropuestas;
    }

    public Set<Pasajero> getPasajeros() {
        return pasajeros;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public void setMontoTotal(float monto) {
        this.montoTotal = monto;
    }

    public void setMontoSenia(float monto) {
        this.montoSenia = monto;
    }

    public void setMoneda(TipoMoneda moneda) {
        this.moneda = moneda;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public void addPago(Pago pago) {
        pago.setReserva(this);
        pagos.add(pago);
    }

    public void addRHPropuesta(RHPropuesta rhPropuesta) {
        rhPropuesta.setReserva(this);
        rhPropuestas.add(rhPropuesta);
    }

    public void removeRHPropuesta(int idPropuesta) {
        rhPropuestas.removeIf(p -> p.getId() != null && p.getId() == idPropuesta);
    }

    @Override
    public String toString() {
        return "Reserva{id=" + id + ", activa=" + activa + ", total=" + montoTotal
                + ", estado=" + estado + ", propuestas=" + rhPropuestas.size() + '}';
    }
}