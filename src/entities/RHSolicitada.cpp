#include "../../include/entities/RHSolicitada.hpp"

// Constructor
RHSolicitada::RHSolicitada(const Fecha& fechaInicio, const Fecha& fechaFin, const std::set<AtributoHabitacion>& atributosHabitacion,
                           int cantMatrimonios, int cantIndividuales, int cantBebes, int cantCunas)
    : ReservaHabitacion(fechaInicio, fechaFin, atributosHabitacion),
      cantMatrimonios(cantMatrimonios), cantIndividuales(cantIndividuales),
      cantBebes(cantBebes), cantCunas(cantCunas) {}

// Getters
int RHSolicitada::getCantMatrimonios() const {
    return cantMatrimonios;
}
int RHSolicitada::getCantIndividuales() const {
    return cantIndividuales;
}
int RHSolicitada::getCantBebes() const {
    return cantBebes;
}
int RHSolicitada::getCantCunas() const {
    return cantCunas;
}
std::set<RHPropuesta*> RHSolicitada::getRHPropuestasAsociadas() const {
    return rhpropuestasAsociadas;
}

// Setters
void RHSolicitada::addRHPropuestaAsociada(RHPropuesta* rhpropuesta) {
    rhpropuestasAsociadas.insert(rhpropuesta);
}

// Destructor
RHSolicitada::~RHSolicitada() {}