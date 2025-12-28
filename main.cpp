#include <iostream>
#include <string>
#include <vector>
#include <map>

struct Fecha {
    int dia;
    int mes;
    int anio;
};

bool operator<(const Fecha& a, const Fecha& b) {
    if (a.anio != b.anio) return a.anio < b.anio;
    if (a.mes  != b.mes)  return a.mes  < b.mes;
    return a.dia < b.dia;
}

bool operator<=(const Fecha& a, const Fecha& b) {
    return !(b < a);
}

int edadEn(const Fecha& nac, const Fecha& hoy) {
    int e = hoy.anio - nac.anio;
    if (hoy.mes < nac.mes || (hoy.mes == nac.mes && hoy.dia < nac.dia))
        e--;
    return e;
}

class Pasajero {
public:
    std::string nombre;
    std::string ci;
    Fecha nacimiento;

    Pasajero(std::string n, std::string c, Fecha f)
        : nombre(std::move(n)), ci(std::move(c)), nacimiento(f) {}
};

class Habitacion {
public:
    int numero;
    std::vector<std::string> atributos;

    Habitacion(int n, std::vector<std::string> attrs)
        : numero(n), atributos(std::move(attrs)) {}
};

class Titular {
public:
    std::string nombre;
    std::string telefono;

    Titular(std::string n, std::string t)
        : nombre(std::move(n)), telefono(std::move(t)) {}
};

class Pago {
public:
    double monto;
    // numero_habitacion -> parte del monto
    std::map<int, double> distribucion;

    Pago(double m, std::map<int,double> dist)
        : monto(m), distribucion(std::move(dist)) {}
};

class Reserva {
public:
    Fecha entrada;
    Fecha salida;
    Habitacion* habitacion;          // se asume que la maneja otro lado
    std::vector<Pasajero> pasajeros;
    Titular titular;
    double total;
    double senia;
    Pago* pago; // Cambiado de std::optional<Pago> a un puntero

    Reserva(Fecha fe, Fecha fs,
            Habitacion* h,
            std::vector<Pasajero> ps,
            Titular t,
            double tot,
            double s = 0.0,
            Pago* p = nullptr) // Ajustado el constructor para usar puntero
        : entrada(fe), salida(fs), habitacion(h),
          pasajeros(std::move(ps)), titular(std::move(t)),
          total(tot), senia(s), pago(p) {}

    bool periodoIncluye(const Fecha& f) const {
        return entrada <= f && f <= salida;
    }

    bool intersecta(const Fecha& desde, const Fecha& hasta) const {
        return !(salida < desde || hasta < entrada);
    }
};

class ControladorReservas {
public:
    std::vector<Reserva> reservas;

    void agregar(const Reserva& r) {
        reservas.push_back(r);
    }

    std::vector<const Reserva*> reservasEntre(const Fecha& desde,
                                              const Fecha& hasta) const {
        std::vector<const Reserva*> res;
        for (const auto& r : reservas) {
            if (r.intersecta(desde, hasta)) res.push_back(&r);
        }
        return res;
    }
};

class ControladorPasajeros {
public:
    const std::vector<Reserva>* reservasRef = nullptr;

    void setReservas(const std::vector<Reserva>& rs) {
        reservasRef = &rs;
    }

    std::vector<const Pasajero*> pasajerosPorEdad(int minEdad,
                                                  int maxEdad,
                                                  const Fecha& hoy) const {
        std::vector<const Pasajero*> res;
        if (!reservasRef) return res;

        for (const auto& r : *reservasRef) {
            for (const auto& p : r.pasajeros) {
                int e = edadEn(p.nacimiento, hoy);
                if (e >= minEdad && e <= maxEdad)
                    res.push_back(&p);
            }
        }
        return res;
    }
};

class ControladorHabitaciones {
public:
    std::vector<Habitacion> habitaciones;

    explicit ControladorHabitaciones(std::vector<Habitacion> hs)
        : habitaciones(std::move(hs)) {}

    std::vector<const Habitacion*> buscarPorAtributos(
            const std::vector<std::string>& attrs) const {

        std::vector<const Habitacion*> res;
        for (const auto& h : habitaciones) {
            bool ok = true;
            for (const auto& a : attrs) {
                bool tiene = false;
                for (const auto& ha : h.atributos)
                    if (ha == a) { tiene = true; break; }
                if (!tiene) { ok = false; break; }
            }
            if (ok) res.push_back(&h);
        }
        return res;
    }
};

// Ejemplo mínimo de uso
int main() {
    Habitacion h101(101, {"matrimonial", "balcon"});
    Habitacion h102(102, {"twin"});

    ControladorHabitaciones ctrlHab({h101, h102});

    Titular tit("Agustin Rivero", "091000000");
    Pasajero p1("Juan Perez", "5.123.456-7", {10, 3, 1995});
    Pasajero p2("Ana Lopez", "4.987.654-3", {5, 7, 2010});

    Pago pago1(3000.0, {{101, 3000.0}});
    Reserva r1({10,1,2026}, {15,1,2026}, &ctrlHab.habitaciones[0],
               {p1, p2}, tit, 3000.0, 500.0, &pago1);

    ControladorReservas ctrlRes;
    ctrlRes.agregar(r1);

    ControladorPasajeros ctrlPas;
    ctrlPas.setReservas(ctrlRes.reservas);

    Fecha hoy{1,1,2026};
    auto menores = ctrlPas.pasajerosPorEdad(0, 17, hoy);

    std::cout << "Menores hospedados:\n";
    for (auto* p : menores) {
        std::cout << " - " << p->nombre << "\n";
    }

    auto conBalcon = ctrlHab.buscarPorAtributos({"balcon"});
    std::cout << "Habitaciones con balcon:\n";
    for (auto* h : conBalcon) {
        std::cout << " - " << h->numero << "\n";
    }

    return 0;
}
