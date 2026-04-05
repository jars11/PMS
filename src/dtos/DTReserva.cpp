#include "../../include/dtos/DTReserva.hpp"

DTReserva::DTReserva(bool activa, float montoTotal, float montoSenia, TipoMoneda moneda, EstadoReserva estado, std::set<DTRHPropuesta> dtrhpropuestas)
    : activa(activa), montoTotal(montoTotal), montoSenia(montoSenia), moneda(moneda), estado(estado), dtrhpropuestas(dtrhpropuestas) {}

// Getters
bool DTReserva::isActiva() const {
    return activa;
}
float DTReserva::getMontoTotal() const {
    return montoTotal;
}
float DTReserva::getMontoSenia() const {
    return montoSenia;
}
TipoMoneda DTReserva::getMoneda() const {
    return moneda;
}
EstadoReserva DTReserva::getEstado() const {
    return estado;
}
std::set<DTRHPropuesta> DTReserva::getDTRHPropuestas() const {
    return dtrhpropuestas;
}

bool DTReserva::operator==(const DTReserva& other) const {
    return activa == other.activa &&
           montoTotal == other.montoTotal &&
           montoSenia == other.montoSenia &&
           moneda == other.moneda &&
           estado == other.estado &&
           dtrhpropuestas == other.dtrhpropuestas;
}

// Destructor
DTReserva::~DTReserva() {}