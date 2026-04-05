#ifndef RHPROPUESTA_HPP
#define RHPROPUESTA_HPP

#include <set>
#include "../utils.hpp"
#include "ReservaHabitacion.hpp"
#include "Habitacion.hpp"

class RHSolicitada; // Declaración adelantada

class RHPropuesta : public ReservaHabitacion {
    private:
        int id;
        float subtotal;
        TipoMoneda moneda;
        int camasMatrimoniales;
        int camasIndividuales;
        int cunas;
        Habitacion* habitacion;
        std::set<RHSolicitada*> rhsolicitadasAsociadas;
    public:
        // Constructor
        RHPropuesta(const Fecha& fechaInicio, const Fecha& fechaFin, const std::set<AtributoHabitacion>& atributosHabitacion,
                     float subtotal, TipoMoneda moneda, int camasMatrimoniales, int camasIndividuales, int cunas);

        // Getters
        int getId() const;
        float getSubtotal() const;
        TipoMoneda getMoneda() const;
        int getCamasMatrimoniales() const;
        int getCamasIndividuales() const;
        int getCunas() const;
        Habitacion* getHabitacion() const;
        std::set<RHSolicitada*> getRHSolicitadasAsociadas() const;

        // Setters
        void setHabitacion(Habitacion* habitacion);

        // Destructor
        ~RHPropuesta();
};

#endif