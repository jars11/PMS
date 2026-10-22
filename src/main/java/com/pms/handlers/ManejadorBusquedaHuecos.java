package com.pms.handlers;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.Fechas;
import com.pms.utils.TipoMoneda;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Búsqueda de huecos: arma conjuntos de propuestas de habitaciones que cubren
 * todas las solicitudes pedidas, degradando lo menos importante primero y
 * preguntando al usuario antes de cada relajación.
 *
 * <p>El pseudocódigo de referencia está en
 * {@code Pseudocodigo busqueda de lugares (prolijo).txt}. Los números de sección
 * del código siguen los del pseudocódigo.
 *
 * <p>El algoritmo nunca hace I/O directo: pregunta y notifica a través de
 * {@link InteraccionUsuario}.
 */
public class ManejadorBusquedaHuecos {

    private static final int DIAS_VENTANA_POR_DEFECTO = 15;

    // Cota de extensión al buscar el borde libre ("libre sin límite" se aproxima).
    private static final int MAX_EXTENSION_DIAS = 30;

    /** Criterio sobre (solicitud, habitación) usado por el backtracking conjunto. */
    @FunctionalInterface
    private interface Criterio {
        boolean acepta(DTRHSolicitada solicitud, Habitacion habitacion);
    }

    /** Una alternativa completa junto con la penalidad de cada propuesta (alineada con las solicitudes). */
    private record Alternativa(Set<RHPropuesta> conjunto, List<List<Integer>> penalidades) {

        /** Costo total de la alternativa: suma de subtotales de sus propuestas. */
        double totalSubtotal() {
            double total = 0.0;
            for (RHPropuesta p : conjunto) {
                total += p.getSubtotal();
            }
            return total;
        }

        /** Clave deterministica por numeros de habitacion, para desempatar. */
        String claveHabitaciones() {
            List<String> numeros = new ArrayList<>();
            for (RHPropuesta p : conjunto) {
                String numero = p.getHabitacion() == null
                        || p.getHabitacion().getNumero() == null
                        ? "?" : String.format("%04d", p.getHabitacion().getNumero());
                numeros.add(numero);
            }
            numeros.sort(String::compareTo);
            return String.join(",", numeros);
        }
    }

    // =========================================================================
    // API
    // =========================================================================

    /** Sobrecarga con los valores por defecto del pseudocódigo. */
    public Set<Set<RHPropuesta>> buscarHuecos(Collection<DTRHSolicitada> solicitudes, InteraccionUsuario interaccion) {
        return buscarHuecos(solicitudes, interaccion, false, 0, false, false, DIAS_VENTANA_POR_DEFECTO);
    }

    public Set<Set<RHPropuesta>> buscarHuecos(Collection<DTRHSolicitada> solicitudes,
                                              InteraccionUsuario interaccion,
                                              boolean permitirColchonesExtra,
                                              int relajacionDistribucion,
                                              boolean atributosRelajados,
                                              boolean fechasRelajadas,
                                              int diasVentana) {
        if (solicitudes == null || solicitudes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<DTRHSolicitada> sols = new ArrayList<>(solicitudes);
        sols.sort(Comparator.comparing(DTRHSolicitada::getFechaInicio));

        // 0. Fechas relajadas: mapa de disponibilidad, el usuario decide qué reservar
        if (fechasRelajadas) {
            return mapaDisponibilidad(sols, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
        }

        LocalDate min = sols.stream().map(DTRHSolicitada::getFechaInicio).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = sols.stream().map(DTRHSolicitada::getFechaFin).max(LocalDate::compareTo).orElseThrow();
        Map<LocalDate, Set<Habitacion>> libres = nochesLibres(min, max);

        // 1. Capacidad total por noche
        for (LocalDate n = min; n.isBefore(max); n = n.plusDays(1)) {
            int personas = personasEnNoche(sols, n);
            int capacidad = 0;
            int capacidadConColchones = 0;
            for (Habitacion h : libres.getOrDefault(n, Set.of())) {
                capacidad += h.capacidad() + (permitirColchonesExtra ? h.getColchones() : 0);
                capacidadConColchones += h.capacidad() + h.getColchones();
            }
            if (personas > capacidad) {
                if (!permitirColchonesExtra && personas <= capacidadConColchones) {
                    final int dist = relajacionDistribucion;
                    final boolean attr = atributosRelajados;
                    return ofrecerRelajacion(interaccion,
                            "¿Buscar con colchones extra?",
                            () -> buscarHuecos(solicitudes, interaccion, true, dist, attr, false, diasVentana),
                            solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
                }
                return ofrecerOtrasFechas(interaccion, solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
            }
        }

        // 2. Capacidad por habitación
        if (relajacionDistribucion < 2) {
            Criterio porCapacidad = (s, h) -> capacidadEfectiva(h, permitirColchonesExtra) >= personas(s);
            if (!alcanza(sols, porCapacidad, libres)) {
                if (!permitirColchonesExtra) {
                    Criterio conColchones = (s, h) -> capacidadEfectiva(h, true) >= personas(s);
                    if (alcanza(sols, conColchones, libres)) {
                        final int dist = relajacionDistribucion;
                        final boolean attr = atributosRelajados;
                        return ofrecerRelajacion(interaccion,
                                "¿Agregar colchones extra en las habitaciones que los admiten?",
                                () -> buscarHuecos(solicitudes, interaccion, true, dist, attr, false, diasVentana),
                                solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
                    }
                }
                final boolean extra = permitirColchonesExtra;
                final boolean attr = atributosRelajados;
                return ofrecerRelajacion(interaccion,
                        "¿Alojar al total de personas con otra cantidad por habitación?",
                        () -> buscarHuecos(solicitudes, interaccion, extra, 2, attr, false, diasVentana),
                        solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
            }
        } else {
            interaccion.notificar("La distribución de personas no es la deseada.");
        }

        // 3. Distribución de camas por habitación
        if (relajacionDistribucion == 0) {
            Criterio porCamas = (s, h) -> camasAlcanzan(s, h, permitirColchonesExtra);
            if (!alcanza(sols, porCamas, libres)) {
                final boolean extra = permitirColchonesExtra;
                final boolean attr = atributosRelajados;
                // Un solo cartel: el aviso y la pregunta en la misma ventana.
                return ofrecerRelajacion(interaccion,
                        "No se encontró habitación",
                        "No se encontró habitación para la fecha solicitada con la "
                                + "distribución de camas pedida.\n\n"
                                + "¿Buscar habitaciones con la capacidad pedida pero otra "
                                + "distribución de camas?",
                        () -> buscarHuecos(solicitudes, interaccion, extra, 1, attr, false, diasVentana),
                        solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
            }
        }
        if (relajacionDistribucion == 1) {
            interaccion.notificar("Distribución de camas no deseada.");
        }

        // 4. Atributos de las habitaciones
        Map<DTRHSolicitada, Set<Set<AtributoHabitacion>>> maximalesPorSolicitud = new LinkedHashMap<>();
        if (relajacionDistribucion < 2) {
            if (!atributosRelajados) {
                Criterio porDistribucion = distribucionCriterio(permitirColchonesExtra, relajacionDistribucion);
                Criterio porAtributos = (s, h) -> porDistribucion.acepta(s, h)
                        && h.getAtributos().containsAll(s.getAtributosHabitacion());
                if (!alcanza(sols, porAtributos, libres)) {
                    final boolean extra = permitirColchonesExtra;
                    final int dist = relajacionDistribucion;
                    return ofrecerRelajacion(interaccion,
                            "¿Buscar habitaciones que cumplan la mayor cantidad posible de los atributos pedidos?",
                            () -> buscarHuecos(solicitudes, interaccion, extra, dist, true, false, diasVentana),
                            solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
                }
            } else {
                // Se aceptan subconjuntos maximales de los atributos de cada solicitud
                for (DTRHSolicitada s : sols) {
                    Set<Set<AtributoHabitacion>> maximales =
                            subconjuntosMaximales(s, sols, permitirColchonesExtra, relajacionDistribucion, libres);
                    maximalesPorSolicitud.put(s, maximales);
                    notificarFaltantes(interaccion, s, maximales);
                }
            }
        } else if (!atributosRelajados) {
            // Nivel 2 sin relajar atributos: los atributos deben entrar dentro del
            // mínimo de habitaciones que cubre la capacidad (interpretación 1).
            if (!puedeCubrirAtributosNivel2(sols, libres, permitirColchonesExtra)) {
                final boolean extra = permitirColchonesExtra;
                return ofrecerRelajacion(interaccion,
                        "¿Buscar habitaciones que cumplan la mayor cantidad posible de los atributos pedidos?",
                        () -> buscarHuecos(solicitudes, interaccion, extra, 2, true, false, diasVentana),
                        solicitudes, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, diasVentana);
            }
        }

        // 5. Armado del Set<Set<RHPropuesta>>
        if (relajacionDistribucion < 2) {
            Criterio criterioBuild = criterioArmado(permitirColchonesExtra, relajacionDistribucion, atributosRelajados, maximalesPorSolicitud);
            List<List<Habitacion>> asignaciones = new ArrayList<>();
            asignarTodas(sols, 0, criterioBuild, libres, new HashMap<>(), new ArrayList<>(), asignaciones);
            return materializar(sols, asignaciones, permitirColchonesExtra, relajacionDistribucion);
        }
        return nivel2(sols, libres, permitirColchonesExtra, atributosRelajados, interaccion);
    }

    // =========================================================================
    // Auxiliar: ofrecerRelajacion / ofrecerOtrasFechas
    // =========================================================================

    private Set<Set<RHPropuesta>> ofrecerRelajacion(InteraccionUsuario interaccion,
                                                    String pregunta,
                                                    java.util.function.Supplier<Set<Set<RHPropuesta>>> relajada,
                                                    Collection<DTRHSolicitada> solicitudes,
                                                    boolean permitirColchonesExtra,
                                                    int relajacionDistribucion,
                                                    boolean atributosRelajados,
                                                    int diasVentana) {
        return ofrecerRelajacion(interaccion, null, pregunta, relajada, solicitudes,
                permitirColchonesExtra, relajacionDistribucion, atributosRelajados,
                diasVentana);
    }

    private Set<Set<RHPropuesta>> ofrecerRelajacion(InteraccionUsuario interaccion,
                                                    String titulo,
                                                    String pregunta,
                                                    java.util.function.Supplier<Set<Set<RHPropuesta>>> relajada,
                                                    Collection<DTRHSolicitada> solicitudes,
                                                    boolean permitirColchonesExtra,
                                                    int relajacionDistribucion,
                                                    boolean atributosRelajados,
                                                    int diasVentana) {
        boolean acepta = titulo == null
                ? interaccion.confirmar(pregunta)
                : interaccion.confirmar(titulo, pregunta);
        if (acepta) {
            return relajada.get();
        }
        return ofrecerOtrasFechas(interaccion, solicitudes, permitirColchonesExtra,
                relajacionDistribucion, atributosRelajados, diasVentana);
    }

    private Set<Set<RHPropuesta>> ofrecerOtrasFechas(InteraccionUsuario interaccion,
                                                     Collection<DTRHSolicitada> solicitudes,
                                                     boolean permitirColchonesExtra,
                                                     int relajacionDistribucion,
                                                     boolean atributosRelajados,
                                                     int diasVentana) {
        String pregunta = "¿Buscar en otras fechas (±" + diasVentana + " días) con las restricciones actuales?";
        if (interaccion.confirmar(pregunta)) {
            return buscarHuecos(solicitudes, interaccion, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, true, diasVentana);
        }
        return new LinkedHashSet<>();
    }

    // =========================================================================
    // 0. Fechas relajadas
    // =========================================================================

    /**
     * Devuelve la disponibilidad dentro de [min - diasVentana, max + diasVentana] sin forzar
     * la duración pedida. La agrupa por tipo pedido: cada conjunto interno son las propuestas
     * disponibles de un mismo tipo, ordenadas por fecha y penalidad.
     */
    private Set<Set<RHPropuesta>> mapaDisponibilidad(List<DTRHSolicitada> sols,
                                                     boolean permitirColchonesExtra,
                                                     int relajacionDistribucion,
                                                     boolean atributosRelajados,
                                                     int diasVentana) {
        LocalDate min = sols.stream().map(DTRHSolicitada::getFechaInicio).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = sols.stream().map(DTRHSolicitada::getFechaFin).max(LocalDate::compareTo).orElseThrow();
        LocalDate inicioVentana = min.minusDays(diasVentana);
        LocalDate finVentana = max.plusDays(diasVentana);

        // Agrupar solicitudes por tipo pedido (mismos valores)
        Map<DTRHSolicitada, List<DTRHSolicitada>> porTipo = new LinkedHashMap<>();
        for (DTRHSolicitada s : sols) {
            porTipo.computeIfAbsent(s, k -> new ArrayList<>()).add(s);
        }

        Set<Set<RHPropuesta>> resultado = new LinkedHashSet<>();
        for (DTRHSolicitada tipo : porTipo.keySet()) {
            Set<RHPropuesta> grupo = new LinkedHashSet<>();
            Criterio aceptable = aceptableParaFechas(tipo, permitirColchonesExtra, relajacionDistribucion, atributosRelajados, sols);
            for (Habitacion h : ControladorHabitaciones.getInstance().getHabitaciones()) {
                if (!aceptable.acepta(tipo, h)) {
                    continue;
                }
                for (LocalDate[] rango : rangosLibres(h, inicioVentana, finVentana)) {
                    LocalDate desde = rango[0];
                    LocalDate hasta = rango[1];
                    if (desde.equals(inicioVentana)) {
                        desde = extenderAtras(h, desde);
                    }
                    if (hasta.equals(finVentana)) {
                        hasta = extenderAdelante(h, hasta);
                    }
                    RHPropuesta propuesta = propuestaRango(h, desde, hasta, permitirColchonesExtra,
                            tipo.getCantMatrimonios(), tipo.getCantIndividuales(),
                            tipo.getCantBebes(), tipo.getCantCunas());
                    grupo.add(propuesta);
                }
            }
            if (!grupo.isEmpty()) {
                resultado.add(grupo);
            }
        }
        return resultado;
    }

    private Criterio aceptableParaFechas(DTRHSolicitada tipo,
                                         boolean permitirColchonesExtra,
                                         int relajacionDistribucion,
                                         boolean atributosRelajados,
                                         List<DTRHSolicitada> sols) {
        Criterio dist = distribucionCriterio(permitirColchonesExtra, relajacionDistribucion);
        return (s, h) -> {
            if (!dist.acepta(s, h)) {
                return false;
            }
            if (!atributosRelajados) {
                return h.getAtributos().containsAll(s.getAtributosHabitacion());
            }
            // Relajados: se acepta cualquier solapamiento no vacío de atributos
            Set<AtributoHabitacion> cubiertos = new TreeSet<>(h.getAtributos());
            cubiertos.retainAll(s.getAtributosHabitacion());
            return !cubiertos.isEmpty() || s.getAtributosHabitacion().isEmpty();
        };
    }

    private List<LocalDate[]> rangosLibres(Habitacion h, LocalDate desde, LocalDate hasta) {
        List<LocalDate[]> rangos = new ArrayList<>();
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        LocalDate inicioRango = null;
        for (LocalDate n = desde; !n.isAfter(hasta); n = n.plusDays(1)) {
            boolean libre = ch.habitacionLibreEnRango(h, n, n.plusDays(1));
            if (libre) {
                if (inicioRango == null) {
                    inicioRango = n;
                }
            } else if (inicioRango != null) {
                rangos.add(new LocalDate[]{inicioRango, n});
                inicioRango = null;
            }
        }
        if (inicioRango != null) {
            rangos.add(new LocalDate[]{inicioRango, hasta});
        }
        return rangos;
    }

    private LocalDate extenderAtras(Habitacion h, LocalDate desde) {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        LocalDate n = desde.minusDays(1);
        int limite = 0;
        while (ch.habitacionLibreEnRango(h, n, n.plusDays(1)) && limite < MAX_EXTENSION_DIAS) {
            n = n.minusDays(1);
            limite++;
        }
        return n.plusDays(1);
    }

    private LocalDate extenderAdelante(Habitacion h, LocalDate hasta) {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        LocalDate n = hasta;
        int limite = 0;
        while (ch.habitacionLibreEnRango(h, n, n.plusDays(1)) && limite < MAX_EXTENSION_DIAS) {
            n = n.plusDays(1);
            limite++;
        }
        return n;
    }

    // =========================================================================
    // 5. Armado (niveles 0 y 1)
    // =========================================================================

    private Set<Set<RHPropuesta>> materializar(List<DTRHSolicitada> sols,
                                               List<List<Habitacion>> asignaciones,
                                               boolean permitirColchonesExtra,
                                               int relajacionDistribucion) {
        // Deduplicar por (configuración, fechas) y quedarse con la primera
        Map<String, Alternativa> unicas = new LinkedHashMap<>();
        for (List<Habitacion> asig : asignaciones) {
            StringBuilder clave = new StringBuilder();
            Set<RHPropuesta> conjunto = new LinkedHashSet<>();
            List<List<Integer>> penalidades = new ArrayList<>();
            for (int i = 0; i < sols.size(); i++) {
                DTRHSolicitada s = sols.get(i);
                Habitacion h = asig.get(i);
                conjunto.add(propuesta(s, h, permitirColchonesExtra));
                penalidades.add(penalidad(s, h, permitirColchonesExtra, relajacionDistribucion, s.getAtributosHabitacion()));
                clave.append(h.getNumero()).append(':')
                        .append(configuracionDe(h)).append('@')
                        .append(s.getFechaInicio()).append("..").append(s.getFechaFin()).append(';');
            }
            unicas.putIfAbsent(clave.toString(), new Alternativa(conjunto, penalidades));
        }

        // El precio manda SIEMPRE: se elige la alternativa de menor suma de
        // subtotales, aunque tenga capacidad/atributos de sobra. Las penalidades
        // y los numeros de habitacion solo desempatan de forma deterministica.
        Comparator<Alternativa> porCosto = Comparator
                .comparingDouble(Alternativa::totalSubtotal)
                .thenComparingInt(a -> sumaPenalidad(a.penalidades()))
                .thenComparing(Alternativa::claveHabitaciones);
        Alternativa elegida = unicas.values().stream().min(porCosto).orElse(null);

        Set<Set<RHPropuesta>> resultado = new LinkedHashSet<>();
        if (elegida != null) {
            resultado.add(elegida.conjunto());
        }
        return resultado;
    }

    private int sumaPenalidad(List<List<Integer>> penalidades) {
        int suma = 0;
        for (List<Integer> p : penalidades) {
            for (int v : p) {
                suma += v;
            }
        }
        return suma;
    }

    // =========================================================================
    // 5b. Nivel 2: reparto por noche (sin correspondencia por habitación)
    // =========================================================================

    /**
     * Nivel 2: reparto por noche sin correspondencia habitación-solicitud.
     *
     * <p>Interpretación 1: la cantidad de habitaciones la fija la CAPACIDAD (el
     * mínimo que la cubre) y los atributos deben entrar dentro de ese mínimo. Así,
     * cada solución cubre la capacidad (y algunos atributos), y el conjunto de
     * soluciones cubre todos los atributos posibles. Sin relajar atributos hay una
     * sola solución; con atributos relajados, una por cada cobertura maximal.
     */
    private Set<Set<RHPropuesta>> nivel2(List<DTRHSolicitada> sols,
                                         Map<LocalDate, Set<Habitacion>> libres,
                                         boolean permitirColchonesExtra,
                                         boolean atributosRelajados,
                                         InteraccionUsuario interaccion) {
        LocalDate desde = sols.stream().map(DTRHSolicitada::getFechaInicio).min(LocalDate::compareTo).orElseThrow();
        LocalDate hasta = sols.stream().map(DTRHSolicitada::getFechaFin).max(LocalDate::compareTo).orElseThrow();

        Set<Set<RHPropuesta>> resultado = new LinkedHashSet<>();

        if (!atributosRelajados) {
            Set<RHPropuesta> conjunto = construirNivel2(sols, libres, permitirColchonesExtra, null, desde, hasta);
            if (!conjunto.isEmpty()) {
                resultado.add(conjunto);
            }
            return resultado;
        }

        Set<AtributoHabitacion> union = new TreeSet<>();
        for (DTRHSolicitada s : sols) {
            union.addAll(s.getAtributosHabitacion());
        }
        for (Set<AtributoHabitacion> b : subconjuntosMaximalesNivel2(union, sols, libres, permitirColchonesExtra, desde, hasta)) {
            Set<RHPropuesta> conjunto = construirNivel2(sols, libres, permitirColchonesExtra, b, desde, hasta);
            if (!conjunto.isEmpty()) {
                resultado.add(conjunto);
                Set<AtributoHabitacion> faltantes = new TreeSet<>(union);
                faltantes.removeAll(b);
                if (!faltantes.isEmpty()) {
                    interaccion.notificar("Faltan atributos por cumplir: " + faltantes);
                }
            }
        }
        if (resultado.isEmpty()) {
            Set<RHPropuesta> conjunto = construirNivel2(sols, libres, permitirColchonesExtra, Set.of(), desde, hasta);
            if (!conjunto.isEmpty()) {
                resultado.add(conjunto);
            }
        }
        return resultado;
    }

    /** ¿Los atributos pedidos entran en el mínimo de habitaciones que cubre la capacidad? */
    private boolean puedeCubrirAtributosNivel2(List<DTRHSolicitada> sols,
                                               Map<LocalDate, Set<Habitacion>> libres,
                                               boolean permitirColchonesExtra) {
        LocalDate desde = sols.stream().map(DTRHSolicitada::getFechaInicio).min(LocalDate::compareTo).orElseThrow();
        LocalDate hasta = sols.stream().map(DTRHSolicitada::getFechaFin).max(LocalDate::compareTo).orElseThrow();
        return !construirNivel2(sols, libres, permitirColchonesExtra, null, desde, hasta).isEmpty();
    }

    /** Subconjuntos maximales de atributos cubribles en nivel 2 (sin exigir el total). */
    private List<Set<AtributoHabitacion>> subconjuntosMaximalesNivel2(Set<AtributoHabitacion> union,
                                                                      List<DTRHSolicitada> sols,
                                                                      Map<LocalDate, Set<Habitacion>> libres,
                                                                      boolean permitirColchonesExtra,
                                                                      LocalDate desde, LocalDate hasta) {
        List<Set<AtributoHabitacion>> maximales = new ArrayList<>();
        for (Set<AtributoHabitacion> b : subsetsPorTamanoDesc(new ArrayList<>(union))) {
            if (maximales.stream().anyMatch(m -> m.containsAll(b))) {
                continue;
            }
            if (!construirNivel2(sols, libres, permitirColchonesExtra, b, desde, hasta).isEmpty()) {
                maximales.add(b);
            }
        }
        return maximales;
    }

    /**
     * Arma una propuesta por noche con el mínimo de habitaciones que cubra personas
     * y los atributos requeridos ({@code requeridosFijos == null} -> se exige la
     * unión de atributos pedidos de cada noche, sin relajar).
     */
    private Set<RHPropuesta> construirNivel2(List<DTRHSolicitada> sols,
                                             Map<LocalDate, Set<Habitacion>> libres,
                                             boolean permitirColchonesExtra,
                                             Set<AtributoHabitacion> requeridosFijos,
                                             LocalDate desde, LocalDate hasta) {
        Map<LocalDate, List<Habitacion>> eleccion = new LinkedHashMap<>();
        for (LocalDate n = desde; n.isBefore(hasta); n = n.plusDays(1)) {
            int personas = personasEnNoche(sols, n);
            Set<AtributoHabitacion> requeridos = requeridosFijos != null ? requeridosFijos : atributosEnNoche(sols, n);
            List<Habitacion> disponibles = new ArrayList<>(libres.getOrDefault(n, Set.of()));
            List<Habitacion> elegidas = elegirMinimas(disponibles, personas, requeridos, permitirColchonesExtra);
            if (elegidas == null) {
                return new LinkedHashSet<>();
            }
            eleccion.put(n, elegidas);
        }
        return fusionarEnPropuestas(eleccion, desde, hasta, permitirColchonesExtra);
    }

    /** Fusiona la elección por noche en propuestas (habitación, desde, hasta). */
    private Set<RHPropuesta> fusionarEnPropuestas(Map<LocalDate, List<Habitacion>> eleccion,
                                                  LocalDate desde, LocalDate hasta,
                                                  boolean permitirColchonesExtra) {
        Set<RHPropuesta> conjunto = new LinkedHashSet<>();
        for (Habitacion h : ControladorHabitaciones.getInstance().getHabitaciones()) {
            LocalDate inicioRango = null;
            LocalDate prev = null;
            for (LocalDate n = desde; n.isBefore(hasta); n = n.plusDays(1)) {
                boolean elegida = eleccion.getOrDefault(n, List.of()).contains(h);
                if (elegida) {
                    if (inicioRango == null) {
                        inicioRango = n;
                    }
                    prev = n;
                } else if (inicioRango != null) {
                    conjunto.add(propuestaRango(h, inicioRango, prev.plusDays(1), permitirColchonesExtra));
                    inicioRango = null;
                }
            }
            if (inicioRango != null) {
                conjunto.add(propuestaRango(h, inicioRango, prev.plusDays(1), permitirColchonesExtra));
            }
        }
        return conjunto;
    }

    /**
     * Interpretación 1: la cantidad de habitaciones se fija por la CAPACIDAD
     * (el mínimo que la cubre) y dentro de ese mínimo se exige que entren los
     * atributos requeridos. Si ningún subconjunto de ese tamaño los cubre,
     * devuelve null (y el llamador ofrece relajar atributos).
     */
    private List<Habitacion> elegirMinimas(List<Habitacion> disponibles,
                                           int personas,
                                           Set<AtributoHabitacion> requeridos,
                                           boolean permitirColchonesExtra) {
        List<Habitacion> ordenadas = new ArrayList<>(disponibles);
        ordenadas.sort(Comparator.comparingInt((Habitacion h) -> capacidadEfectiva(h, permitirColchonesExtra)).reversed());
        int kMin = -1;
        for (int k = 1; k <= ordenadas.size(); k++) {
            if (buscarSubconjuntoK(ordenadas, k, 0, new ArrayList<>(), personas, null, permitirColchonesExtra) != null) {
                kMin = k;
                break;
            }
        }
        if (kMin < 0) {
            return null;
        }
        return buscarSubconjuntoK(ordenadas, kMin, 0, new ArrayList<>(), personas, requeridos, permitirColchonesExtra);
    }

    private List<Habitacion> buscarSubconjuntoK(List<Habitacion> disponibles,
                                                int k,
                                                int inicio,
                                                List<Habitacion> actual,
                                                int personas,
                                                Set<AtributoHabitacion> requeridos,
                                                boolean permitirColchonesExtra) {
        if (actual.size() == k) {
            int capacidad = 0;
            Set<AtributoHabitacion> cubiertos = new TreeSet<>();
            for (Habitacion h : actual) {
                capacidad += capacidadEfectiva(h, permitirColchonesExtra);
                cubiertos.addAll(h.getAtributos());
            }
            if (capacidad < personas || (requeridos != null && !cubiertos.containsAll(requeridos))) {
                return null;
            }
            return new ArrayList<>(actual);
        }
        for (int i = inicio; i < disponibles.size(); i++) {
            actual.add(disponibles.get(i));
            List<Habitacion> r = buscarSubconjuntoK(disponibles, k, i + 1, actual, personas, requeridos, permitirColchonesExtra);
            if (r != null) {
                return r;
            }
            actual.remove(actual.size() - 1);
        }
        return null;
    }

    // =========================================================================
    // 6. Auxiliar: subconjuntosMaximales(A)
    // =========================================================================

    private Set<Set<AtributoHabitacion>> subconjuntosMaximales(DTRHSolicitada s,
                                                               List<DTRHSolicitada> todas,
                                                               boolean permitirColchonesExtra,
                                                               int relajacionDistribucion,
                                                               Map<LocalDate, Set<Habitacion>> libres) {
        List<AtributoHabitacion> elems = new ArrayList<>(s.getAtributosHabitacion());
        List<Set<AtributoHabitacion>> subsets = subsetsPorTamanoDesc(elems);
        List<Set<AtributoHabitacion>> maximales = new ArrayList<>();
        Criterio dist = distribucionCriterio(permitirColchonesExtra, relajacionDistribucion);
        for (Set<AtributoHabitacion> b : subsets) {
            boolean contenido = maximales.stream().anyMatch(m -> m.containsAll(b));
            if (contenido) {
                continue;
            }
            Criterio exigiendoB = (t, h) -> {
                if (!dist.acepta(t, h)) {
                    return false;
                }
                Set<AtributoHabitacion> requeridos = t.equals(s) ? b : t.getAtributosHabitacion();
                return h.getAtributos().containsAll(requeridos);
            };
            if (alcanza(todas, exigiendoB, libres)) {
                maximales.add(b);
            }
        }
        return new LinkedHashSet<>(maximales);
    }

    private List<Set<AtributoHabitacion>> subsetsPorTamanoDesc(List<AtributoHabitacion> elems) {
        int n = elems.size();
        List<Set<AtributoHabitacion>> subsets = new ArrayList<>();
        for (int mask = 1; mask < (1 << n); mask++) {
            Set<AtributoHabitacion> sub = new TreeSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    sub.add(elems.get(i));
                }
            }
            subsets.add(sub);
        }
        subsets.sort(Comparator.comparingInt((Set<AtributoHabitacion> x) -> x.size()).reversed());
        return subsets;
    }

    private void notificarFaltantes(InteraccionUsuario interaccion, DTRHSolicitada s, Set<Set<AtributoHabitacion>> maximales) {
        Set<AtributoHabitacion> mejor = maximales.stream().max(Comparator.comparingInt(Set::size)).orElse(Set.of());
        Set<AtributoHabitacion> faltantes = new TreeSet<>(s.getAtributosHabitacion());
        faltantes.removeAll(mejor);
        if (!faltantes.isEmpty()) {
            interaccion.notificar("Faltan atributos por cumplir: " + faltantes);
        }
    }

    // =========================================================================
    // Backtracking conjunto
    // =========================================================================

    /** Existe una asignación de habitaciones distintas por noche para TODAS las solicitudes (primera solución). */
    private boolean alcanza(List<DTRHSolicitada> sols, Criterio criterio, Map<LocalDate, Set<Habitacion>> libres) {
        return asignarPrimera(sols, 0, criterio, libres, new HashMap<>());
    }

    private boolean asignarPrimera(List<DTRHSolicitada> sols, int i, Criterio criterio,
                                   Map<LocalDate, Set<Habitacion>> libres,
                                   Map<Habitacion, Set<LocalDate>> usadas) {
        if (i == sols.size()) {
            return true;
        }
        DTRHSolicitada s = sols.get(i);
        for (Habitacion h : candidatas(s, criterio, libres, usadas)) {
            marcar(usadas, h, s);
            if (asignarPrimera(sols, i + 1, criterio, libres, usadas)) {
                desmarcar(usadas, h, s);
                return true;
            }
            desmarcar(usadas, h, s);
        }
        return false;
    }

    private void asignarTodas(List<DTRHSolicitada> sols, int i, Criterio criterio,
                              Map<LocalDate, Set<Habitacion>> libres,
                              Map<Habitacion, Set<LocalDate>> usadas,
                              List<Habitacion> actual,
                              List<List<Habitacion>> out) {
        if (i == sols.size()) {
            out.add(new ArrayList<>(actual));
            return;
        }
        DTRHSolicitada s = sols.get(i);
        for (Habitacion h : candidatas(s, criterio, libres, usadas)) {
            marcar(usadas, h, s);
            actual.add(h);
            asignarTodas(sols, i + 1, criterio, libres, usadas, actual, out);
            actual.remove(actual.size() - 1);
            desmarcar(usadas, h, s);
        }
    }

    private List<Habitacion> candidatas(DTRHSolicitada s, Criterio criterio,
                                        Map<LocalDate, Set<Habitacion>> libres,
                                        Map<Habitacion, Set<LocalDate>> usadas) {
        List<Habitacion> res = new ArrayList<>();
        for (Habitacion h : ControladorHabitaciones.getInstance().getHabitaciones()) {
            if (!criterio.acepta(s, h)) {
                continue;
            }
            if (!libreParaTodas(s, h, libres)) {
                continue;
            }
            if (conflicto(usadas, h, s)) {
                continue;
            }
            res.add(h);
        }
        return res;
    }

    private boolean libreParaTodas(DTRHSolicitada s, Habitacion h, Map<LocalDate, Set<Habitacion>> libres) {
        for (LocalDate n = s.getFechaInicio(); n.isBefore(s.getFechaFin()); n = n.plusDays(1)) {
            if (!libres.getOrDefault(n, Set.of()).contains(h)) {
                return false;
            }
        }
        return true;
    }

    private boolean conflicto(Map<Habitacion, Set<LocalDate>> usadas, Habitacion h, DTRHSolicitada s) {
        Set<LocalDate> ocupadas = usadas.get(h);
        if (ocupadas == null) {
            return false;
        }
        for (LocalDate n = s.getFechaInicio(); n.isBefore(s.getFechaFin()); n = n.plusDays(1)) {
            if (ocupadas.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private void marcar(Map<Habitacion, Set<LocalDate>> usadas, Habitacion h, DTRHSolicitada s) {
        Set<LocalDate> set = usadas.computeIfAbsent(h, k -> new HashSet<>());
        for (LocalDate n = s.getFechaInicio(); n.isBefore(s.getFechaFin()); n = n.plusDays(1)) {
            set.add(n);
        }
    }

    private void desmarcar(Map<Habitacion, Set<LocalDate>> usadas, Habitacion h, DTRHSolicitada s) {
        Set<LocalDate> set = usadas.get(h);
        if (set == null) {
            return;
        }
        for (LocalDate n = s.getFechaInicio(); n.isBefore(s.getFechaFin()); n = n.plusDays(1)) {
            set.remove(n);
        }
    }

    // =========================================================================
    // Criterios
    // =========================================================================

    private Criterio distribucionCriterio(boolean permitirColchonesExtra, int relajacionDistribucion) {
        return (s, h) -> {
            if (relajacionDistribucion == 0) {
                return camasAlcanzan(s, h, permitirColchonesExtra);
            }
            if (relajacionDistribucion == 1) {
                return capacidadEfectiva(h, permitirColchonesExtra) >= personas(s);
            }
            return true; // nivel 2 se resuelve aparte
        };
    }

    private Criterio criterioArmado(boolean permitirColchonesExtra,
                                    int relajacionDistribucion,
                                    boolean atributosRelajados,
                                    Map<DTRHSolicitada, Set<Set<AtributoHabitacion>>> maximalesPorSolicitud) {
        Criterio dist = distribucionCriterio(permitirColchonesExtra, relajacionDistribucion);
        return (s, h) -> {
            if (!dist.acepta(s, h)) {
                return false;
            }
            if (!atributosRelajados) {
                return h.getAtributos().containsAll(s.getAtributosHabitacion());
            }
            Set<AtributoHabitacion> cubiertos = new TreeSet<>(h.getAtributos());
            cubiertos.retainAll(s.getAtributosHabitacion());
            Set<Set<AtributoHabitacion>> maximales = maximalesPorSolicitud.getOrDefault(s, Set.of());
            return maximales.isEmpty() || maximales.stream().anyMatch(m -> m.equals(cubiertos));
        };
    }

    private boolean camasAlcanzan(DTRHSolicitada s, Habitacion h, boolean permitirColchonesExtra) {
        int individuales = h.getCantidadIndividuales() + (permitirColchonesExtra ? h.getColchones() : 0);
        // Las cunas no se exigen: si hay, mejor; si no, no importa.
        return h.getCantidadMatrimoniales() >= s.getCantMatrimonios()
                && individuales >= s.getCantIndividuales();
    }

    // =========================================================================
    // Penalidad
    // =========================================================================

    private List<Integer> penalidad(DTRHSolicitada s, Habitacion h, boolean permitirColchonesExtra,
                                    int relajacionDistribucion, Set<AtributoHabitacion> requeridos) {
        int faltantes = 0;
        for (AtributoHabitacion a : requeridos) {
            if (!h.getAtributos().contains(a)) {
                faltantes++;
            }
        }
        int colchones = colchonesUsados(s, h, permitirColchonesExtra);
        int sobrante = Math.max(0, capacidadEfectiva(h, permitirColchonesExtra) - personas(s));
        return List.of(relajacionDistribucion, faltantes, colchones, sobrante, 1);
    }

    // =========================================================================
    // Propuestas
    // =========================================================================

    private RHPropuesta propuesta(DTRHSolicitada s, Habitacion h, boolean permitirColchonesExtra) {
        long noches = Fechas.diasEntre(s.getFechaInicio(), s.getFechaFin());
        float subtotal = h.getTarifaPorNoche() * noches;
        AsignacionCamas.Asignacion asignacion = AsignacionCamas.calcular(h,
                s.getCantMatrimonios(), s.getCantIndividuales(), s.getCantBebes(),
                s.getCantCunas(), permitirColchonesExtra);
        RHPropuesta rp = new RHPropuesta(s.getFechaInicio(), s.getFechaFin(), new TreeSet<>(h.getAtributos()),
                subtotal, TipoMoneda.USD, asignacion.camasMatrimoniales(),
                asignacion.camasIndividuales(), asignacion.cunas(),
                s.getCantMatrimonios(), s.getCantIndividuales(), s.getCantBebes());
        rp.setHabitacion(h);
        return rp;
    }

    private RHPropuesta propuestaRango(Habitacion h, LocalDate desde, LocalDate hasta, boolean permitirColchonesExtra) {
        return propuestaRango(h, desde, hasta, permitirColchonesExtra, 0, 0, 0, 0);
    }

    private RHPropuesta propuestaRango(Habitacion h, LocalDate desde, LocalDate hasta,
                                       boolean permitirColchonesExtra, int parejas,
                                       int personasIndividuales, int bebes, int cunasPedidas) {
        long noches = Math.max(1, Fechas.diasEntre(desde, hasta));
        float subtotal = h.getTarifaPorNoche() * noches;
        if (parejas == 0 && personasIndividuales == 0 && bebes == 0) {
            // Nivel 2 sin desglose por persona: se mantiene el comportamiento
            // historico de usar todas las camas del cuarto.
            RHPropuesta rp = new RHPropuesta(desde, hasta, new TreeSet<>(h.getAtributos()),
                    subtotal, TipoMoneda.USD, h.getCantidadMatrimoniales(),
                    h.getCantidadIndividuales(), h.getCantidadCunas());
            rp.setHabitacion(h);
            return rp;
        }
        AsignacionCamas.Asignacion asignacion = AsignacionCamas.calcular(h,
                parejas, personasIndividuales, bebes, cunasPedidas, permitirColchonesExtra);
        RHPropuesta rp = new RHPropuesta(desde, hasta, new TreeSet<>(h.getAtributos()),
                subtotal, TipoMoneda.USD, asignacion.camasMatrimoniales(),
                asignacion.camasIndividuales(), asignacion.cunas(),
                parejas, personasIndividuales, bebes);
        rp.setHabitacion(h);
        return rp;
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private Map<LocalDate, Set<Habitacion>> nochesLibres(LocalDate min, LocalDate max) {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        Map<LocalDate, Set<Habitacion>> libres = new HashMap<>();
        for (LocalDate n = min; n.isBefore(max); n = n.plusDays(1)) {
            libres.put(n, ch.habitacionesLibresEnNoche(n));
        }
        return libres;
    }

    private int personas(DTRHSolicitada s) {
        // Los bebés NO cuentan como personas: ocupan una cuna si hay, pero no consumen capacidad.
        return 2 * s.getCantMatrimonios() + s.getCantIndividuales();
    }

    private int personasEnNoche(List<DTRHSolicitada> sols, LocalDate n) {
        int total = 0;
        for (DTRHSolicitada s : sols) {
            if (cubre(s, n)) {
                total += personas(s);
            }
        }
        return total;
    }

    private Set<AtributoHabitacion> atributosEnNoche(List<DTRHSolicitada> sols, LocalDate n) {
        Set<AtributoHabitacion> union = new TreeSet<>();
        for (DTRHSolicitada s : sols) {
            if (cubre(s, n)) {
                union.addAll(s.getAtributosHabitacion());
            }
        }
        return union;
    }

    private boolean cubre(DTRHSolicitada s, LocalDate n) {
        return !n.isBefore(s.getFechaInicio()) && n.isBefore(s.getFechaFin());
    }

    private int capacidadEfectiva(Habitacion h, boolean permitirColchonesExtra) {
        return h.capacidad() + (permitirColchonesExtra ? h.getColchones() : 0);
    }

    private int colchonesUsados(DTRHSolicitada s, Habitacion h, boolean permitirColchonesExtra) {
        if (!permitirColchonesExtra) {
            return 0;
        }
        int faltante = personas(s) - h.capacidad();
        if (faltante <= 0) {
            return 0;
        }
        return Math.min(faltante, h.getColchones());
    }

    private String configuracionDe(Habitacion h) {
        return h.getCantidadMatrimoniales() + "M" + h.getCantidadIndividuales() + "I"
                + h.getCantidadCunas() + "C" + h.getColchones() + "X" + new TreeSet<>(h.getAtributos());
    }
}
