#ifndef PAGO_HPP
#define PAGO_HPP

#include "../utils.hpp"
#include "../dtos/Fecha.hpp"

class Reserva; // Declaración adelantada
class TitularReserva; // Declaración adelantada

class Pago {
    private:
        int id;
        float monto;
        TipoMoneda moneda;
        Fecha fecha;
        TitularReserva* titular;
        Reserva* reserva; // Asociación con Reserva
    
    public:
        // Constructor
        Pago(float monto, TipoMoneda moneda, Fecha fecha, TitularReserva* titular, Reserva* reserva);

        // Getters
        int getId() const;
        float getMonto() const;
        TipoMoneda getMoneda() const;
        Fecha getFecha() const;
        Reserva* getReserva() const;
        TitularReserva* getTitular() const;
};

#endif