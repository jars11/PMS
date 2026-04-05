#ifndef UTILS_HPP
#define UTILS_HPP

#include <string>

// =======================
// Enumeraciones
// =======================

enum class EstadoReserva {
    PendienteDePago,
    SeniaPaga,
    PagoTotal,
    Finalizada
};

enum class TipoDocumento {
    DNI,
    PASAPORTE,
    CEDULA,
    OTRO
};

enum class TipoMoneda {
    UYU,
    USD
};

enum class AtributoHabitacion {
    Balcon,
    VistaAlMar,
    Jacuzzi,
    Twin,
    Cuna,
    ColchonExtra,
    Doble,
    Triple,
    Cuadruple,
    Quintuple,
    DosAmbientes
};

enum class TipoEvento {
    cumpleanios,
    aniversario,
    boda,
    cumple_xv,
    after_office,
    musica_en_vivo,
    charla,
    otro
};

// =======================
// to_string
// =======================

std::string to_string(EstadoReserva e);
std::string to_string(TipoDocumento t);
std::string to_string(TipoMoneda m);
std::string to_string(AtributoHabitacion a);
std::string to_string(TipoEvento e);

#endif // UTILS_HPP
