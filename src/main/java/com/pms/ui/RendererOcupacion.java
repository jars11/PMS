package com.pms.ui;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.util.function.IntFunction;

/**
 * Renderer del calendario. Pinta, por celda, el fondo de la habitacion (limpia
 * o sucia), la barra contigua de la estadia usando la geometria fraccionaria de
 * {@link GeometriaBarra}, la cola del dia de salida, el borde de traspaso, el
 * gris de las habitaciones incompatibles durante un movimiento y el preview
 * gris sobre la fila valida apuntada.
 *
 * <p>La contiguedad se logra porque el segmento {@code x1=1.0} llega al borde
 * derecho de la celda y el {@code x0=0.0} de la vecina al borde izquierdo, con
 * separacion entre celdas cero. El estado del arrastre se inyecta como
 * {@link EstadoArrastre} y la compatibilidad por fila como
 * {@code IntFunction<Boolean>}, asi el renderer no conoce al panel.
 */
public final class RendererOcupacion extends DefaultTableCellRenderer {

    private static final Color FONDO_LIMPIA = new Color(0xFF, 0xFF, 0xFF);
    private static final Color FONDO_SUCIA = new Color(0xFF, 0xF3, 0xD6);
    private static final Color FONDO_GRIS = new Color(0xE8, 0xE8, 0xE8);
    private static final Color FONDO_ENCABEZADO = new Color(0xE0, 0xE0, 0xE0);
    private static final Color FONDO_SELECCION = new Color(0x9E, 0xC5, 0xFF);
    private static final Color COLOR_BARRA = new Color(0xE5, 0x73, 0x73);
    private static final Color COLOR_BARRA_GRIS = new Color(0xB5, 0xB5, 0xB5);
    private static final Color COLOR_PREVIEW = new Color(0xBD, 0xBD, 0xBD, 180);
    private static final Color COLOR_BORDE_TURNOVER = new Color(0x9E, 0x9E, 0x9E);
    private static final Color COLOR_GRILLA = new Color(0xC0, 0xC0, 0xC0);

    private static final int MARGEN_VERTICAL = 4;

    private final EstadoArrastre estado;
    private final IntFunction<Boolean> esCompatible;

    private int filaActual = -1;
    private int columnaActual = -1;
    private GeometriaBarra.Segmento segmentoHead;
    private GeometriaBarra.Segmento segmentoTail;
    private GeometriaBarra.Segmento segmentoPreview;
    private GeometriaBarra.Segmento segmentoCreacion;
    private boolean bordeTurnover;
    private boolean filaGris;

    public RendererOcupacion(EstadoArrastre estado, IntFunction<Boolean> esCompatible) {
        this.estado = estado == null ? new EstadoArrastre() : estado;
        this.esCompatible = esCompatible == null ? fila -> true : esCompatible;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component componente = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        filaActual = row;
        columnaActual = column;
        segmentoHead = null;
        segmentoTail = null;
        segmentoPreview = null;
        segmentoCreacion = null;
        bordeTurnover = false;
        filaGris = false;

        boolean enMovimiento = estado.esMovimiento();
        boolean esOrigen = enMovimiento && row == estado.filaOrigen();
        boolean compatible = esCompatible.apply(row);
        boolean gris = enMovimiento && !esOrigen && !compatible;

        if (column == 0) {
            // La columna de habitacion tambien se grisea completa durante un movimiento.
            if (gris) {
                componente.setBackground(FONDO_GRIS);
                filaGris = true;
            } else {
                componente.setBackground(FONDO_ENCABEZADO);
            }
            componente.setForeground(table.getForeground());
            return componente;
        }

        setText("");

        if (isSelected) {
            componente.setBackground(FONDO_SELECCION);
            componente.setForeground(table.getForeground());
            return componente;
        }

        boolean seleccionRango = estado.esCeldaSeleccionada(row, column);

        if (seleccionRango) {
            componente.setBackground(FONDO_SELECCION);
        } else if (gris) {
            componente.setBackground(FONDO_GRIS);
        } else if (table.getModel() instanceof CalendarioOcupacionModel modelo) {
            componente.setBackground(modelo.limpiaDeFila(row) ? FONDO_LIMPIA : FONDO_SUCIA);
        } else {
            componente.setBackground(FONDO_LIMPIA);
        }
        componente.setForeground(table.getForeground());

        if (table.getModel() instanceof CalendarioOcupacionModel modelo) {
            LocalDate fecha = modelo.fechaDeColumna(column);
            if (fecha == null) {
                return componente;
            }
            if (seleccionRango) {
                // Barra fantasma de la creacion en curso (rango en arrastre).
                int min = Math.min(estado.colIni(), estado.colFin());
                int max = Math.max(estado.colIni(), estado.colFin());
                LocalDate fechaIni = modelo.fechaDeColumna(min);
                LocalDate fechaMax = modelo.fechaDeColumna(max);
                LocalDate fechaFin = fechaMax == null ? null : fechaMax.plusDays(1);
                segmentoCreacion = estado.previewCreacion(row, column, fecha, fechaIni, fechaFin);
            } else {
                // El gate de compatibilidad del preview vive en EstadoArrastre:
                // una fila incompatible devuelve null, asi que se pinta la barra real.
                GeometriaBarra.Segmento preview = row == estado.filaHover()
                        ? estado.previewSegmento(fecha, compatible)
                        : null;
                if (preview != null) {
                    segmentoPreview = preview;
                } else {
                    segmentoTail = modelo.segmentoSalidaEn(row, column);
                    segmentoHead = modelo.segmentoEn(row, column);
                    bordeTurnover = segmentoTail != null && segmentoHead != null;
                    filaGris = gris;
                }
            }
        }
        return componente;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (columnaActual > 0) {
                int ancho = getWidth();
                int alto = Math.max(1, getHeight() - 2 * MARGEN_VERTICAL);

                if (segmentoCreacion != null) {
                    // La creacion en curso tapa cualquier barra real de la celda.
                    pintarSegmento(g2, segmentoCreacion, ancho, alto, COLOR_PREVIEW);
                } else if (segmentoPreview != null) {
                    pintarSegmento(g2, segmentoPreview, ancho, alto, COLOR_PREVIEW);
                } else {
                    Color colorBarra = filaGris ? COLOR_BARRA_GRIS : COLOR_BARRA;
                    if (segmentoTail != null) {
                        pintarSegmento(g2, segmentoTail, ancho, alto, colorBarra);
                    }
                    if (segmentoHead != null) {
                        pintarSegmento(g2, segmentoHead, ancho, alto, colorBarra);
                    }
                    if (bordeTurnover) {
                        g2.setColor(COLOR_BORDE_TURNOVER);
                        g2.fillRect(ancho / 4, MARGEN_VERTICAL, 1, alto);
                    }
                }
            }
            pintarGrilla(g2);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Grilla de separacion: una linea horizontal al pie de TODA celda (incluida
     * la columna 0) y una vertical al borde derecho de cada columna de dia.
     */
    private void pintarGrilla(Graphics2D g2) {
        g2.setColor(COLOR_GRILLA);
        g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        if (columnaActual >= 1) {
            g2.fillRect(getWidth() - 1, 0, 1, getHeight());
        }
    }

    private void pintarSegmento(Graphics2D g2, GeometriaBarra.Segmento seg, int ancho, int alto, Color color) {
        int x0 = (int) Math.round(seg.x0() * ancho);
        int x1 = (int) Math.round(seg.x1() * ancho);
        g2.setColor(color);
        g2.fillRect(x0, MARGEN_VERTICAL, Math.max(0, x1 - x0), alto);
    }
}
