#ifndef DTRHSOLICITADA_HPP
#define DTRHSOLICITADA_HPP

#include "DTReservaHabitacion.hpp"

class DTRHSolicitada : public DTReservaHabitacion {
    private:
        int cantMatrimonios;
        int cantIndividuales;
        int cantBebes;
        int cantCunas;
    
    public:
        // Constructor
        DTRHSolicitada(Fecha fechaInicio, Fecha fechaFin, std::set<AtributoHabitacion> atributosHabitacion,
                       int cantMatrimonios, int cantIndividuales, int cantBebes, int cantCunas);
        
        // Getters
        int getCantMatrimonios() const;
        int getCantIndividuales() const;
        int getCantBebes() const;
        int getCantCunas() const;
        bool operator==(const DTRHSolicitada& other) const;

        // Destructor
        ~DTRHSolicitada();
};

#endif