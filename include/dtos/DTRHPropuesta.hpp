#ifndef DTRHPROPUESTA_HPP
#define DTRHPROPUESTA_HPP

#include "../utils.hpp"
#include "Fecha.hpp"
#include <set>
#include "DTReservaHabitacion.hpp"

class DTRHPropuesta : public DTReservaHabitacion {
    private:
        float subtotal;
        TipoMoneda moneda;
        int camasMatrimoniales;
        int camasIndividuales;
        int cunas;

    public:
        // Constructor
        DTRHPropuesta(Fecha fechaInicio, Fecha fechaFin, std::set<AtributoHabitacion> atributosHabitacion, float subtotal, TipoMoneda moneda, int camasMatrimoniales, int camasIndividuales, int cunas);

        // Getter
        float getSubtotal() const;
        TipoMoneda getMoneda() const;
        int getCamasMatrimoniales() const;
        int getCamasIndividuales() const;
        int getCunas() const;

        bool operator==(const DTRHPropuesta& other) const;

        // Destructor
        ~DTRHPropuesta() {}
};

#endif