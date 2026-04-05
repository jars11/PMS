#include "../../include/dtos/DTRHSolicitada.hpp"

// Constructor
DTRHSolicitada::DTRHSolicitada(Fecha fechaInicio, Fecha fechaFin, std::set<AtributoHabitacion> atributosHabitacion,
                               int cantMatrimonios, int cantIndividuales, int cantBebes, int cantCunas)
    : DTReservaHabitacion(fechaInicio, fechaFin, atributosHabitacion),
      cantMatrimonios(cantMatrimonios),
      cantIndividuales(cantIndividuales),
      cantBebes(cantBebes),
      cantCunas(cantCunas) {}

// Getters
int DTRHSolicitada::getCantMatrimonios() const {
    return cantMatrimonios;
}
int DTRHSolicitada::getCantIndividuales() const {
    return cantIndividuales;
}
int DTRHSolicitada::getCantBebes() const {
    return cantBebes;
}
int DTRHSolicitada::getCantCunas() const {
    return cantCunas;
}

bool DTRHSolicitada::operator==(const DTRHSolicitada& other) const {
    return DTReservaHabitacion::operator==(other) &&
           cantMatrimonios == other.cantMatrimonios &&
           cantIndividuales == other.cantIndividuales &&
           cantBebes == other.cantBebes &&
           cantCunas == other.cantCunas;
}

// Destructor
DTRHSolicitada::~DTRHSolicitada() {}