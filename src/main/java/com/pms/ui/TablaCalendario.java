package com.pms.ui;

import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.utils.AtributoHabitacion;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Tabla del calendario: agrega sobre las celdas pintadas por
 * {@link RendererOcupacion} las etiquetas de cada reserva (titular + conteo de
 * personas/bebes), el preview gris de las propuestas del asistente, las flechas
 * de redimension de la reserva seleccionada y la tarjeta de informacion que
 * sigue al mouse.
 *
 * <p>La tarjeta es un overlay: se muestra SOLO sobre la celda de la habitacion
 * (columna 0) con las propiedades del cuarto, o sobre una celda ocupada con los
 * datos de la reserva. Permanece mientras el mouse este en esa celda y
 * desaparece al salir o al empezar un arrastre.
 *
 * <p>La etiqueta de cada reserva usa todo el ancho del rango: el nombre queda a
 * la izquierda (truncado con puntos suspensivos si no entra) y el conteo se
 * dibuja contra el borde derecho del rango.
 */
final class TablaCalendario extends JTable {

    private static final Color COLOR_ETIQUETA = new Color(0x33, 0x1A, 0x1A);
    private static final Color COLOR_PROPUESTA = new Color(0x9E, 0x9E, 0x9E, 165);
    private static final Color COLOR_PLACA = new Color(0xB0, 0x2A, 0x2A);
    private static final int PADDING = 4;
    private static final int MARGEN_VERTICAL = 4;
    private static final int ANCHO_TARJETA = 235;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EstadoArrastre estado;
    private final Font fuenteEtiqueta;
    private final List<RHPropuesta> propuestas = new ArrayList<>();

    /** Tarjeta reutilizable para el overlay de informacion. */
    private final JLabel tarjeta = new JLabel();

    private int hoverFila = -1;
    private int hoverColumna = -1;
    private Point hoverPunto;

    TablaCalendario(EstadoArrastre estado) {
        this.estado = estado;
        this.fuenteEtiqueta = getFont().deriveFont(Font.BOLD, 11f);

        setRowHeight(26);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // La seleccion de fila de JTable pintaria toda la fila de celeste y
        // taparia la barra; el rango lo pinta el renderer.
        setRowSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);
        setRowMargin(0);
        setOpaque(true);

        tarjeta.setOpaque(true);
        tarjeta.setBackground(new Color(0xFF, 0xFF, 0xFF, 245));
        tarjeta.setForeground(new Color(0x22, 0x22, 0x22));
        tarjeta.setFont(getFont().deriveFont(11f));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x8C, 0x8C, 0x8C)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                actualizarHover(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                limpiarHover();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                limpiarHover();
            }
        });
    }

    /** Propuestas del asistente que se pintan en gris antes de confirmarlas. */
    void setPropuestas(Collection<RHPropuesta> nuevas) {
        propuestas.clear();
        if (nuevas != null) {
            propuestas.addAll(nuevas);
        }
        repaint();
    }

    /** True cuando la fila se ve completa o parcialmente en el viewport. */
    boolean filaVisible(int fila) {
        if (fila < 0 || fila >= getRowCount()) {
            return false;
        }
        return getCellRect(fila, 0, false).intersects(getVisibleRect());
    }

    // ------------------------------------------------------------- tarjeta

    private void actualizarHover(Point punto) {
        int fila = rowAtPoint(punto);
        int columna = columnAtPoint(punto);
        if (fila < 0 || fila >= getRowCount()) {
            fila = -1;
            columna = -1;
        }
        hoverFila = fila;
        hoverColumna = columna;
        hoverPunto = new Point(punto);
        repaint();
    }

    void limpiarHover() {
        if (hoverFila < 0 && hoverColumna < 0) {
            return;
        }
        hoverFila = -1;
        hoverColumna = -1;
        hoverPunto = null;
        repaint();
    }

    /**
     * Texto de la tarjeta para la celda indicada: propiedades de la habitacion
     * en la columna 0, datos de la reserva cuando el mouse esta SOBRE LA BARRA
     * de una celda ocupada, {@code null} en una noche libre o sobre el blanco de
     * la celda. Sin punto explicito se usa el centro de la celda.
     */
    String textoHover(int fila, int columna) {
        return textoHover(fila, columna, null);
    }

    String textoHover(int fila, int columna, Point punto) {
        if (!(getModel() instanceof CalendarioOcupacionModel modelo)
                || fila < 0 || fila >= modelo.getRowCount()
                || columna < 0 || columna >= modelo.getColumnCount()) {
            return null;
        }
        if (columna == 0) {
            Habitacion habitacion = modelo.habitacionDeFila(fila);
            return habitacion == null ? null : envolver(htmlHabitacion(habitacion));
        }

        Rectangle celda = getCellRect(fila, columna, false);
        Point p = punto == null
                ? new Point(celda.x + celda.width / 2, celda.y + celda.height / 2) : punto;
        int x = p.x - celda.x;
        int y = p.y - celda.y;
        int altoBarraInicio = MARGEN_VERTICAL;
        int altoBarraFin = celda.height - MARGEN_VERTICAL;
        if (y < altoBarraInicio || y > altoBarraFin) {
            return null;
        }

        // La cabeza/interior se pinta DESPUES de la cola: si ambas cubren el
        // punto, gana la cabeza (la reserva que realmente ocupa la noche).
        OcupacionHabitacion.BloqueNoche head = modelo.bloqueEn(fila, columna);
        if (head != null && contieneBarra(modelo.segmentoEn(fila, columna), celda, x)) {
            return envolver(htmlReserva(modelo, fila, head));
        }
        OcupacionHabitacion.BloqueNoche tail = modelo.bloqueSalidaEn(fila, columna);
        if (tail != null && contieneBarra(modelo.segmentoSalidaEn(fila, columna), celda, x)) {
            return envolver(htmlReserva(modelo, fila, tail));
        }
        return null;
    }

    private static boolean contieneBarra(GeometriaBarra.Segmento segmento, Rectangle celda, int x) {
        if (segmento == null) {
            return false;
        }
        int[] px = GeometriaBarra.pixeles(segmento, celda.width);
        return x >= px[0] && x <= px[1];
    }

    /**
     * Bloque de reserva que esta debajo del punto, mirando los pixeles reales de
     * la barra (cabeza/interior o cola de check-out). Devuelve {@code null} en
     * una noche libre o en las franjas blancas de la celda.
     */
    OcupacionHabitacion.BloqueNoche bloqueEnPunto(int fila, int columna, Point punto) {
        if (!(getModel() instanceof CalendarioOcupacionModel modelo)
                || fila < 0 || fila >= modelo.getRowCount()
                || columna < 1 || columna >= modelo.getColumnCount() || punto == null) {
            return null;
        }
        Rectangle celda = getCellRect(fila, columna, false);
        int x = punto.x - celda.x;
        int y = punto.y - celda.y;
        if (y < MARGEN_VERTICAL || y > celda.height - MARGEN_VERTICAL) {
            return null;
        }
        OcupacionHabitacion.BloqueNoche head = modelo.bloqueEn(fila, columna);
        if (head != null && contieneBarra(modelo.segmentoEn(fila, columna), celda, x)) {
            return head;
        }
        OcupacionHabitacion.BloqueNoche tail = modelo.bloqueSalidaEn(fila, columna);
        if (tail != null && contieneBarra(modelo.segmentoSalidaEn(fila, columna), celda, x)) {
            return tail;
        }
        return null;
    }

    private static String envolver(String cuerpo) {
        return "<html><body style='width:" + ANCHO_TARJETA + "px'>" + cuerpo + "</body></html>";
    }

    private static String htmlHabitacion(Habitacion h) {
        String atributos = "ninguno";
        if (h.getAtributos() != null && !h.getAtributos().isEmpty()) {
            List<String> etiquetas = new ArrayList<>();
            for (AtributoHabitacion a : new TreeSet<>(h.getAtributos())) {
                etiquetas.add(DialogoHabitacion.etiquetaAtributo(a));
            }
            atributos = String.join(", ", etiquetas);
        }
        return "<b>Habitaci&oacute;n " + h.getNumero() + "</b><br>"
                + "Camas: " + h.getCantidadMatrimoniales() + " matrimonial(es), "
                + h.getCantidadIndividuales() + " individual(es)<br>"
                + "Cunas: " + h.getCantidadCunas() + " &middot; Colchones: " + h.getColchones()
                + " &middot; Capacidad: " + h.capacidad() + "<br>"
                + "Atributos: " + atributos + "<br>"
                + "Estado: " + (h.isLimpia() ? "limpia" : "sucia");
    }

    private static String htmlReserva(CalendarioOcupacionModel modelo, int fila,
                                      OcupacionHabitacion.BloqueNoche b) {
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        long noches = ChronoUnit.DAYS.between(b.fechaInicio(), b.fechaFin());
        StringBuilder html = new StringBuilder();
        html.append("<b>Reserva #").append(b.reservaId()).append("</b><br>");
        html.append("Titular: ").append(escapar(b.titular())).append("<br>");
        if (habitacion != null) {
            html.append("Habitaci&oacute;n: ").append(habitacion.getNumero()).append("<br>");
        }
        html.append("Check-in: ").append(FORMATO_FECHA.format(b.fechaInicio())).append("<br>");
        html.append("Check-out: ").append(FORMATO_FECHA.format(b.fechaFin())).append("<br>");
        html.append("Noches: ").append(noches).append("<br>");
        html.append("Personas: ").append(b.personas());
        if (b.bebes() > 0) {
            html.append(" &middot; Beb&eacute;s: ").append(b.bebes());
        }
        html.append("<br>");
        if (b.subtotal() > 0) {
            html.append("Subtotal: ").append(ServicioPropuestasDemo.formatearMonto(b.subtotal()))
                    .append("<br>");
        }
        if (b.estado() != null) {
            html.append("Estado: ").append(b.estado().name()).append("<br>");
        }
        if (b.lateCheckout()) {
            html.append("Late check-out: s&iacute;<br>");
        }
        return html.toString();
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -------------------------------------------------------------- pintado

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!(getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle clip = g.getClipBounds();
            if (clip != null) {
                g2.clip(clip);
            }
            pintarPropuestas(g2, modelo);
            pintarEtiquetas(g2, modelo);
            // La tarjeta sigue al mouse; se pinta antes que las flechas para
            // que los handles queden siempre visibles encima.
            pintarTarjeta(g2);
            pintarHandles(g2, modelo);
        } finally {
            g2.dispose();
        }
    }

    /** Preview gris de las propuestas devueltas por el asistente. */
    private void pintarPropuestas(Graphics2D g2, CalendarioOcupacionModel modelo) {
        if (propuestas.isEmpty()) {
            return;
        }
        Map<Long, Integer> filaPorHabitacion = new HashMap<>();
        Map<Integer, Integer> filaPorNumero = new HashMap<>();
        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            Habitacion h = modelo.habitacionDeFila(fila);
            if (h != null && h.getId() != null) {
                filaPorHabitacion.put(h.getId(), fila);
            }
            if (h != null && h.getNumero() != null) {
                filaPorNumero.put(h.getNumero(), fila);
            }
        }

        Rectangle visible = getVisibleRect();
        for (RHPropuesta p : propuestas) {
            if (p == null || p.getHabitacion() == null
                    || p.getFechaInicio() == null || p.getFechaFin() == null) {
                continue;
            }
            Integer fila = p.getHabitacion().getId() != null
                    ? filaPorHabitacion.get(p.getHabitacion().getId()) : null;
            if (fila == null) {
                fila = filaPorNumero.get(p.getHabitacion().getNumero());
            }
            if (fila == null) {
                continue;
            }
            GeometriaBarra.Bloque bloque =
                    new GeometriaBarra.Bloque(p.getFechaInicio(), p.getFechaFin(), false);
            for (int columna = 1; columna < modelo.getColumnCount(); columna++) {
                LocalDate dia = modelo.fechaDeColumna(columna);
                GeometriaBarra.Segmento segmento = GeometriaBarra.segmento(bloque, dia);
                if (segmento == null) {
                    continue;
                }
                Rectangle celda = getCellRect(fila, columna, false);
                if (!celda.intersects(visible)) {
                    continue;
                }
                int[] px = GeometriaBarra.pixeles(segmento, celda.width);
                g2.setColor(COLOR_PROPUESTA);
                g2.fillRect(celda.x + px[0], celda.y + MARGEN_VERTICAL,
                        Math.max(0, px[1] - px[0]), celda.height - 2 * MARGEN_VERTICAL);
            }
        }
    }

    /** Etiqueta de cada reserva visible, mas la de la reserva que se esta moviendo o redimensionando. */
    private void pintarEtiquetas(Graphics2D g2, CalendarioOcupacionModel modelo) {
        Rectangle visible = getVisibleRect();
        FontMetrics fm = getFontMetrics(fuenteEtiqueta);
        g2.setFont(fuenteEtiqueta);

        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            if (!getCellRect(fila, 0, false).intersects(visible)) {
                continue;
            }
            for (OcupacionHabitacion.BloqueNoche bloque : modelo.bloquesDeFila(fila)) {
                if (estaOcultoEnLaFila(fila, bloque)) {
                    continue;
                }
                if (estaRedimensionando(fila, bloque)) {
                    continue;
                }
                pintarEtiqueta(g2, fm, modelo, fila, bloque);
            }
        }

        // La reserva movida se dibuja -con su etiqueta- sobre la fila destino
        // resuelta, igual que la barra en color original del renderer. Puede no
        // coincidir con la fila bajo el cursor si esa fila es gris.
        int destino = estado.filaDestino();
        if (estado.esMovimiento() && destino >= 0 && destino < modelo.getRowCount()
                && destino != estado.filaOrigen()) {
            OcupacionHabitacion.BloqueNoche origen =
                    buscarBloque(modelo, estado.filaOrigen(), estado.reservaOrigenId());
            if (origen != null) {
                pintarEtiqueta(g2, fm, modelo, destino, origen);
            }
        }

        // Durante una redimension la etiqueta sigue al rango previsualizado.
        if (estado.esRedimension() && estado.inicioRedimension() != null
                && estado.finRedimension() != null) {
            OcupacionHabitacion.BloqueNoche original =
                    buscarBloque(modelo, estado.filaSeleccionada(), estado.reservaSeleccionadaId());
            if (original != null) {
                OcupacionHabitacion.BloqueNoche preview = new OcupacionHabitacion.BloqueNoche(
                        original.reservaId(), estado.inicioRedimension(), estado.finRedimension(),
                        original.lateCheckout(), original.titular(), original.personas(),
                        original.bebes(), original.subtotal(), original.moneda(), original.estado());
                pintarEtiqueta(g2, fm, modelo, estado.filaSeleccionada(), preview);
            }
        }
    }

    /** La reserva movida desaparece de su fila original apenas el cursor la abandona. */
    private boolean estaOcultoEnLaFila(int fila, OcupacionHabitacion.BloqueNoche bloque) {
        return estado.esMovimiento()
                && fila == estado.filaOrigen()
                && estado.ocultarBloqueOrigen()
                && estado.reservaOrigenId() != null
                && estado.reservaOrigenId() == bloque.reservaId();
    }

    /** La barra original de la reserva seleccionada se oculta mientras se redimensiona. */
    private boolean estaRedimensionando(int fila, OcupacionHabitacion.BloqueNoche bloque) {
        return estado.esRedimension()
                && fila == estado.filaSeleccionada()
                && estado.reservaSeleccionadaId() != null
                && estado.reservaSeleccionadaId() == bloque.reservaId();
    }

    private static OcupacionHabitacion.BloqueNoche buscarBloque(CalendarioOcupacionModel modelo,
                                                                int fila, Long reservaId) {
        if (fila < 0 || fila >= modelo.getRowCount() || reservaId == null) {
            return null;
        }
        for (OcupacionHabitacion.BloqueNoche bloque : modelo.bloquesDeFila(fila)) {
            if (reservaId == bloque.reservaId()) {
                return bloque;
            }
        }
        return null;
    }

    private void pintarEtiqueta(Graphics2D g2, FontMetrics fm, CalendarioOcupacionModel modelo,
                                int fila, OcupacionHabitacion.BloqueNoche bloque) {
        if (bloque == null || bloque.fechaInicio() == null || bloque.fechaFin() == null) {
            return;
        }
        int colIni = columnaInicialVisible(modelo, bloque.fechaInicio());
        int colFin = columnaFinalVisible(modelo, bloque.fechaFin());
        if (colIni < 1 || colFin < 1 || colFin < colIni) {
            return;
        }

        Rectangle celdaIni = getCellRect(fila, colIni, false);
        Rectangle celdaFin = getCellRect(fila, colFin, false);
        if (!celdaIni.intersects(getVisibleRect())) {
            return;
        }

        GeometriaBarra.Bloque geometria =
                new GeometriaBarra.Bloque(bloque.fechaInicio(), bloque.fechaFin(), bloque.lateCheckout());
        int[] pxIni = GeometriaBarra.pixeles(
                GeometriaBarra.segmento(geometria, modelo.fechaDeColumna(colIni)), celdaIni.width);
        int[] pxFin = GeometriaBarra.pixeles(
                GeometriaBarra.segmento(geometria, modelo.fechaDeColumna(colFin)), celdaFin.width);

        // La etiqueta arranca donde arranca la barra (no en el borde de la celda
        // de check-in) y termina en el borde derecho real del rango.
        int x0 = celdaIni.x + pxIni[0] + PADDING;
        int x1 = celdaFin.x + pxFin[1] - PADDING;
        int ancho = x1 - x0;
        if (ancho < 14) {
            return;
        }

        int yBarra = celdaIni.y + MARGEN_VERTICAL;
        int altoBarra = Math.max(1, celdaIni.height - 2 * MARGEN_VERTICAL);
        int baseline = yBarra + (altoBarra - fm.getHeight()) / 2 + fm.getAscent();

        String conteo = EtiquetaReserva.conteo(bloque.personas(), bloque.bebes());
        // Etiqueta orientada a la izquierda: nombre y conteo seguidos. El conteo
        // se reserva primero, asi siempre se ve aunque el nombre se recorte.
        String guion = " \u2014 ";
        int anchoConteo = fm.stringWidth(conteo);
        int anchoNombreMinimo = fm.stringWidth("M" + EtiquetaReserva.PUNTOS);
        boolean usarGuion = !conteo.isEmpty()
                && ancho - anchoConteo - fm.stringWidth(guion) >= anchoNombreMinimo;
        String sufijo = conteo.isEmpty() ? "" : (usarGuion ? guion + conteo : " " + conteo);
        int anchoSufijo = fm.stringWidth(sufijo);
        int anchoNombre = Math.max(0, ancho - anchoSufijo);
        String nombre = EtiquetaReserva.truncar(bloque.titular(), anchoNombre, fm::stringWidth);

        Shape clipPrevio = g2.getClip();
        g2.clipRect(x0, celdaIni.y, ancho + PADDING, celdaIni.height);
        try {
            g2.setColor(COLOR_ETIQUETA);
            int xTexto = x0;
            if (!nombre.isEmpty()) {
                g2.drawString(nombre, xTexto, baseline);
                xTexto += fm.stringWidth(nombre);
            }
            if (!sufijo.isEmpty()) {
                g2.drawString(sufijo, xTexto, baseline);
            }
        } finally {
            g2.setClip(clipPrevio);
        }
    }

    // -------------------------------------------------------------- flechas

    /** Zona de flecha bajo el punto, solo si hay una reserva seleccionada. */
    HandlesEstadia.Zona zonaDeHandle(Point punto) {
        if (estado.esRedimension()) {
            return null;
        }
        return HandlesEstadia.en(zonasHandles(), punto.x, punto.y);
    }

    /**
     * Coordenada X del extremo real de la barra para el borde indicado: el
     * inicio de la placa del check-in o el fin de la placa del check-out.
     */
    int edgeXDeHandle(HandlesEstadia.Borde borde) {
        List<HandlesEstadia.Zona> zonas = zonasHandles();
        if (zonas.isEmpty()) {
            return -1;
        }
        if (borde == HandlesEstadia.Borde.INICIO) {
            return zonas.stream()
                    .filter(z -> z.borde() == borde)
                    .mapToInt(z -> z.rect().x)
                    .min().orElse(-1);
        }
        return zonas.stream()
                .filter(z -> z.borde() == borde)
                .mapToInt(z -> z.rect().x + z.rect().width)
                .max().orElse(-1);
    }

    private List<HandlesEstadia.Zona> zonasHandles() {
        if (!estado.haySeleccion() || !(getModel() instanceof CalendarioOcupacionModel modelo)) {
            return List.of();
        }
        int fila = estado.filaSeleccionada();
        GeometriaBarra.Bloque bloque = estado.bloqueSeleccionado();
        if (fila < 0 || fila >= modelo.getRowCount() || bloque == null) {
            return List.of();
        }
        int ultima = modelo.getColumnCount() - 1;
        int colIni = modelo.columnaDeFecha(bloque.fechaInicio());
        int colFin = modelo.columnaDeFecha(bloque.fechaFin());
        boolean inicioVisible = colIni >= 1 && colIni <= ultima;
        boolean finVisible = colFin >= 1 && colFin <= ultima;
        HandlesEstadia.Flechas pedidas = estado.flechasSeleccion();
        HandlesEstadia.Flechas efectivas = new HandlesEstadia.Flechas(
                inicioVisible && pedidas.alargarInicio(),
                inicioVisible && pedidas.acortarInicio(),
                finVisible && pedidas.acortarFin(),
                finVisible && pedidas.alargarFin());
        if (!efectivas.alguna()) {
            return List.of();
        }
        Rectangle celdaIni = new Rectangle();
        Rectangle barraIni = new Rectangle();
        Rectangle celdaFin = new Rectangle();
        Rectangle barraFin = new Rectangle();
        if (inicioVisible) {
            celdaIni = getCellRect(fila, colIni, false);
            barraIni = barraEnCelda(bloque, modelo.fechaDeColumna(colIni), celdaIni);
        }
        if (finVisible) {
            celdaFin = getCellRect(fila, colFin, false);
            barraFin = barraEnCelda(bloque, modelo.fechaDeColumna(colFin), celdaFin);
        }
        return HandlesEstadia.zonas(celdaIni, barraIni, celdaFin, barraFin, efectivas);
    }

    private static Rectangle barraEnCelda(GeometriaBarra.Bloque bloque, LocalDate dia,
                                          Rectangle celda) {
        GeometriaBarra.Segmento segmento = GeometriaBarra.segmento(bloque, dia);
        int alto = Math.max(1, celda.height - 2 * MARGEN_VERTICAL);
        if (segmento == null) {
            return new Rectangle(celda.x, celda.y + MARGEN_VERTICAL, 1, alto);
        }
        int[] px = GeometriaBarra.pixeles(segmento, celda.width);
        return new Rectangle(celda.x + px[0], celda.y + MARGEN_VERTICAL,
                Math.max(1, px[1] - px[0]), alto);
    }

    private void pintarHandles(Graphics2D g2, CalendarioOcupacionModel modelo) {
        if (!estado.haySeleccion() || estado.esRedimension()) {
            return;
        }
        List<HandlesEstadia.Zona> zonas = zonasHandles();
        if (zonas.isEmpty()) {
            return;
        }
        Rectangle placaInicio = null;
        Rectangle placaFin = null;
        for (HandlesEstadia.Zona zona : zonas) {
            Rectangle r = zona.rect();
            if (zona.borde() == HandlesEstadia.Borde.INICIO) {
                placaInicio = unir(placaInicio, r);
            } else {
                placaFin = unir(placaFin, r);
            }
        }
        g2.setColor(COLOR_PLACA);
        pintarPlaca(g2, placaInicio);
        pintarPlaca(g2, placaFin);
        for (HandlesEstadia.Zona zona : zonas) {
            pintarTriangulo(g2, zona);
        }
    }

    private static Rectangle unir(Rectangle actual, Rectangle nuevo) {
        if (actual == null) {
            return new Rectangle(nuevo);
        }
        return actual.union(nuevo);
    }

    private static void pintarPlaca(Graphics2D g2, Rectangle placa) {
        if (placa == null) {
            return;
        }
        g2.fillRoundRect(placa.x - 1, placa.y, placa.width + 2, placa.height, 6, 6);
    }

    private static void pintarTriangulo(Graphics2D g2, HandlesEstadia.Zona zona) {
        Rectangle r = zona.rect();
        int margenY = Math.max(2, r.height / 4);
        int[] xs;
        int[] ys;
        if (zona.haciaIzquierda()) {
            xs = new int[]{r.x + r.width - 2, r.x + r.width - 2, r.x + 1};
            ys = new int[]{r.y + margenY, r.y + r.height - margenY, r.y + r.height / 2};
        } else {
            xs = new int[]{r.x + 1, r.x + 1, r.x + r.width - 2};
            ys = new int[]{r.y + margenY, r.y + r.height - margenY, r.y + r.height / 2};
        }
        g2.setColor(Color.WHITE);
        g2.fillPolygon(xs, ys, 3);
    }

    // -------------------------------------------------------------- tarjeta

    private void pintarTarjeta(Graphics2D g2) {
        if (estado.activo() || hoverFila < 0 || hoverColumna < 0 || hoverPunto == null) {
            return;
        }
        String texto = textoHover(hoverFila, hoverColumna, hoverPunto);
        if (texto == null) {
            return;
        }
        tarjeta.setText(texto);
        Dimension tamano = tarjeta.getPreferredSize();
        Rectangle visible = getVisibleRect();
        int x = hoverPunto.x + 14;
        int y = hoverPunto.y + 14;
        if (x + tamano.width > visible.x + visible.width) {
            x = hoverPunto.x - tamano.width - 14;
        }
        if (y + tamano.height > visible.y + visible.height) {
            y = hoverPunto.y - tamano.height - 14;
        }
        x = Math.max(visible.x + 2,
                Math.min(x, visible.x + visible.width - tamano.width - 2));
        y = Math.max(visible.y + 2,
                Math.min(y, visible.y + visible.height - tamano.height - 2));
        tarjeta.setSize(tamano);
        Graphics2D g2t = (Graphics2D) g2.create();
        try {
            g2t.translate(x, y);
            tarjeta.paint(g2t);
        } finally {
            g2t.dispose();
        }
    }

    /**
     * Columna inicial visible del bloque: si la estadia arranca antes de la
     * ventana, la etiqueta se ancla a la primera columna visible para que el
     * principio del nombre nunca se pierda.
     */
    private static int columnaInicialVisible(CalendarioOcupacionModel modelo, LocalDate fechaInicio) {
        int columna = modelo.columnaDeFecha(fechaInicio);
        if (columna >= 1) {
            return columna;
        }
        LocalDate primera = modelo.fechaDeColumna(1);
        return primera != null && fechaInicio.isBefore(primera) ? 1 : -1;
    }

    /** Columna final visible: el dia de check-out del bloque, recortado a la ventana. */
    private static int columnaFinalVisible(CalendarioOcupacionModel modelo, LocalDate fechaFin) {
        int columna = modelo.columnaDeFecha(fechaFin);
        if (columna >= 1) {
            return columna;
        }
        LocalDate ultima = modelo.fechaDeColumna(modelo.getColumnCount() - 1);
        return ultima != null && fechaFin.isAfter(ultima) ? modelo.getColumnCount() - 1 : -1;
    }
}
