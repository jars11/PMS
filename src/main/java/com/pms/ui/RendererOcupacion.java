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
 * {@link GeometriaBarra}, la cola del dia de salida, el gris de las
 * habitaciones incompatibles durante un movimiento y la reserva movida pintada
 * en su color original sobre la fila apuntada (el origen se levanta apenas el
 * cursor abandona la fila).
 *
 * <p>Las barras de dias contiguos se dibujan con una separacion de
 * {@link GeometriaBarra#SEPARACION_PX} pixeles en los bordes compartidos, para
 * que dos estadias consecutivas no se lean como una sola. El estado del arrastre
 * se inyecta como {@link EstadoArrastre} y la compatibilidad por fila como
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
            boolean redimension = estado.esRedimension() && row == estado.filaSeleccionada();
            // Mientras el cursor esta fuera de su fila, la reserva movida se
            // levanta del origen: sus segmentos no se pintan ahi. Durante una
            // redimension se levanta la barra vieja y se pinta el rango nuevo.
            Long ignorar = null;
            if (enMovimiento && row == estado.filaOrigen() && estado.ocultarBloqueOrigen()) {
                ignorar = estado.reservaOrigenId();
            } else if (redimension) {
                ignorar = estado.reservaSeleccionadaId();
            }

            if (seleccionRango) {
                // Barra fantasma de la creacion en curso (rango en arrastre).
                // La ultima columna resaltada ES el dia de check-out.
                int selIni = estado.seleccionIni();
                int selFin = estado.seleccionFin();
                if (selIni >= 1 && selFin >= 1) {
                    LocalDate fechaIni = modelo.fechaDeColumna(selIni);
                    LocalDate fechaFin = modelo.fechaDeColumna(selFin);
                    if (fechaIni != null && fechaFin != null) {
                        segmentoCreacion = estado.previewCreacion(row, column, fecha, fechaIni, fechaFin);
                    }
                }
            }
            // La celda de check-out puede estar compartida con el proximo
            // huesped: se siguen pintando las barras reales.
            segmentoTail = modelo.segmentoSalidaEnIgnorando(row, column, ignorar);
            segmentoHead = modelo.segmentoEnIgnorando(row, column, ignorar);
            filaGris = gris;

            // La reserva movida se pinta en su color original sobre la fila
            // DESTINO resuelta (la permitida mas cercana), que puede no ser la
            // fila bajo el cursor si esa fila es gris.
            if (row == estado.filaDestino()) {
                segmentoPreview = estado.previewSegmento(fecha);
            }
            if (redimension) {
                segmentoPreview = estado.previewRedimensionSegmento(fecha);
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

                Color colorBarra = filaGris ? COLOR_BARRA_GRIS : COLOR_BARRA;
                if (segmentoTail != null) {
                    pintarSegmento(g2, segmentoTail, ancho, alto, colorBarra);
                }
                if (segmentoHead != null) {
                    pintarSegmento(g2, segmentoHead, ancho, alto, colorBarra);
                }
                if (segmentoPreview != null) {
                    // La reserva movida usa el MISMO color que una reserva real.
                    pintarSegmento(g2, segmentoPreview, ancho, alto, COLOR_BARRA);
                }
                if (segmentoCreacion != null) {
                    // La creacion en curso se pinta ENCIMA de la barra real: en la
                    // celda de check-out compartida conviven la cola nueva y la
                    // cabeza del proximo huesped, separadas por SEPARACION_PX.
                    pintarSegmento(g2, segmentoCreacion, ancho, alto, COLOR_BARRA);
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
        int[] px = GeometriaBarra.pixeles(seg, ancho);
        g2.setColor(color);
        g2.fillRect(px[0], MARGEN_VERTICAL, Math.max(0, px[1] - px[0]), alto);
    }
}
