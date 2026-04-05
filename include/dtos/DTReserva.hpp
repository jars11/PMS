#ifndef DTRESERVA_HPP
#define DTRESERVA_HPP

#include <set>
#include "DTRHPropuesta.hpp"

class DTReserva {
    private:
        bool activa;
        float montoTotal;
        float montoSenia;
        TipoMoneda moneda;
        EstadoReserva estado;
        std::set<DTRHPropuesta> dtrhpropuestas;

    public:
        // Constructor
        DTReserva(bool activa, float montoTotal, float montoSenia, TipoMoneda moneda, EstadoReserva estado, std::set<DTRHPropuesta> dtrhpropuestas)
            : activa(activa), montoTotal(montoTotal), montoSenia(montoSenia), moneda(moneda), estado(estado), dtrhpropuestas(dtrhpropuestas) {}

        // Getters
        bool isActiva() const;
        float getMontoTotal() const;
        float getMontoSenia() const;
        TipoMoneda getMoneda() const;
        EstadoReserva getEstado() const;
        std::set<DTRHPropuesta> getDTRHPropuestas() const;

        bool operator==(const DTReserva& other) const;

        // Destructor
        ~DTReserva() {}
};

#endif