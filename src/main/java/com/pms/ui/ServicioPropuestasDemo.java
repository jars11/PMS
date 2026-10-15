package com.pms.ui;

import com.pms.dtos.DTRHPropuesta;
import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoMoneda;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lógica DEMO de propuesta; la asignación real (cadena de manejadores) se
 * integrará en una etapa posterior.
 *
 * <p>Servicio de demostración, reemplazable, que arma una propuesta de estadía
 * totalmente en memoria: no usa persistencia, no consulta disponibilidad real
 * (no toca {@code ControladorReservas}) y trabaja sobre un conjunto fijo de
 * habitaciones sembradas. Todo el resultado es determinístico.
 *
 * <p>Heurística DEMO documentada:
 * <ul>
 *   <li>Habitaciones sembradas (6): 101 1M+1I+Balcón ($1500), 102 1M+1I+Vista
 *       al mar+Jacuzzi ($2200), 103 1M+2I+Balcón ($1800), 104 1M+2I+Vista al
 *       mar ($2100), 201 2M+2I+Balcón ($2600), 202 2M+2I+Dos ambientes
 *       ($2400). M = cama matrimonial, I = cama individual.</li>
 *   <li>Capacidad de camas: cada habitación expone sus cantidades reales
 *       ({@code cantidadMatrimoniales} y {@code cantidadIndividuales}). Las
 *       cunas no se usan en esta heurística demo.</li>
 *   <li>Necesidades: los matrimonios requieren camas matrimoniales; los
 *       adultos separados y los niños requieren camas individuales.</li>
 *   <li>Preferencia de atributos (objetivo del sistema): se ordenan las
 *       habitaciones candidatas por (1) cantidad de atributos solicitados que
 *       poseen, descendente; (2) tarifa por noche, ascendente; (3) número,
 *       ascendente. Así se prefiere la habitación que cumple todos los
 *       atributos y, si no existe, la que cumple el subconjunto más grande.
 *       Una sola pasada sobre ese ranking: por cada habitación se toman las
 *       camas aún necesarias (primero matrimoniales, luego individuales) y se
 *       registra como asignada si aporta al menos una cama. Se detiene cuando
 *       las necesidades quedan cubiertas o no queda ninguna habitación que
 *       pueda aportar.</li>
 * </ul>
 */
public final class ServicioPropuestasDemo {

    /** Habitaciones fijas del demo, construidas en memoria (sin persistencia). */
    private static final List<Habitacion> HABITACIONES = sembrarHabitaciones();

    ServicioPropuestasDemo() {
    }

    /**
     * Genera la propuesta de estadía para las necesidades indicadas.
     *
     * @param checkIn            fecha de check-in, obligatoria y anterior a {@code checkOut}
     * @param checkOut           fecha de check-out, obligatoria
     * @param matrimonios        cantidad de matrimonios (≥ 0)
     * @param adultosSeparados   cantidad de adultos separados (≥ 0)
     * @param ninos              cantidad de niños (≥ 0)
     * @param atributosSolicitados atributos deseados; puede ser {@code null} (se trata como vacío)
     * @return la propuesta resultante, completa o con faltantes reportados
     * @throws IllegalArgumentException si las fechas o cantidades son inválidas
     */
    public ResultadoPropuesta generarPropuesta(LocalDate checkIn, LocalDate checkOut,
                                               int matrimonios, int adultosSeparados, int ninos,
                                               Set<AtributoHabitacion> atributosSolicitados) {
        validar(checkIn, checkOut, matrimonios, adultosSeparados, ninos);
        Set<AtributoHabitacion> solicitados = new HashSet<>(
                atributosSolicitados != null ? atributosSolicitados : Set.of());

        long noches = ChronoUnit.DAYS.between(checkIn, checkOut);
        int faltantesMatrimoniales = matrimonios;
        int faltantesIndividuales = adultosSeparados + ninos;

        List<Habitacion> candidatas = new ArrayList<>(HABITACIONES);
        candidatas.sort(comparadorCandidatas(solicitados));

        Set<PropuestaAsignada> asignaciones = new TreeSet<>(
                Comparator.comparingInt(a -> a.habitacion().getNumero()));
        Set<DTRHPropuesta> propuestas = new TreeSet<>();

        for (Habitacion habitacion : candidatas) {
            if (faltantesMatrimoniales == 0 && faltantesIndividuales == 0) {
                break;
            }
            int[] camas = camasDe(habitacion);
            int camasMatrimoniales = Math.min(faltantesMatrimoniales, camas[0]);
            int camasIndividuales = Math.min(faltantesIndividuales, camas[1]);
            if (camasMatrimoniales == 0 && camasIndividuales == 0) {
                continue; // no puede contribuir ninguna cama aún necesaria
            }
            faltantesMatrimoniales -= camasMatrimoniales;
            faltantesIndividuales -= camasIndividuales;
            float subtotal = noches * habitacion.getTarifaPorNoche();
            DTRHPropuesta propuesta = new DTRHPropuesta(checkIn, checkOut,
                    habitacion.getAtributos(), subtotal, TipoMoneda.USD,
                    camasMatrimoniales, camasIndividuales, 0);
            asignaciones.add(new PropuestaAsignada(habitacion, propuesta,
                    camasMatrimoniales, camasIndividuales, 0));
            propuestas.add(propuesta);
        }

        return new ResultadoPropuesta(propuestas, asignaciones,
                faltantesMatrimoniales, faltantesIndividuales);
    }

    /**
     * Valida la entrada de una solicitud. Rechaza fechas nulas, check-out no
     * posterior al check-in y cantidades negativas.
     *
     * @throws IllegalArgumentException con mensaje en español neutral si la entrada es inválida
     */
    public static void validar(LocalDate checkIn, LocalDate checkOut,
                               int matrimonios, int adultosSeparados, int ninos) {
        if (checkIn == null) {
            throw new IllegalArgumentException("La fecha de check-in es obligatoria.");
        }
        if (checkOut == null) {
            throw new IllegalArgumentException("La fecha de check-out es obligatoria.");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException(
                    "La fecha de check-out debe ser posterior a la fecha de check-in.");
        }
        if (matrimonios < 0 || adultosSeparados < 0 || ninos < 0) {
            throw new IllegalArgumentException(
                    "Las cantidades deben ser mayores o iguales a cero.");
        }
    }

    /**
     * Formatea un monto en USD con formato de número español (miles con punto,
     * decimales con coma), por ejemplo {@code USD 7.400,00}.
     */
    static String formatearMonto(float monto) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-UY"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "USD " + formato.format(monto);
    }

    private static List<Habitacion> sembrarHabitaciones() {
        List<Habitacion> habitaciones = new ArrayList<>();
        habitaciones.add(habitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f));
        habitaciones.add(habitacion(102, Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi), 1, 1, 0, 0, 2200.0f));
        habitaciones.add(habitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 0, 1800.0f));
        habitaciones.add(habitacion(104, Set.of(AtributoHabitacion.VistaAlMar), 1, 2, 0, 0, 2100.0f));
        habitaciones.add(habitacion(201, Set.of(AtributoHabitacion.Balcon), 2, 2, 0, 0, 2600.0f));
        habitaciones.add(habitacion(202, Set.of(AtributoHabitacion.DosAmbientes), 2, 2, 0, 0, 2400.0f));
        return List.copyOf(habitaciones);
    }

    private static Habitacion habitacion(int numero, Set<AtributoHabitacion> atributos,
                                         int cantidadMatrimoniales, int cantidadIndividuales,
                                         int cantidadCunas, int colchones, float tarifa) {
        Habitacion habitacion = new Habitacion(numero, atributos, cantidadMatrimoniales,
                cantidadIndividuales, cantidadCunas, colchones);
        habitacion.setTarifaPorNoche(tarifa);
        return habitacion;
    }

    /** Camas de una habitación: [matrimoniales, individuales] reales. */
    private static int[] camasDe(Habitacion habitacion) {
        return new int[]{habitacion.getCantidadMatrimoniales(), habitacion.getCantidadIndividuales()};
    }

    private static Comparator<Habitacion> comparadorCandidatas(Set<AtributoHabitacion> solicitados) {
        return Comparator
                .comparingInt((Habitacion h) -> coincidencias(h, solicitados)).reversed()
                .thenComparingDouble(Habitacion::getTarifaPorNoche)
                .thenComparingInt(Habitacion::getNumero);
    }

    private static int coincidencias(Habitacion habitacion, Set<AtributoHabitacion> solicitados) {
        int cantidad = 0;
        for (AtributoHabitacion atributo : habitacion.getAtributos()) {
            if (solicitados.contains(atributo)) {
                cantidad++;
            }
        }
        return cantidad;
    }

    /** Una habitación asignada junto con su propuesta y las camas aportadas. */
    public record PropuestaAsignada(Habitacion habitacion, DTRHPropuesta propuesta,
                                    int camasMatrimoniales, int camasIndividuales, int cunas) {
    }

    /** Resultado de una propuesta: asignaciones, faltantes y total. */
    public static final class ResultadoPropuesta {

        /** Propuestas ordenadas según el orden natural de {@link DTRHPropuesta}. */
        private final Set<DTRHPropuesta> propuestas;
        /** Asignaciones ordenadas por número de habitación. */
        private final Set<PropuestaAsignada> asignaciones;
        private final int camasMatrimonialesFaltantes;
        private final int camasIndividualesFaltantes;

        private ResultadoPropuesta(Set<DTRHPropuesta> propuestas,
                                   Set<PropuestaAsignada> asignaciones,
                                   int camasMatrimonialesFaltantes,
                                   int camasIndividualesFaltantes) {
            this.propuestas = new TreeSet<>(propuestas);
            this.asignaciones = new TreeSet<>(Comparator.comparingInt(a -> a.habitacion().getNumero()));
            this.asignaciones.addAll(asignaciones);
            this.camasMatrimonialesFaltantes = camasMatrimonialesFaltantes;
            this.camasIndividualesFaltantes = camasIndividualesFaltantes;
        }

        public Set<DTRHPropuesta> getPropuestas() {
            return propuestas;
        }

        public Set<PropuestaAsignada> getAsignaciones() {
            return asignaciones;
        }

        public int getCamasMatrimonialesFaltantes() {
            return camasMatrimonialesFaltantes;
        }

        public int getCamasIndividualesFaltantes() {
            return camasIndividualesFaltantes;
        }

        /** {@code true} cuando no faltan camas de ningún tipo. */
        public boolean esCompleta() {
            return camasMatrimonialesFaltantes == 0 && camasIndividualesFaltantes == 0;
        }

        /** Suma de los subtotales de todas las propuestas. */
        public float getTotal() {
            float total = 0.0f;
            for (DTRHPropuesta propuesta : propuestas) {
                total += propuesta.getSubtotal();
            }
            return total;
        }

        /** Resumen en español neutral indicando cobertura o faltantes. */
        public String resumen() {
            if (esCompleta()) {
                int cantidad = asignaciones.size();
                String habitaciones = cantidad == 1 ? "habitacion" : "habitaciones";
                return "Propuesta completa: " + cantidad + " " + habitaciones
                        + ", total " + ServicioPropuestasDemo.formatearMonto(getTotal());
            }
            StringBuilder resumen = new StringBuilder("Propuesta incompleta: faltan ");
            if (camasMatrimonialesFaltantes > 0) {
                resumen.append(camasMatrimonialesFaltantes)
                        .append(camasMatrimonialesFaltantes == 1 ? " cama matrimonial" : " camas matrimoniales");
            }
            if (camasIndividualesFaltantes > 0) {
                if (camasMatrimonialesFaltantes > 0) {
                    resumen.append(" y ");
                }
                resumen.append(camasIndividualesFaltantes)
                        .append(camasIndividualesFaltantes == 1 ? " cama individual" : " camas individuales");
            }
            return resumen.append('.').toString();
        }

        @Override
        public String toString() {
            return resumen();
        }
    }
}