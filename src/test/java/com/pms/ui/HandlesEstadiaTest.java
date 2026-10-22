package com.pms.ui;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas de las flechas de redimension: SIEMPRE dos por extremo visible (una
 * hacia adentro y una hacia afuera), sin importar la cantidad de noches ni si
 * hay una reserva pegada. Ademas, la geometria de las placas y los triangulos.
 */
class HandlesEstadiaTest {

    @Test
    void siempreHayDosFlechasPorExtremoVisible() {
        HandlesEstadia.Flechas f = HandlesEstadia.flechas(true, true);

        assertTrue(f.alargarInicio());
        assertTrue(f.acortarInicio());
        assertTrue(f.acortarFin());
        assertTrue(f.alargarFin());
    }

    @Test
    void unExtremoFueraDeLaVentanaNoMuestraFlechas() {
        HandlesEstadia.Flechas f = HandlesEstadia.flechas(true, false);

        assertTrue(f.alargarInicio());
        assertTrue(f.acortarInicio());
        assertFalse(f.acortarFin());
        assertFalse(f.alargarFin());
    }

    @Test
    void sinExtremosVisiblesNoHayFlechas() {
        HandlesEstadia.Flechas f = HandlesEstadia.flechas(false, false);

        assertFalse(f.alguna());
    }

    @Test
    void geometriaDeLasPlacasYLosCuatroTriangulos() {
        Rectangle celdaIni = new Rectangle(110, 0, 56, 26);
        Rectangle barraIni = new Rectangle(126, 4, 40, 18);
        Rectangle celdaFin = new Rectangle(166, 0, 56, 26);
        Rectangle barraFin = new Rectangle(166, 4, 12, 18);
        HandlesEstadia.Flechas f = new HandlesEstadia.Flechas(true, true, true, true);

        List<HandlesEstadia.Zona> zonas =
                HandlesEstadia.zonas(celdaIni, barraIni, celdaFin, barraFin, f);

        assertEquals(4, zonas.size());
        assertEquals(2, zonas.stream()
                .filter(z -> z.borde() == HandlesEstadia.Borde.INICIO).count());
        assertEquals(2, zonas.stream()
                .filter(z -> z.borde() == HandlesEstadia.Borde.FIN).count());
        int anchoInicio = zonas.stream()
                .filter(z -> z.borde() == HandlesEstadia.Borde.INICIO)
                .mapToInt(z -> z.rect().width).sum();
        int anchoFin = zonas.stream()
                .filter(z -> z.borde() == HandlesEstadia.Borde.FIN)
                .mapToInt(z -> z.rect().width).sum();
        assertEquals(anchoInicio, anchoFin, "ambas placas deben medir lo mismo");
        for (HandlesEstadia.Zona z : zonas) {
            assertTrue(z.rect().width > 0);
            assertTrue(z.rect().height > 0);
        }
        HandlesEstadia.Zona alargarInicio = zonas.stream()
                .filter(z -> z.borde() == HandlesEstadia.Borde.INICIO
                        && z.direccion() == HandlesEstadia.Direccion.ALARGAR)
                .findFirst().orElseThrow();
        assertTrue(alargarInicio.haciaIzquierda(), "extender el check-in apunta a la izquierda");
        HandlesEstadia.Zona acortarFin = zonas.stream()
                .filter(z -> z.borde() == HandlesEstadia.Borde.FIN
                        && z.direccion() == HandlesEstadia.Direccion.ACORTAR)
                .findFirst().orElseThrow();
        assertTrue(acortarFin.haciaIzquierda(), "acortar el check-out apunta a la izquierda");
    }

    @Test
    void elPrimerDiaSeGanaAlSuperarEl25PorCientoDelAncho() {
        int ancho = 56; // 25% = 14 px
        assertEquals(0, CalendarioPanel.diasRedimension(10, ancho, HandlesEstadia.Borde.FIN));
        assertEquals(1, CalendarioPanel.diasRedimension(15, ancho, HandlesEstadia.Borde.FIN));
        assertEquals(1, CalendarioPanel.diasRedimension(60, ancho, HandlesEstadia.Borde.FIN));
        assertEquals(2, CalendarioPanel.diasRedimension(80, ancho, HandlesEstadia.Borde.FIN));
        assertEquals(-1, CalendarioPanel.diasRedimension(-15, ancho, HandlesEstadia.Borde.FIN));
        assertEquals(1, CalendarioPanel.diasRedimension(-15, ancho, HandlesEstadia.Borde.INICIO));
    }

    @Test
    void unaSolaFlechaOcupaTodaLaPlacaYSePuedeGolpear() {
        Rectangle celdaIni = new Rectangle(0, 0, 56, 26);
        Rectangle barraIni = new Rectangle(16, 4, 40, 18);
        Rectangle celdaFin = new Rectangle(56, 0, 56, 26);
        Rectangle barraFin = new Rectangle(56, 4, 12, 18);
        HandlesEstadia.Flechas f = new HandlesEstadia.Flechas(false, false, true, false);

        List<HandlesEstadia.Zona> zonas =
                HandlesEstadia.zonas(celdaIni, barraIni, celdaFin, barraFin, f);

        assertEquals(1, zonas.size());
        HandlesEstadia.Zona zona = zonas.get(0);
        assertEquals(HandlesEstadia.Borde.FIN, zona.borde());
        assertEquals(HandlesEstadia.Direccion.ACORTAR, zona.direccion());
        assertNotNull(HandlesEstadia.en(zonas, zona.rect().x + 1, zona.rect().y + 1));
        assertNull(HandlesEstadia.en(zonas, 1000, 1000));
    }
}
