package com.pms.ui;

import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test de la tabla del calendario: pinta en un buffer sin abrir ventanas
 * y verifica que las etiquetas, el preview de propuestas y el tooltip no
 * rompan el EDT.
 */
class TablaCalendarioTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIN = INICIO.plusDays(3);

    private static CalendarioOcupacionModel modelo() {
        Habitacion h103 = new Habitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 1);
        h103.setTarifaPorNoche(1800.0f);
        OcupacionHabitacion.BloqueNoche bloque = new OcupacionHabitacion.BloqueNoche(
                7L, INICIO, FIN, false, "Agustin Rivero", 3, 1);
        OcupacionHabitacion ocupacion = new OcupacionHabitacion(h103,
                Set.of(INICIO, INICIO.plusDays(1), INICIO.plusDays(2)),
                Map.of(INICIO, 7L), List.of(bloque));
        return new CalendarioOcupacionModel(INICIO, 14, List.of(ocupacion));
    }

    private static TablaCalendario tabla(EstadoArrastre estado) {
        return tabla(estado, modelo());
    }

    private static TablaCalendario tabla(EstadoArrastre estado, CalendarioOcupacionModel modelo) {
        TablaCalendario tabla = new TablaCalendario(estado);
        tabla.setModel(modelo);
        tabla.setDefaultRenderer(Object.class, new RendererOcupacion(estado, fila -> true));
        tabla.setSize(1000, 200);
        tabla.doLayout();
        for (int columna = 0; columna < tabla.getColumnCount(); columna++) {
            tabla.getColumnModel().getColumn(columna).setWidth(columna == 0 ? 110 : 56);
        }
        return tabla;
    }

    private static BufferedImage pintar(TablaCalendario tabla) {
        BufferedImage imagen = new BufferedImage(tabla.getWidth(), tabla.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagen.createGraphics();
        try {
            tabla.paint(g2);
        } finally {
            g2.dispose();
        }
        return imagen;
    }

    @Test
    void pintaEtiquetasSinExcepcion() {
        TablaCalendario tabla = tabla(new EstadoArrastre());
        BufferedImage imagen = pintar(tabla);

        // La barra roja y el texto oscuro de la etiqueta tienen que existir en
        // el area del bloque (fila 0, columnas 1..4).
        boolean hayBarra = false;
        boolean hayTexto = false;
        for (int x = 110; x < 110 + 4 * 56 && x < imagen.getWidth(); x++) {
            for (int y = 0; y < tabla.getRowHeight(); y++) {
                int rgb = imagen.getRGB(x, y);
                int rojo = (rgb >> 16) & 0xFF;
                int verde = (rgb >> 8) & 0xFF;
                int azul = rgb & 0xFF;
                if (rojo > 200 && verde > 80 && verde < 160 && azul > 80 && azul < 160) {
                    hayBarra = true;
                }
                if (rojo < 90 && verde < 90 && azul < 90) {
                    hayTexto = true;
                }
            }
        }
        assertTrue(hayBarra, "la barra de la reserva debe pintarse");
        assertTrue(hayTexto, "la etiqueta del titular debe pintarse sobre la barra");
    }

    @Test
    void pintaPreviewDePropuestasSinExcepcion() {
        TablaCalendario tabla = tabla(new EstadoArrastre());
        RHPropuesta propuesta = new RHPropuesta(INICIO.plusDays(5), INICIO.plusDays(8),
                Set.of(AtributoHabitacion.Balcon), 3000.0f, TipoMoneda.USD, 1, 1, 0);
        Habitacion h103 = new Habitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 1);
        propuesta.setHabitacion(h103);

        tabla.setPropuestas(Set.of(propuesta));
        pintar(tabla);

        tabla.setPropuestas(Set.of());
        pintar(tabla);
    }

    @Test
    void laTarjetaDeLaHabitacionSoloApareceSobreLaColumnaDeHabitacion() {
        TablaCalendario tabla = tabla(new EstadoArrastre());

        String habitacion = tabla.textoHover(0, 0);

        assertNotNull(habitacion);
        assertTrue(habitacion.contains("103"), "numero de habitacion");
        assertTrue(habitacion.contains("Camas"), "camas");
        assertTrue(habitacion.contains("Balc"), "atributos");
        assertFalse(habitacion.contains("1800"), "la tarifa no se muestra");
        assertNull(tabla.textoHover(0, 5), "una noche libre no muestra la habitacion");
    }

    @Test
    void laTarjetaDeUnaReservaMuestraTitularYDatos() {
        TablaCalendario tabla = tabla(new EstadoArrastre());

        String reserva = tabla.textoHover(0, 1);

        assertNotNull(reserva);
        assertTrue(reserva.contains("Reserva #7"), "id de la reserva");
        assertTrue(reserva.contains("Agustin Rivero"), "nombre y apellido del titular");
        assertTrue(reserva.contains("Personas: 3"), "conteo de personas");
        assertTrue(reserva.contains("Noches: 3"), "duracion de la estadia");
    }

    @Test
    void laTarjetaDeReservaSoloApareceSobreLaBarraYNoEnElBlanco() {
        TablaCalendario tabla = tabla(new EstadoArrastre());

        Rectangle checkin = tabla.getCellRect(0, 1, false);
        assertNull(tabla.textoHover(0, 1, new Point(checkin.x + 2, checkin.y + 10)),
                "el inset blanco del check-in no muestra la reserva");
        assertNotNull(tabla.textoHover(0, 1, new Point(checkin.x + 30, checkin.y + 10)));

        Rectangle checkout = tabla.getCellRect(0, 4, false);
        assertNotNull(tabla.textoHover(0, 4, new Point(checkout.x + 4, checkout.y + 10)),
                "la cola del check-out si muestra la reserva");
        assertNull(tabla.textoHover(0, 4, new Point(checkout.x + 30, checkout.y + 10)),
                "el blanco del dia de salida no muestra la reserva");
    }

    @Test
    void bloqueEnPuntoSoloDevuelveLaReservaSobreLaBarra() {
        TablaCalendario tabla = tabla(new EstadoArrastre());

        Rectangle checkin = tabla.getCellRect(0, 1, false);
        assertNull(tabla.bloqueEnPunto(0, 1, new Point(checkin.x + 2, checkin.y + 10)),
                "el blanco del check-in no selecciona la reserva");
        assertNotNull(tabla.bloqueEnPunto(0, 1, new Point(checkin.x + 30, checkin.y + 10)));

        Rectangle checkout = tabla.getCellRect(0, 4, false);
        assertNotNull(tabla.bloqueEnPunto(0, 4, new Point(checkout.x + 4, checkout.y + 10)),
                "la cola de check-out si selecciona la reserva");
        assertNull(tabla.bloqueEnPunto(0, 4, new Point(checkout.x + 30, checkout.y + 10)),
                "el blanco del dia de salida no selecciona la reserva");
    }

    @Test
    void enUnaCeldaCompartidaMuestraLaReservaQueEstaBajoElMouse() {
        Habitacion h = habitacion(103);
        OcupacionHabitacion.BloqueNoche saliente = new OcupacionHabitacion.BloqueNoche(
                7L, INICIO.minusDays(2), INICIO, false, "Sale Temprano", 2, 0);
        OcupacionHabitacion.BloqueNoche entrante = new OcupacionHabitacion.BloqueNoche(
                8L, INICIO, INICIO.plusDays(2), false, "Entra Nuevo", 1, 0);
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(
                new OcupacionHabitacion(h,
                        Set.of(INICIO, INICIO.plusDays(1)),
                        Map.of(INICIO, 8L, INICIO.plusDays(1), 8L),
                        List.of(saliente, entrante))));
        TablaCalendario tabla = tabla(new EstadoArrastre(), modelo);
        Rectangle celda = tabla.getCellRect(0, 1, false);

        String cola = tabla.textoHover(0, 1, new Point(celda.x + 4, celda.y + 10));
        String cabeza = tabla.textoHover(0, 1, new Point(celda.x + 40, celda.y + 10));

        assertNotNull(cola);
        assertTrue(cola.contains("Sale Temprano"), "a la izquierda esta la cola de la saliente");
        assertNotNull(cabeza);
        assertTrue(cabeza.contains("Entra Nuevo"), "a la derecha esta el check-in de la entrante");
    }

    @Test
    void laTarjetaDeHoverSePintaSinExcepcion() {
        TablaCalendario tabla = tabla(new EstadoArrastre());
        MouseEvent evento = new MouseEvent(tabla, MouseEvent.MOUSE_MOVED, 0L, 0,
                50, 10, 0, false);
        for (java.awt.event.MouseMotionListener listener : tabla.getMouseMotionListeners()) {
            listener.mouseMoved(evento);
        }

        BufferedImage imagen = pintar(tabla);

        assertNotNull(imagen);
    }

    @Test
    void lasFlechasDeLaReservaSeleccionadaSePintanYSePuedenGolpear() {
        EstadoArrastre estado = new EstadoArrastre();
        TablaCalendario tabla = tabla(estado);
        estado.seleccionar(0, 7L, new GeometriaBarra.Bloque(INICIO, FIN, false),
                new HandlesEstadia.Flechas(true, true, true, true));

        pintar(tabla);

        boolean hayZona = false;
        for (int x = 110; x < 110 + 4 * 56 && x < tabla.getWidth() && !hayZona; x++) {
            for (int y = 0; y < tabla.getRowHeight(); y++) {
                if (tabla.zonaDeHandle(new java.awt.Point(x, y)) != null) {
                    hayZona = true;
                    break;
                }
            }
        }
        assertTrue(hayZona, "la reserva seleccionada debe exponer flechas clickeables");
    }

    @Test
    void elUmbralDeRedimensionSeMideDesdeElExtremoDeLaBarra() {
        EstadoArrastre estado = new EstadoArrastre();
        TablaCalendario tabla = tabla(estado);
        GeometriaBarra.Bloque bloque = new GeometriaBarra.Bloque(INICIO, FIN, false);
        estado.seleccionar(0, 7L, bloque, new HandlesEstadia.Flechas(true, true, true, true));

        Rectangle checkin = tabla.getCellRect(0, 1, false);
        Rectangle checkout = tabla.getCellRect(0, 4, false);
        int[] pxIni = GeometriaBarra.pixeles(
                GeometriaBarra.segmento(bloque, INICIO), checkin.width);
        int[] pxFin = GeometriaBarra.pixeles(
                GeometriaBarra.segmento(bloque, FIN), checkout.width);

        assertEquals(checkin.x + pxIni[0], tabla.edgeXDeHandle(HandlesEstadia.Borde.INICIO));
        assertEquals(checkout.x + pxFin[1], tabla.edgeXDeHandle(HandlesEstadia.Borde.FIN));
    }

    // --------------------------------------------------- movimiento con snap

    private static Habitacion habitacion(int numero) {
        Habitacion h = new Habitacion(numero, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 1);
        h.setTarifaPorNoche(1800.0f);
        return h;
    }

    /** Dos filas: la 0 libre y la 1 con la reserva 7 que se va a mover. */
    private static CalendarioOcupacionModel modeloConOrigen() {
        Habitacion libre = habitacion(101);
        Habitacion origen = habitacion(102);
        OcupacionHabitacion.BloqueNoche bloque = new OcupacionHabitacion.BloqueNoche(
                7L, INICIO, INICIO.plusDays(3), false, "Agustin Rivero", 3, 0);
        return new CalendarioOcupacionModel(INICIO, 14, List.of(
                new OcupacionHabitacion(libre, Set.of(), Map.of(), List.of()),
                new OcupacionHabitacion(origen,
                        Set.of(INICIO, INICIO.plusDays(1), INICIO.plusDays(2)),
                        Map.of(INICIO, 7L), List.of(bloque))));
    }

    private static boolean esBarra(int rgb) {
        int rojo = (rgb >> 16) & 0xFF;
        int verde = (rgb >> 8) & 0xFF;
        int azul = rgb & 0xFF;
        return rojo > 200 && verde > 80 && verde < 160 && azul > 80 && azul < 160;
    }

    @Test
    void alMoverLaReservaSeLevantaDelOrigenYSePintaEnElDestinoResuelto() {
        EstadoArrastre estado = new EstadoArrastre();
        TablaCalendario tabla = tabla(estado, modeloConOrigen());
        estado.iniciarMovimiento(1, 1,
                new GeometriaBarra.Bloque(INICIO, INICIO.plusDays(3), false), 7L);
        estado.hover(0);
        estado.destino(0);

        BufferedImage imagen = pintar(tabla);

        int x = 110 + 3 * 56 - 10;
        int yDestino = 21;
        int yOrigen = tabla.getRowHeight() + 21;
        assertTrue(esBarra(imagen.getRGB(x, yDestino)), "la reserva se pinta en la fila destino");
        assertFalse(esBarra(imagen.getRGB(x, yOrigen)), "la reserva ya no se pinta en el origen");
    }

    /**
     * Bug visual: en la celda de traspaso del destino conviven la cola de la
     * reserva movida y el check-in del proximo huesped. El check-in no debe
     * desaparecer durante el arrastre.
     */
    @Test
    void elCheckinVecinoSigueVisibleJuntoALaColaDeLaReservaMovida() {
        Habitacion destino = habitacion(101);
        Habitacion origen = habitacion(102);
        OcupacionHabitacion.BloqueNoche entrante = new OcupacionHabitacion.BloqueNoche(
                8L, INICIO.plusDays(2), INICIO.plusDays(4), false, "Otro Huesped", 2, 0);
        OcupacionHabitacion.BloqueNoche movida = new OcupacionHabitacion.BloqueNoche(
                7L, INICIO, INICIO.plusDays(2), false, "Agustin Rivero", 3, 0);
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(
                new OcupacionHabitacion(destino,
                        Set.of(INICIO.plusDays(2), INICIO.plusDays(3)),
                        Map.of(INICIO.plusDays(2), 8L, INICIO.plusDays(3), 8L),
                        List.of(entrante)),
                new OcupacionHabitacion(origen,
                        Set.of(INICIO, INICIO.plusDays(1)),
                        Map.of(INICIO, 7L, INICIO.plusDays(1), 7L),
                        List.of(movida))));

        EstadoArrastre estado = new EstadoArrastre();
        TablaCalendario tabla = tabla(estado, modelo);
        estado.iniciarMovimiento(1, 1,
                new GeometriaBarra.Bloque(INICIO, INICIO.plusDays(2), false), 7L);
        estado.hover(0);
        estado.destino(0);

        BufferedImage imagen = pintar(tabla);

        // Columna del dia de traspaso: la cabeza del check-in ocupa [0.25,1]
        // (desde ~x=222+16) y la cola movida [0,0.25]. La cabeza debe seguir roja.
        int xCheckin = 110 + 2 * 56 + 30;
        assertTrue(esBarra(imagen.getRGB(xCheckin, 21)),
                "el check-in del huesped entrante no debe desaparecer durante el arrastre");
    }
}
