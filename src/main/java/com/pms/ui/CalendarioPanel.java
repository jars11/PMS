package com.pms.ui;

import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.controllers.ControladorReservas.ResultadoCompatibilidad;
import com.pms.controllers.ControladorReservas.ResultadoMovimiento;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.utils.MotivoRechazo;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pestana del calendario nocturno. Muestra una {@link JTable} con el
 * {@link CalendarioOcupacionModel} y navega con el selector de fecha de inicio
 * (mas Shift + rueda). La cantidad de noches visibles se adapta al ancho
 * disponible de la ventana.
 *
 * <p>La interaccion con el raton la gobierna {@link EstadoArrastre}: un press
 * sobre una noche libre arranca una seleccion de rango; un press sobre una noche
 * ocupada arranca un MOVIMIENTO del bloque clickeado. Durante un movimiento el
 * renderer pinta en gris las filas incompatibles y la reserva movida en su color
 * original sobre la fila permitida mas cercana al cursor (las filas grises no
 * cuentan). Al soltar se llama a {@code moverReserva} sobre esa fila; un rechazo
 * muestra el mensaje exacto mas el detalle del motivo, y un drop sin destino
 * cancela en silencio. El click derecho abre el menu contextual de siempre.
 */
public final class CalendarioPanel extends JPanel {

    private static final int ANCHO_HABITACION = 110;
    private static final int ANCHO_NOCHE = 56;
    private static final int TAM_BOTON_CUADRADO = 28;
    private static final int UMBRAL_ARRASTRE_PX = 5;

    private final ControladorReservas controlador = ControladorReservas.getInstance();
    private final TablaCalendario tabla;
    private final JScrollPane scroll;
    private final JSpinner spinnerInicio = new JSpinner(
            new SpinnerDateModel(aFecha(LocalDate.now()), null, null, Calendar.DAY_OF_MONTH));

    /** Estado del arrastre en curso (rango o movimiento). */
    private final EstadoArrastre estado = new EstadoArrastre();

    /** Compatibilidad cacheada por habitacion para el movimiento en curso. */
    private final Map<Long, MotivoRechazo> motivoPorHabitacion = new HashMap<>();

    /** Contexto del movimiento en curso; null cuando no hay uno. */
    private Long reservaOrigenId;
    private Long habitacionOrigenId;
    private LocalDate nocheOrigen;

    /**
     * Press sobre una reserva que todavia no se convirtio en movimiento: un
     * click simple selecciona la reserva (y muestra sus flechas), un arrastre
     * pasado el umbral arranca el movimiento.
     */
    private boolean presionPendiente;
    private int filaPresion = -1;
    private int columnaPresion = -1;
    private Point puntoPresion;
    private OcupacionHabitacion.BloqueNoche bloquePresion;

    /** Extremo real de la barra y ancho de celda usados por el umbral del 25%. */
    private int bordeXRedimension = -1;
    private int anchoPasoRedimension = ANCHO_NOCHE;

    private final MouseAdapter raton = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                mostrarMenu(e);
                return;
            }
            if (SwingUtilities.isLeftMouseButton(e)) {
                iniciarInteraccion(e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (estado.esRedimension()) {
                actualizarRedimension(e);
                return;
            }
            if (estado.esMovimiento()) {
                estado.hover(filaEn(e));
                actualizarDestino();
                tabla.repaint();
                return;
            }
            if (presionPendiente) {
                if (puntoPresion == null
                        || e.getPoint().distance(puntoPresion) < UMBRAL_ARRASTRE_PX) {
                    return;
                }
                presionPendiente = false;
                if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
                    return;
                }
                iniciarMovimiento(modelo, filaPresion, columnaPresion, bloquePresion);
                if (estado.esMovimiento()) {
                    estado.hover(filaEn(e));
                    actualizarDestino();
                    tabla.repaint();
                }
                return;
            }
            if (estado.tipo() == EstadoArrastre.Tipo.RANGO) {
                extenderRango(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                mostrarMenu(e);
                return;
            }
            if (estado.esRedimension()) {
                finalizarRedimension();
                return;
            }
            if (presionPendiente) {
                // Click simple: la reserva queda seleccionada con sus flechas.
                presionPendiente = false;
                if (bloquePresion != null && !estado.haySeleccion()
                        && tabla.getModel() instanceof CalendarioOcupacionModel modelo) {
                    seleccionarReserva(modelo, filaPresion, bloquePresion);
                }
                bloquePresion = null;
                tabla.repaint();
                return;
            }
            if (!estado.activo()) {
                return;
            }
            if (estado.esMovimiento()) {
                finalizarMovimiento(e);
            } else {
                finalizarRango(e);
            }
        }
    };

    public CalendarioPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tabla = new TablaCalendario(estado);
        // Los dias son columnas fijas: reordenarlas por drag rompe la lectura
        // del calendario (los numeros se movian y las reservas no).
        tabla.getTableHeader().setReorderingAllowed(false);
        // Las columnas tampoco se redimensionan arrastrando el borde.
        tabla.getTableHeader().setResizingAllowed(false);
        tabla.addMouseListener(raton);
        tabla.addMouseMotionListener(raton);

        add(construirBarraNavegacion(), BorderLayout.NORTH);
        scroll = new JScrollPane(tabla);
        // Shift + rueda navega dia a dia (rueda arriba = dia anterior).
        // La rueda normal desplaza las filas aunque el mouse este sobre
        // cualquier celda del calendario, no solo sobre la barra derecha.
        tabla.addMouseWheelListener(e -> {
            if (e.isShiftDown()) {
                mover(e.getWheelRotation() < 0 ? -1 : 1);
                e.consume();
                return;
            }
            JScrollBar barra = scroll.getVerticalScrollBar();
            if (barra != null && barra.isVisible()) {
                int paso = Math.max(3 * tabla.getRowHeight(), barra.getUnitIncrement());
                barra.setValue(barra.getValue() + e.getWheelRotation() * paso);
                e.consume();
            }
        });
        // La cantidad de dias visibles se adapta al ancho de la ventana: al
        // agrandar o achicar, se recalculan las columnas.
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarNochesAlAncho();
            }
        });
        add(scroll, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                ajustarNochesAlAncho();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                ajustarNochesAlAncho();
            }
        });

        refrescar();
        // El tamano real recien existe despues del layout del frame.
        SwingUtilities.invokeLater(this::ajustarNochesAlAncho);
    }

    private JPanel construirBarraNavegacion() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        spinnerInicio.setEditor(new JSpinner.DateEditor(spinnerInicio, "dd/MM/yyyy"));
        spinnerInicio.addChangeListener(e -> refrescar());

        JButton refrescar = new JButton(new IconoRefrescar());
        refrescar.setToolTipText("Refrescar");
        refrescar.setFocusable(false);
        refrescar.setMargin(new Insets(0, 0, 0, 0));
        refrescar.setPreferredSize(new Dimension(TAM_BOTON_CUADRADO, TAM_BOTON_CUADRADO));
        refrescar.setMinimumSize(new Dimension(TAM_BOTON_CUADRADO, TAM_BOTON_CUADRADO));
        refrescar.setMaximumSize(new Dimension(TAM_BOTON_CUADRADO, TAM_BOTON_CUADRADO));
        refrescar.addActionListener(e -> refrescar());

        barra.add(new JLabel("Inicio:"));
        barra.add(spinnerInicio);
        barra.add(refrescar);
        return barra;
    }

    /** Icono cuadrado de refresco: flecha circular dibujada, sin depender de la fuente. */
    private static final class IconoRefrescar implements Icon {

        private static final int TAM = 16;

        @Override
        public int getIconWidth() {
            return TAM;
        }

        @Override
        public int getIconHeight() {
            return TAM;
        }

        @Override
        public void paintIcon(Component componente, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setColor(componente == null || componente.getForeground() == null
                        ? new Color(0x40, 0x40, 0x40) : componente.getForeground());
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(2, 2, TAM - 4, TAM - 4, 60, 280);
                // Punta de flecha en el extremo superior derecho del arco.
                g2.fillPolygon(new int[]{TAM - 6, TAM - 1, TAM - 5},
                        new int[]{1, 4, 7}, 3);
            } finally {
                g2.dispose();
            }
        }
    }

    private void mover(int dias) {
        spinnerInicio.setValue(aFecha(inicioActual().plusDays(dias)));
    }

    // ------------------------------------------------------- estado del raton

    private int filaEn(MouseEvent e) {
        int fila = tabla.rowAtPoint(e.getPoint());
        return (fila < 0 || fila >= tabla.getRowCount()) ? -1 : fila;
    }

    private void iniciarInteraccion(MouseEvent e) {
        // 1) ¿Se apreto una flecha de la reserva seleccionada?
        if (estado.haySeleccion()) {
            HandlesEstadia.Zona zona = tabla.zonaDeHandle(e.getPoint());
            if (zona != null) {
                iniciarRedimension(zona, e);
                return;
            }
        }

        int fila = filaEn(e);
        int columna = tabla.columnAtPoint(e.getPoint());
        boolean modeloValido = tabla.getModel() instanceof CalendarioOcupacionModel;
        CalendarioOcupacionModel modelo = modeloValido
                ? (CalendarioOcupacionModel) tabla.getModel() : null;

        if (fila < 0 || columna <= 0 || modelo == null) {
            // Click fuera de las celdas: si habia una reserva seleccionada,
            // solo se ocultan sus flechas.
            quitarSeleccionReserva();
            limpiarSeleccion();
            return;
        }

        OcupacionHabitacion.BloqueNoche bloque =
                tabla.bloqueEnPunto(fila, columna, e.getPoint());

        if (estado.haySeleccion()) {
            if (esElBloqueSeleccionado(bloque, fila)) {
                // Press sobre la misma reserva: queda lista para un arrastre.
                prepararPresion(fila, columna, e, bloque);
                return;
            }
            // Segundo click fuera del rango seleccionado: solo desaparecen las
            // flechas. No crea, no mueve, no selecciona otra reserva.
            quitarSeleccionReserva();
            limpiarSeleccion();
            return;
        }

        if (bloque != null) {
            // La reserva se selecciona al SOLTAR (click), no al presionar: asi
            // las flechas no aparecen mientras se arrastra para moverla.
            prepararPresion(fila, columna, e, bloque);
            return;
        }

        if (modelo.estaOcupada(fila, columna)) {
            // Franja blanca de una celda ocupada: no hace nada.
            limpiarSeleccion();
            return;
        }

        // Casillero libre: arranca un rango para crear arrastrando. Un click
        // sin cambio de casillero no crea nada (se descarta al soltar).
        limpiarSeleccion();
        estado.iniciarRango(fila, columna, tabla.getColumnCount() - 1);
        tabla.repaint();
    }

    private void prepararPresion(int fila, int columna, MouseEvent e,
                                 OcupacionHabitacion.BloqueNoche bloque) {
        presionPendiente = true;
        filaPresion = fila;
        columnaPresion = columna;
        bloquePresion = bloque;
        puntoPresion = e.getPoint();
    }

    /** Selecciona la reserva clickeada (cabeza, interior o cola de check-out). */
    private void seleccionarReserva(CalendarioOcupacionModel modelo, int fila,
                                    OcupacionHabitacion.BloqueNoche bloque) {
        if (bloque == null || bloque.fechaInicio() == null || bloque.fechaFin() == null) {
            quitarSeleccionReserva();
            return;
        }
        GeometriaBarra.Bloque geometria = new GeometriaBarra.Bloque(
                bloque.fechaInicio(), bloque.fechaFin(), bloque.lateCheckout());
        int ultima = modelo.getColumnCount() - 1;
        int colIni = modelo.columnaDeFecha(bloque.fechaInicio());
        int colFin = modelo.columnaDeFecha(bloque.fechaFin());
        boolean inicioVisible = colIni >= 1 && colIni <= ultima;
        boolean finVisible = colFin >= 1 && colFin <= ultima;
        HandlesEstadia.Flechas flechas = HandlesEstadia.flechas(inicioVisible, finVisible);
        estado.seleccionar(fila, bloque.reservaId(), geometria, flechas);
        tabla.repaint();
    }

    private boolean esElBloqueSeleccionado(OcupacionHabitacion.BloqueNoche bloque, int fila) {
        if (bloque == null || !estado.haySeleccion() || fila != estado.filaSeleccionada()) {
            return false;
        }
        GeometriaBarra.Bloque seleccionado = estado.bloqueSeleccionado();
        return seleccionado != null
                && seleccionado.fechaInicio().equals(bloque.fechaInicio())
                && seleccionado.fechaFin().equals(bloque.fechaFin())
                && estado.reservaSeleccionadaId() != null
                && estado.reservaSeleccionadaId() == bloque.reservaId();
    }

    private void quitarSeleccionReserva() {
        if (!estado.haySeleccion()) {
            return;
        }
        estado.limpiarSeleccionReserva();
        tabla.repaint();
    }

    private void iniciarRedimension(HandlesEstadia.Zona zona, MouseEvent e) {
        estado.iniciarRedimension(zona.borde(), zona.direccion());
        bordeXRedimension = tabla.edgeXDeHandle(zona.borde());
        if (bordeXRedimension < 0) {
            bordeXRedimension = e.getX();
        }
        int columnaBorde = -1;
        if (tabla.getModel() instanceof CalendarioOcupacionModel modelo
                && estado.bloqueSeleccionado() != null) {
            GeometriaBarra.Bloque seleccionado = estado.bloqueSeleccionado();
            LocalDate fecha = zona.borde() == HandlesEstadia.Borde.INICIO
                    ? seleccionado.fechaInicio() : seleccionado.fechaFin();
            columnaBorde = modelo.columnaDeFecha(fecha);
        }
        anchoPasoRedimension = columnaBorde >= 1
                ? tabla.getCellRect(estado.filaSeleccionada(), columnaBorde, false).width
                : ANCHO_NOCHE;
        presionPendiente = false;
        bloquePresion = null;
        tabla.repaint();
    }

    /**
     * Traduce el arrastre de una flecha a un rango nuevo. El ancho cambia en
     * cuanto el mouse supera el 25% del ancho de la celda, sin necesidad de
     * entrar en la celda siguiente. Alargar se detiene en la primera noche
     * ocupada o en el borde de la ventana; acortar deja al menos una noche.
     */
    private void actualizarRedimension(MouseEvent e) {
        if (!estado.esRedimension() || estado.bloqueSeleccionado() == null
                || bordeXRedimension < 0
                || !(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        int fila = estado.filaSeleccionada();
        int ultima = modelo.getColumnCount() - 1;
        int deltaX = e.getX() - bordeXRedimension;
        int dias = diasRedimension(deltaX, anchoPasoRedimension,
                estado.bordeRedimension());

        LocalDate ini = estado.bloqueSeleccionado().fechaInicio();
        LocalDate fin = estado.bloqueSeleccionado().fechaFin();
        long noches = ChronoUnit.DAYS.between(ini, fin);

        if (estado.bordeRedimension() == HandlesEstadia.Borde.INICIO) {
            if (dias > 0) {
                // Agranda hacia atras (check-in mas temprano).
                int max = 0;
                while (max < dias) {
                    LocalDate noche = ini.minusDays(max + 1);
                    int col = modelo.columnaDeFecha(noche);
                    if (col < 1 || col > ultima || modelo.estaOcupada(fila, col)) {
                        break;
                    }
                    max++;
                }
                ini = ini.minusDays(max);
            } else if (dias < 0) {
                // Achica desde el check-in.
                int max = Math.max(0, (int) noches - 1);
                ini = ini.plusDays(Math.min(-dias, max));
            }
        } else {
            if (dias > 0) {
                // Agranda hacia adelante (check-out mas tarde).
                int max = 0;
                while (max < dias) {
                    LocalDate noche = fin.plusDays(max);
                    int col = modelo.columnaDeFecha(noche);
                    if (col < 1 || col > ultima || modelo.estaOcupada(fila, col)) {
                        break;
                    }
                    max++;
                }
                fin = fin.plusDays(max);
            } else if (dias < 0) {
                // Achica desde el check-out.
                int max = Math.max(0, (int) noches - 1);
                fin = fin.minusDays(Math.min(-dias, max));
            }
        }
        if (ini.isBefore(fin)) {
            estado.previewRedimension(ini, fin);
            tabla.repaint();
        }
    }

    /**
     * Dias ganados/perdidos por el arrastre. El primer dia se gana al superar
     * el 25% del ancho de la celda; cada dia adicional pide un ancho completo.
     * Devuelve positivo cuando la estadia crece y negativo cuando se acorta.
     */
    static int diasRedimension(int deltaX, int anchoCelda,
                               HandlesEstadia.Borde borde) {
        int signo = borde == HandlesEstadia.Borde.INICIO ? -1 : 1;
        int avance = deltaX * signo; // positivo = agrandar
        int ancho = Math.max(1, anchoCelda);
        int umbral = Math.max(1, ancho / 4);
        if (Math.abs(avance) <= umbral) {
            return 0;
        }
        int dias = 1 + (Math.abs(avance) - umbral) / ancho;
        return avance > 0 ? dias : -dias;
    }

    /** Persiste el rango previsualizado (si cambio) y refresca el calendario. */
    private void finalizarRedimension() {
        if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            estado.terminarRedimension();
            return;
        }
        GeometriaBarra.Bloque original = estado.bloqueSeleccionado();
        LocalDate nuevoInicio = estado.inicioRedimension();
        LocalDate nuevoFin = estado.finRedimension();
        int fila = estado.filaSeleccionada();
        Long reservaId = estado.reservaSeleccionadaId();
        Habitacion habitacion = fila >= 0 && fila < modelo.getRowCount()
                ? modelo.habitacionDeFila(fila) : null;
        estado.terminarRedimension();

        boolean cambio = original != null && nuevoInicio != null && nuevoFin != null
                && (!nuevoInicio.equals(original.fechaInicio())
                || !nuevoFin.equals(original.fechaFin()));
        if (!cambio || reservaId == null || habitacion == null || habitacion.getId() == null) {
            tabla.repaint();
            return;
        }
        try {
            controlador.redimensionarReserva(reservaId, habitacion.getId(),
                    original.fechaInicio(), nuevoInicio, nuevoFin);
            refrescar();
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
            refrescar();
        }
    }

    private void iniciarMovimiento(CalendarioOcupacionModel modelo, int fila, int columna,
                                   OcupacionHabitacion.BloqueNoche bloque) {
        if (bloque == null || bloque.reservaId() <= 0) {
            limpiarSeleccion();
            return;
        }
        Long reservaId = bloque.reservaId();
        // Una noche perteneciente a la propuesta (sirve tambien si el click fue
        // sobre la cola de check-out, que no ocupa la noche).
        LocalDate fecha = bloque.fechaInicio();
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (habitacion == null || habitacion.getId() == null || fecha == null) {
            limpiarSeleccion();
            return;
        }

        reservaOrigenId = reservaId;
        habitacionOrigenId = habitacion.getId();
        nocheOrigen = fecha;
        // Al empezar a moverla, las flechas de seleccion dejan de mostrarse.
        estado.limpiarSeleccionReserva();
        motivoPorHabitacion.clear();
        try {
            for (ResultadoCompatibilidad rc
                    : controlador.compatibilidadMovimiento(reservaId, habitacion.getId(), fecha)) {
                motivoPorHabitacion.put(rc.habitacionId(), rc.motivo());
            }
        } catch (RuntimeException ex) {
            limpiarSeleccion();
            mostrarError(ex.getMessage());
            return;
        }

        estado.iniciarMovimiento(fila, columna, new GeometriaBarra.Bloque(
                bloque.fechaInicio(), bloque.fechaFin(), bloque.lateCheckout()), reservaId);
        tabla.repaint();
    }

    private void extenderRango(MouseEvent e) {
        if (filaEn(e) != estado.filaOrigen()) {
            return; // solo se extiende dentro de la misma habitacion
        }
        int noches = tabla.getColumnCount() - 1;
        int columna = Math.max(1, Math.min(noches, tabla.columnAtPoint(e.getPoint())));
        if (columna != estado.colFin()) {
            estado.extenderRango(columna);
            tabla.repaint();
        }
    }

    private void finalizarRango(MouseEvent e) {
        // Un click sin cambio de casillero no crea nada.
        if (!estado.rangoExtendido()) {
            limpiarSeleccion();
            return;
        }
        int fila = estado.filaOrigen();
        int cIni = estado.seleccionIni();
        int cFin = estado.seleccionFin();

        if (fila < 0 || cIni < 0 || cFin < 0
                || !(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            limpiarSeleccion();
            return;
        }
        SeleccionCalendario.Rango rango = SeleccionCalendario.rango(modelo, cIni, cFin);
        if (rango == null) {
            limpiarSeleccion();
            return;
        }
        if (!SeleccionCalendario.esRangoLibre(modelo, fila, cIni, cFin)) {
            limpiarSeleccion();
            DialogoError.mostrar(this, "Selección no válida",
                    "El rango cruza una noche ocupada. Elegí noches libres.");
            return;
        }
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (habitacion == null) {
            limpiarSeleccion();
            return;
        }
        if (habitacion.getId() != null
                && controlador.hayLateCheckoutElDia(habitacion.getId(), rango.checkIn())) {
            JOptionPane.showMessageDialog(this,
                    "La habitación tiene un late check-out el día del check-in. "
                            + "Coordiná la limpieza antes del ingreso.",
                    "Aviso: late check-out", JOptionPane.WARNING_MESSAGE);
        }
        // El rango queda pintado mientras el dialogo esta abierto: el usuario ve
        // sobre que noches esta creando la reserva. Se limpia al cerrar.
        DialogoReserva dialogo = DialogoReserva.paraAlta(ventana(), habitacion, rango.checkIn(), rango.checkOut());
        boolean creada = dialogo.esAceptado();
        limpiarSeleccion();
        if (creada) {
            refrescar();
        }
    }

    private void finalizarMovimiento(MouseEvent e) {
        int filaOrigen = estado.filaOrigen();
        int filaDestino = estado.filaDestino();
        Long reservaId = reservaOrigenId;
        Long origen = habitacionOrigenId;
        LocalDate noche = nocheOrigen;
        limpiarSeleccion();

        if (reservaId == null || origen == null || noche == null) {
            return;
        }
        // Sin destino permitido visible (o soltado sobre el origen): cancelar.
        if (filaDestino < 0 || filaDestino == filaOrigen) {
            return;
        }
        if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        Habitacion destino = modelo.habitacionDeFila(filaDestino);
        if (destino == null || destino.getId() == null) {
            return;
        }

        ResultadoMovimiento resultado =
                controlador.moverReserva(reservaId, origen, noche, destino.getId());
        if (resultado.movida()) {
            refrescar();
        } else {
            DialogoError.mostrar(this, "No se pudo mover",
                    new Object[]{MensajesMovimiento.RECHAZO_MOVIMIENTO,
                            MensajesMovimiento.detalle(resultado.motivo())});
        }
    }

    /** True cuando la habitacion de la fila no tiene motivo de rechazo en el movimiento actual. */
    private boolean esCompatibleFila(int fila) {
        if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return true;
        }
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (habitacion == null || habitacion.getId() == null) {
            return true;
        }
        return motivoPorHabitacion.get(habitacion.getId()) == null;
    }

    /**
     * Fija como destino la fila que esta bajo el mouse, sin saltar filas
     * inhabilitadas: si el cursor esta en el origen, la reserva se queda ahi; si
     * esta en una fila gris, el preview se muestra ahi y el drop la rechaza.
     */
    private void actualizarDestino() {
        if (!estado.esMovimiento()) {
            estado.destino(-1);
            return;
        }
        estado.destino(DestinoMovimiento.resolver(estado.filaHover(),
                estado.filaOrigen(), tabla.getRowCount(), this::esCompatibleFila));
    }

    private void limpiarSeleccion() {
        estado.limpiar();
        motivoPorHabitacion.clear();
        reservaOrigenId = null;
        habitacionOrigenId = null;
        nocheOrigen = null;
        tabla.clearSelection();
        tabla.repaint();
    }

    // ---------------------------------------------------------- menu contextual

    private void mostrarMenu(MouseEvent e) {
        int fila = tabla.rowAtPoint(e.getPoint());
        int columna = tabla.columnAtPoint(e.getPoint());
        if (fila < 0 || fila >= tabla.getRowCount()
                || !(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        SeleccionCalendario.Tipo tipo = SeleccionCalendario.accionDe(modelo, fila, columna);
        if (tipo == SeleccionCalendario.Tipo.NADA) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        if (tipo == SeleccionCalendario.Tipo.EDITAR) {
            menu.add(menuItem("Editar reserva", () -> editarReserva(modelo, fila, columna)));
            menu.add(menuItem("Eliminar reserva", () -> eliminarReserva(modelo, fila, columna)));
        } else {
            menu.add(menuItem("Nueva reserva aquí", () -> nuevaReservaAqui(modelo, fila, columna)));
        }
        menu.show(tabla, e.getX(), e.getY());
    }

    private void editarReserva(CalendarioOcupacionModel modelo, int fila, int columna) {
        Long reservaId = modelo.reservaEn(fila, columna);
        LocalDate noche = modelo.fechaDeColumna(columna);
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (reservaId == null || noche == null || habitacion == null || habitacion.getId() == null) {
            return;
        }
        try {
            ControladorReservas.EdicionReserva edicion =
                    controlador.edicionDe(reservaId, habitacion.getId(), noche);
            DialogoReserva dialogo = DialogoReserva.paraEdicion(ventana(), edicion);
            if (dialogo.esAceptado()) {
                refrescar();
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void eliminarReserva(CalendarioOcupacionModel modelo, int fila, int columna) {
        Long reservaId = modelo.reservaEn(fila, columna);
        if (reservaId == null) {
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this, "¿Eliminar la reserva #" + reservaId + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean eliminada = controlador.eliminarReserva(reservaId);
            if (!eliminada) {
                DialogoError.mostrar(this, "No se pudo eliminar",
                        "No se puede eliminar: la reserva tiene pagos asociados.");
            }
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
        refrescar();
    }

    private void nuevaReservaAqui(CalendarioOcupacionModel modelo, int fila, int columna) {
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        LocalDate noche = modelo.fechaDeColumna(columna);
        if (habitacion == null || noche == null) {
            return;
        }
        DialogoReserva dialogo = DialogoReserva.paraAlta(ventana(), habitacion, noche);
        if (dialogo.esAceptado()) {
            refrescar();
        }
    }

    // ------------------------------------------------------- gestion de habitacion

    /** Reconstruye la ventana visible y reemplaza el modelo. */
    public void refrescar() {
        LocalDate desde = inicioActual();
        int ancho = anchoDisponible();
        int noches = ColumnasCalendario.nochesVisibles(ancho, ANCHO_HABITACION, ANCHO_NOCHE,
                CalendarioOcupacionModel.NOCHES_POR_DEFECTO);
        LocalDate hasta = desde.plusDays(noches);
        List<OcupacionHabitacion> ocupacion = controlador.matrizOcupacion(desde, hasta);
        tabla.setModel(new CalendarioOcupacionModel(desde, noches, ocupacion));
        tabla.limpiarHover();
        tabla.setDefaultRenderer(Object.class, new RendererOcupacion(estado, this::esCompatibleFila));
        aplicarAnchoColumnas(ColumnasCalendario.anchoNoche(ancho, noches, ANCHO_HABITACION, ANCHO_NOCHE), ancho);
        limpiarSeleccion();
        estado.limpiarSeleccionReserva();
    }

    /** Ancho util para las columnas: el viewport, o el panel antes del primer layout. */
    private int anchoDisponible() {
        int ancho = scroll == null ? 0 : scroll.getViewport().getWidth();
        return ancho > 0 ? ancho : getWidth();
    }

    /**
     * Al cambiar el ancho de la ventana: si cambia la cantidad de dias que
     * entran, se reconstruye el modelo; si no, solo se reparten las columnas
     * para llenar el espacio.
     */
    private void ajustarNochesAlAncho() {
        int ancho = anchoDisponible();
        int noches = ColumnasCalendario.nochesVisibles(ancho, ANCHO_HABITACION, ANCHO_NOCHE,
                CalendarioOcupacionModel.NOCHES_POR_DEFECTO);
        if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        if (noches != modelo.getColumnCount() - 1) {
            refrescar();
            return;
        }
        aplicarAnchoColumnas(ColumnasCalendario.anchoNoche(ancho, noches, ANCHO_HABITACION, ANCHO_NOCHE), ancho);
    }

    /**
     * Pinta en gris las propuestas del asistente de estadia para que se vean en
     * el calendario antes de confirmarlas. Un conjunto vacio limpia el preview.
     */
    public void previsualizarPropuestas(Collection<RHPropuesta> propuestas) {
        tabla.setPropuestas(propuestas);
    }

    /** Ancho de la columna de habitacion y de cada noche (se reparte al llenar la ventana). */
    private void aplicarAnchoColumnas(int anchoNoche, int ancho) {
        int columnas = tabla.getColumnModel().getColumnCount();
        if (columnas == 0) {
            return;
        }
        tabla.getColumnModel().getColumn(0).setPreferredWidth(ANCHO_HABITACION);
        tabla.getColumnModel().getColumn(0).setWidth(ANCHO_HABITACION);
        int noches = columnas - 1;
        int resto = Math.max(0, ancho - ANCHO_HABITACION - noches * anchoNoche);
        for (int columna = 1; columna < columnas; columna++) {
            int anchoColumna = anchoNoche + (columna - 1 < resto ? 1 : 0);
            tabla.getColumnModel().getColumn(columna).setPreferredWidth(anchoColumna);
            tabla.getColumnModel().getColumn(columna).setWidth(anchoColumna);
        }
    }

    private LocalDate inicioActual() {
        return ((Date) spinnerInicio.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static JMenuItem menuItem(String texto, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(e -> accion.run());
        return item;
    }

    private java.awt.Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void mostrarError(String mensaje) {
        DialogoError.mostrar(this, "Error", mensaje);
    }

    private static Date aFecha(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

