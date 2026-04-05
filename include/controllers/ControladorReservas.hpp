#ifndef CONTROLADORRESERVAS_HPP
#define CONTROLADORRESERVAS_HPP

#include <string>
#include <set>
#include "../dtos/Fecha.hpp"
#include "../dtos/DTReserva.hpp"
#include "../dtos/DTPago.hpp"
#include "../dtos/DTRHPropuesta.hpp"
#include "../dtos/DTRHSolicitada.hpp"

class ControladorReservas {
    public:
        static ControladorReservas& getInstance() {
            static ControladorReservas instance;
            return instance;
        }

        // Delete copy constructor and assignment operator to prevent copies
        ControladorReservas(const ControladorReservas&) = delete;
        ControladorReservas& operator=(const ControladorReservas&) = delete;

        DTReserva obtenerDisponibilidad(std::set<DTRHSolicitada> Dtrhs);
        void cancelarReserva(int reservaId);
        std::set<DTReserva> mostrarReservas();
        std::set<DTReserva> mostrarReservasPorTitular(int titularId);
        std::set<DTPago> mostrarPagos(int reservaId);
        std::set<DTRHPropuesta> mostrarRHPropuestasPorCheckin(Fecha checkin);
        std::set<DTRHPropuesta> mostrarRHPropuestasPorCheckout(Fecha checkout);
        std::set<DTRHPropuesta> mostrarRHPropuestasPorRangoFechasEnEstadia(Fecha desde, Fecha hasta);

    private:
        std::set<RHSolicitada*> rhsolicitadas;
        std::set<Reserva*> reservas;

        ManejadorCapacidad* manejadorCapacidad;
        ManejadorDistribucionHabs* manejadorDistribucionHabs;
        ManejadorDisponibilidad* manejadorDisponibilidad;
        ManejadorDistribucionCamas* manejadorDistribucionCamas;

        ControladorReservas() = default;
        ~ControladorReservas() = default;
};

#endif