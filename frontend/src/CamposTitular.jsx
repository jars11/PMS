import { TIPOS_DOCUMENTO } from './useTitular';

/**
 * Bloque de titular compartido por el formulario de reserva y la propuesta de
 * estadía (paridad con DialogoReserva/PanelPropuestaEstadia, pero con el flujo
 * web "documento primero"): se pide el documento del titular, al salir del campo
 * se busca y, si existe, se autocompletan sus datos y se usa su id; si no
 * existe, se completan nombres/apellidos/celular/email y el alta se hace al
 * guardar. No hay selector de titulares existentes.
 */
export default function CamposTitular({ titular, autoFocusDocumento = false }) {
  const {
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
  } = titular;

  return (
    <>
      <div className="fila">
        <label>
          Tipo de documento
          <select value={tipoDocumento} onChange={(e) => setTipoDocumento(e.target.value)}>
            {TIPOS_DOCUMENTO.map((tipo) => (
              <option key={tipo} value={tipo}>
                {tipo}
              </option>
            ))}
          </select>
        </label>
        <label>
          Documento del titular
          <input
            value={numeroDocumento}
            autoFocus={autoFocusDocumento}
            placeholder="Documento del titular"
            onChange={(e) => cambiarDocumento(e.target.value)}
            onBlur={() => buscar()}
          />
        </label>
      </div>

      {consultando && <div className="propuesta-aviso">Buscando titular…</div>}
      {mensaje && <div className="propuesta-aviso">{mensaje}</div>}
      {error && <div className="error">{error}</div>}

      {existente ? (
        <div className="fila">
          <label>
            Nombre
            <input value={existente.nombre} readOnly />
          </label>
          <label>
            Celular
            <input value={existente.celular ?? ''} readOnly />
          </label>
          <label>
            Email
            <input value={existente.email ?? ''} readOnly />
          </label>
        </div>
      ) : (
        <>
          <div className="fila">
            <label>
              Nombres (titular nuevo)
              <input
                value={datos.nombres}
                onChange={(e) => cambiarDato('nombres', e.target.value)}
              />
            </label>
            <label>
              Apellidos (titular nuevo)
              <input
                value={datos.apellidos}
                onChange={(e) => cambiarDato('apellidos', e.target.value)}
              />
            </label>
          </div>
          <div className="fila">
            <label>
              Celular
              <input
                value={datos.celular}
                onChange={(e) => cambiarDato('celular', e.target.value)}
              />
            </label>
            <label>
              Teléfono fijo
              <input
                value={datos.telefonoFijo}
                onChange={(e) => cambiarDato('telefonoFijo', e.target.value)}
              />
            </label>
            <label>
              Email
              <input value={datos.email} onChange={(e) => cambiarDato('email', e.target.value)} />
            </label>
          </div>
        </>
      )}
    </>
  );
}
