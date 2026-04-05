#include "../../include/dtos/DTRHPropuesta.hpp"

// Constructor
DTRHPropuesta::DTRHPropuesta(Fecha fechaInicio, Fecha fechaFin, std::set<AtributoHabitacion> atributosHabitacion, float subtotal, TipoMoneda moneda, int camasMatrimoniales, int camasIndividuales, int cunas)
    : DTReservaHabitacion(fechaInicio, fechaFin, atributosHabitacion), subtotal(subtotal), moneda(moneda), camasMatrimoniales(camasMatrimoniales), camasIndividuales(camasIndividuales), cunas(cunas) {}

// Getters
float DTRHPropuesta::getSubtotal() const {
    return subtotal;
}
TipoMoneda DTRHPropuesta::getMoneda() const {
    return moneda;
}
int DTRHPropuesta::getCamasMatrimoniales() const {
    return camasMatrimoniales;
}
int DTRHPropuesta::getCamasIndividuales() const {
    return camasIndividuales;
}
int DTRHPropuesta::getCunas() const {
    return cunas;
}
bool DTRHPropuesta::operator==(const DTRHPropuesta& other) const {
    return DTReservaHabitacion::operator==(other) &&
           subtotal == other.subtotal &&
           moneda == other.moneda &&
           camasMatrimoniales == other.camasMatrimoniales &&
           camasIndividuales == other.camasIndividuales &&
           cunas == other.cunas;
}

// Destructor
DTRHPropuesta::~DTRHPropuesta() {}