#include "../../include/entities/Pago.hpp"

// Constructor
Pago::Pago(float monto, TipoMoneda moneda, Fecha fecha, TitularReserva* titular, Reserva* reserva)
    : monto(monto), moneda(moneda), fecha(fecha), titular(titular), reserva(reserva) {}

// Getters
int Pago::getId() const {
    return id;
}
float Pago::getMonto() const {
    return monto;
}
TipoMoneda Pago::getMoneda() const {
    return moneda;
}
Fecha Pago::getFecha() const {
    return fecha;
}
Reserva* Pago::getReserva() const {
    return reserva;
}
TitularReserva* Pago::getTitular() const {
    return titular;
}