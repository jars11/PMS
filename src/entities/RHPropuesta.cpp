#include "../../include/entities/RHPropuesta.hpp"

// Constructor
RHPropuesta::RHPropuesta(const Fecha& fechaInicio, const Fecha& fechaFin, const std::set<AtributoHabitacion>& atributosHabitacion,
                         float subtotal, TipoMoneda moneda, int camasMatrimoniales, int camasIndividuales, int cunas)
    : ReservaHabitacion(fechaInicio, fechaFin, atributosHabitacion),
      subtotal(subtotal), moneda(moneda), camasMatrimoniales(camasMatrimoniales),
      camasIndividuales(camasIndividuales), cunas(cunas), habitacion(nullptr) {}

// Getters
int RHPropuesta::getId() const {
    return id;
}
float RHPropuesta::getSubtotal() const {
    return subtotal;
}
TipoMoneda RHPropuesta::getMoneda() const {
    return moneda;
}
int RHPropuesta::getCamasMatrimoniales() const {
    return camasMatrimoniales;
}
int RHPropuesta::getCamasIndividuales() const {
    return camasIndividuales;
}
int RHPropuesta::getCunas() const {
    return cunas;
}
Habitacion* RHPropuesta::getHabitacion() const {
    return habitacion;
}
std::set<RHSolicitada*> RHPropuesta::getRHSolicitadasAsociadas() const {
    return rhsolicitadasAsociadas;
}

// Setters
void RHPropuesta::setHabitacion(Habitacion* habitacion) {
    this->habitacion = habitacion;
}

// Destructor
RHPropuesta::~RHPropuesta() {}