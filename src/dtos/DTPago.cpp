#include "../../include/dtos/DTPago.hpp"

DTPago::DTPago(float monto, TipoMoneda moneda, Fecha fechaPago)
    : monto(monto), moneda(moneda), fechaPago(fechaPago) {}

// Getters
float DTPago::getMonto() const {
    return monto;
}
TipoMoneda DTPago::getMoneda() const {
    return moneda;
}
Fecha DTPago::getFechaPago() const {
    return fechaPago;
}

bool DTPago::operator==(const DTPago& other) const {
    return monto == other.monto &&
           moneda == other.moneda &&
           fechaPago == other.fechaPago;
}

// Destructor
DTPago::~DTPago() {}