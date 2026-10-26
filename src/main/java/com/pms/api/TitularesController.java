package com.pms.api;

import com.pms.controllers.ControladorPersonas;
import com.pms.entities.TitularReserva;
import com.pms.utils.TipoDocumento;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Alta y busqueda por documento de titulares (T12d). Ambas operaciones delegan
 * en {@link ControladorPersonas} (no reimplementan la persistencia): el alta usa
 * {@code altaTitular} y la busqueda {@code buscarTitularPorDocumento}.
 *
 * <p>Es el soporte del flujo del formulario web "documento primero": el
 * documento identifica al titular, {@code GET /api/titulares/buscar} lo
 * autocompleta si ya existe y, si no existe (404), el formulario junta los datos
 * y los da de alta con {@code POST /api/titulares}.</p>
 *
 * <p>El listado {@code GET /api/titulares} sigue en {@link CatalogoController}.</p>
 */
@RestController
@RequestMapping("/api/titulares")
public class TitularesController {

    static final String MENSAJE_DOCUMENTO_OBLIGATORIO = "numeroDocumento es obligatorio";
    static final String MENSAJE_NOMBRES_OBLIGATORIOS = "nombres y apellidos son obligatorios";

    private final ControladorPersonas personas = ControladorPersonas.getInstance();

    /** Body del alta: mismos datos que {@code CtrlPersonas.altaTitular} en Swing. */
    public record TitularRequest(
            List<String> nombres,
            List<String> apellidos,
            String numeroDocumento,
            TipoDocumento tipoDocumento,
            String celular,
            String telefonoFijo,
            String email) {
    }

    /** Alta de titular: 201 con la vista del titular creado (incluye su id). */
    @PostMapping
    public ResponseEntity<TitularVistaDto> crear(@RequestBody(required = false) TitularRequest req) {
        TitularRequest datos = req == null
                ? new TitularRequest(null, null, null, null, null, null, null)
                : req;
        String documento = texto(datos.numeroDocumento());
        if (documento == null) {
            throw new IllegalArgumentException(MENSAJE_DOCUMENTO_OBLIGATORIO);
        }
        List<String> nombres = palabras(datos.nombres());
        List<String> apellidos = palabras(datos.apellidos());
        if (nombres.isEmpty() || apellidos.isEmpty()) {
            throw new IllegalArgumentException(MENSAJE_NOMBRES_OBLIGATORIOS);
        }

        TitularReserva titular = personas.altaTitular(nombres, apellidos, documento,
                datos.tipoDocumento(), texto(datos.celular()), texto(datos.telefonoFijo()),
                texto(datos.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TitularVistaDto.de(titular));
    }

    /**
     * Titular registrado con ese documento (para autocompletar el formulario) o
     * 404 {@code {code,mensaje}} cuando no existe, que es la senal de "es un
     * titular nuevo: completa los datos".
     */
    @GetMapping("/buscar")
    public TitularVistaDto buscar(@RequestParam(name = "documento", required = false) String documento) {
        String buscado = texto(documento);
        if (buscado == null) {
            throw new IllegalArgumentException("documento es obligatorio");
        }
        TitularReserva titular = personas.buscarTitularPorDocumento(buscado);
        if (titular == null) {
            throw new IllegalArgumentException("titular no existe para documento: " + buscado);
        }
        return TitularVistaDto.de(titular);
    }

    /** Texto recortado, o null cuando esta vacio/ausente. */
    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    /** Lista de palabras sin vacios (el modelo guarda nombres/apellidos como listas). */
    private static List<String> palabras(List<String> valores) {
        List<String> resultado = new ArrayList<>();
        if (valores != null) {
            for (String valor : valores) {
                String recortado = texto(valor);
                if (recortado != null) {
                    resultado.add(recortado);
                }
            }
        }
        return resultado;
    }
}
