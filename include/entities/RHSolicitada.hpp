#ifndef RHSOLICITADA_HPP
#define RHSOLICITADA_HPP

#include <set>
#include "../utils.hpp"
#include "../dtos/Fecha.hpp"
#include "ReservaHabitacion.hpp"
#include "RHPropuesta.hpp"

class RHSolicitada : public ReservaHabitacion {
    private:
        int cantMatrimonios;
        int cantIndividuales;
        int cantBebes;
        int cantCunas;
        std::set<RHPropuesta*> rhpropuestasAsociadas;

    public:
        // Constructor
        RHSolicitada(const Fecha& fechaInicio, const Fecha& fechaFin, const std::set<AtributoHabitacion>& atributosHabitacion,
                     int cantMatrimonios, int cantIndividuales, int cantBebes, int cantCunas);

        // Getters
        int getCantMatrimonios() const;
        int getCantIndividuales() const;
        int getCantBebes() const;
        int getCantCunas() const;
        std::set<RHPropuesta*> getRHPropuestasAsociadas() const;

        // Setters
        void addRHPropuestaAsociada(RHPropuesta* rhpropuesta);

        // Destructor
        ~RHSolicitada();
};

#endif