#ifndef DTPAGO_HPP
#define DTPAGO_HPP

#include "../utils.hpp"
#include "Fecha.hpp"

class DTPago {
    private:
        float monto;
        TipoMoneda moneda;
        Fecha fechaPago;
    
    public:
        DTPago(float monto, TipoMoneda moneda, Fecha fechaPago);

        // Getters
        float getMonto() const;
        TipoMoneda getMoneda() const;
        Fecha getFechaPago() const;

        bool operator==(const DTPago& other) const;

        // Destructor
        ~DTPago();
};

#endif