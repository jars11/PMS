#ifndef MANEJADORCAPACIDAD_HPP
#define MANEJADORCAPACIDAD_HPP

#include <set>
#include "../dtos/DTCapacidadFaltante.hpp"
#include "../dtos/DTRHSolicitada.hpp"

class ManejadorCapacidad {
    public:
        std::set<DTCapacidadFaltante> hayCapacidad(DTRHSolicitada dtRHS);
};

#endif