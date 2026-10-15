package com.pms.utils;

/** Reservation state. Declaration order matches the C++ enum class. */
public enum EstadoReserva {
    PendienteDePago,
    SeniaPaga,
    PagoTotal,
    Finalizada;

    @Override
    public String toString() {
        return name();
    }
}