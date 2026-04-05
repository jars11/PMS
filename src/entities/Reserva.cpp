#include "../../include/entities/Reserva.hpp"
#include "../../include/utils.hpp"
#include <algorithm>


// Constructor
Reserva::Reserva(int id, TitularReserva* titular)
    : id(id), activa(false), moneda(TipoMoneda::USD), montoTotal(0.0f), montoPagado(0.0f), montoSenia(0.0f), estado(EstadoReserva::PendienteDePago), titular(titular), rhpropuestas({}) {}

// Getters
int Reserva::getId() const {
    return id;
}
bool Reserva::isActiva() const {
    return activa;
}
TipoMoneda Reserva::getMoneda() const {
    return moneda;
}
float Reserva::getMontoTotal() const {
    return montoTotal;
}
float Reserva::getMontoPagado() const {
    return montoPagado;
}
float Reserva::getMontoSenia() const {
    return montoSenia;
}
EstadoReserva Reserva::getEstado() const {
    return estado;
}
TitularReserva* Reserva::getTitular() const {
    return titular;
}
std::vector<Pago*> Reserva::getPagos() const {
    return pagos;
}
std::vector<RHPropuesta> Reserva::getRHPropuestas() const {
    return rhpropuestas;
}

// Setters
void Reserva::setActiva(bool activa) {
    this->activa = activa;
}
void Reserva::addPago(const Pago& pago) {
    pagos.push_back(new Pago(pago));
}
void Reserva::addRHPropuesta(const RHPropuesta& rhpropuesta) {
    rhpropuestas.push_back(rhpropuesta);
}
void Reserva::removeRHPropuesta(int idPropuesta) {
    rhpropuestas.erase(std::remove_if(rhpropuestas.begin(), rhpropuestas.end(),
        [idPropuesta](const RHPropuesta& propuesta) {
            return propuesta.getId() == idPropuesta;
        }), rhpropuestas.end());
}

// Destructor
void Reserva::eliminarReserva() {
    if (pagos.empty()) {
        rhpropuestas.clear();
        delete this;
    }
}

Reserva::~Reserva() {}