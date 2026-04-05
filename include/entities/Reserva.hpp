#ifndef RESERVA_HPP
#define RESERVA_HPP

#include "../utils.hpp"
#include "TitularReserva.hpp"
#include "Pago.hpp"
#include "RHPropuesta.hpp"
#include <vector>

class Reserva {
    private:
        int id;
        bool activa;
        TipoMoneda moneda;
        float montoTotal;
        float montoPagado;
        float montoSenia;
        EstadoReserva estado;
        TitularReserva* titular;
        std::vector<Pago*> pagos;
        std::vector<RHPropuesta> rhpropuestas;
    
        ~Reserva();
    
    public:
        // Constructor
        Reserva(int id, TitularReserva* titular);

        // Getters
        int getId() const;
        bool isActiva() const;
        TipoMoneda getMoneda() const;
        float getMontoTotal() const;
        float getMontoPagado() const;
        float getMontoSenia() const;
        EstadoReserva getEstado() const;
        TitularReserva* getTitular() const;
        std::vector<Pago*> getPagos() const;
        std::vector<RHPropuesta> getRHPropuestas() const;

        // Setters
        void setActiva(bool activa);
        void addPago(const Pago& pago);
        void addRHPropuesta(const RHPropuesta& rhpropuesta);
        void removeRHPropuesta(int idPropuesta);

        // Destructor
        void eliminarReserva();
};

#endif