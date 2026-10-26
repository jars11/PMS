import { useRef, useState } from 'react';
import { api } from './api';

// Mismos valores que el enum TipoDocumento del backend (com.pms.utils.TipoDocumento).
export const TIPOS_DOCUMENTO = ['CEDULA', 'DNI', 'PASAPORTE', 'OTRO'];
export const TIPO_DOCUMENTO_POR_DEFECTO = 'CEDULA';

const DATOS_VACIOS = {
  nombres: '',
  apellidos: '',
  celular: '',
  telefonoFijo: '',
  email: '',
};

/** El backend guarda nombres y apellidos como listas: se parte el texto tipeado. */
function aLista(texto) {
  return (texto || '').trim().split(/\s+/).filter(Boolean);
}

function aDatosDe(t) {
  return {
    nombres: '',
    apellidos: '',
    celular: t?.celular ?? '',
    telefonoFijo: t?.telefonoFijo ?? '',
    email: t?.email ?? '',
  };
}

/**
 * Flujo "documento del titular primero": se busca el documento y, si el titular
 * ya existe, se autocompletan sus datos; si no existe, se completan y el alta se
 * hace con `POST /api/titulares` al guardar. Reusa los endpoints REST de
 * titulares (no reimplementa nada del backend) y lo comparten el formulario de
 * reserva y la propuesta de estadía.
 */
export function useTitular(titularInicial = null) {
  const [tipoDocumento, setTipoDocumento] = useState(
    titularInicial?.tipoDocumento ?? TIPO_DOCUMENTO_POR_DEFECTO,
  );
  const [numeroDocumento, setNumeroDocumento] = useState(titularInicial?.numeroDocumento ?? '');
  const [datos, setDatos] = useState(() => aDatosDe(titularInicial));
  // Titular ya registrado: el que devolvió la búsqueda por documento, el elegido
  // en el selector alternativo o el precargado al editar una reserva.
  const [existente, setExistente] = useState(titularInicial);
  const [consultando, setConsultando] = useState(false);
  const [mensaje, setMensaje] = useState(
    titularInicial ? `Titular existente: ${titularInicial.nombre}` : '',
  );
  const [error, setError] = useState('');
  // Documento vigente: descarta la respuesta de una búsqueda cuyo documento el
  // usuario ya cambió (misma idea que generacionRef en PropuestaEstadia).
  const documentoVigenteRef = useRef((titularInicial?.numeroDocumento ?? '').trim());

  /** Precarga un titular ya conocido (edición o selector alternativo). */
  function usarExistente(t) {
    if (!t) return;
    documentoVigenteRef.current = (t.numeroDocumento ?? '').trim();
    setExistente(t);
    setNumeroDocumento(t.numeroDocumento ?? '');
    setTipoDocumento(t.tipoDocumento ?? TIPO_DOCUMENTO_POR_DEFECTO);
    setDatos(aDatosDe(t));
    setMensaje(`Titular existente: ${t.nombre}`);
    setError('');
  }

  /** El usuario tipea el documento: el titular encontrado antes deja de valer. */
  function cambiarDocumento(valor) {
    documentoVigenteRef.current = (valor || '').trim();
    setNumeroDocumento(valor);
    setExistente(null);
    setMensaje('');
    setError('');
  }

  /** Vuelve a "sin titular elegido" (opción vacía del selector alternativo). */
  function limpiar() {
    documentoVigenteRef.current = '';
    setNumeroDocumento('');
    setTipoDocumento(TIPO_DOCUMENTO_POR_DEFECTO);
    setExistente(null);
    setDatos(DATOS_VACIOS);
    setMensaje('');
    setError('');
  }

  function cambiarDato(campo, valor) {
    setDatos((previos) => ({ ...previos, [campo]: valor }));
  }

  /**
   * Busca el documento tipeado y autocompleta el titular cuando existe. Se
   * dispara al salir del campo y antes de guardar (para que un click directo en
   * "Guardar" no pierda la búsqueda).
   */
  async function buscar() {
    const documento = numeroDocumento.trim();
    if (!documento) {
      setExistente(null);
      setMensaje('');
      return null;
    }
    const generacion = documento;
    setConsultando(true);
    setError('');
    try {
      const encontrado = await api.buscarTitularPorDocumento(documento);
      if (documentoVigenteRef.current !== generacion) {
        return encontrado; // el usuario ya cambió el documento
      }
      if (encontrado) {
        usarExistente(encontrado);
      } else {
        setExistente(null);
        setMensaje(
          'No existe un titular con ese documento: completá los datos para darlo de alta.',
        );
      }
      return encontrado;
    } catch (err) {
      setError(err.message);
      return null;
    } finally {
      setConsultando(false);
    }
  }

  /**
   * Id del titular a usar al guardar: el existente o uno nuevo dado de alta con
   * los datos tipeados. Es el único camino de alta de titulares de la web.
   */
  async function resolverTitularId() {
    if (existente) return Number(existente.id);
    const documento = numeroDocumento.trim();
    if (!documento) {
      throw new Error('El documento del titular es obligatorio.');
    }
    const encontrado = await api.buscarTitularPorDocumento(documento);
    if (encontrado) {
      usarExistente(encontrado);
      return Number(encontrado.id);
    }
    const nombres = aLista(datos.nombres);
    const apellidos = aLista(datos.apellidos);
    if (nombres.length === 0 || apellidos.length === 0) {
      throw new Error('Completá nombres y apellidos para dar de alta el titular.');
    }
    const creado = await api.crearTitular({
      nombres,
      apellidos,
      numeroDocumento: documento,
      tipoDocumento,
      celular: datos.celular.trim(),
      telefonoFijo: datos.telefonoFijo.trim(),
      email: datos.email.trim(),
    });
    usarExistente(creado);
    return Number(creado.id);
  }

  return {
    tipoDocumento,
    setTipoDocumento,
    numeroDocumento,
    cambiarDocumento,
    datos,
    cambiarDato,
    existente,
    consultando,
    mensaje,
    error,
    buscar,
    usarExistente,
    limpiar,
    resolverTitularId,
  };
}
