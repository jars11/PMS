import { TIPOS_DOCUMENTO } from './useTitular';

/**
 * Bloque de titular compartido por el formulario de reserva y la propuesta de
 * estadía (paridad con DialogoReserva/PanelPropuestaEstadia, pero con el flujo
 * web "documento primero"): se pide el documento del titular, al salir del campo
 * se busca y, si existe, se autocompletan sus datos y se usa su id; si no
 * existe, se completan nombres/apellidos/celular/email y el alta se hace al
 * guardar. El selector de titulares existentes queda como alternativa.
 */
export default function CamposTitular({ titular, titulares = [], autoFocusDocumento = false }) {
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
    usarExistente,
    limpiar,
  } = titular;

  const estaEnLista =
    existente && titulares.some((t) => String(t.id) === String(existente.id));

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

      <label>
        Titular existente (alternativa)
        <select
          value={existente ? String(existente.id) : ''}
          onChange={(e) => {
            if (!e.target.value) {
              limpiar();
              return;
            }
            const elegido = titulares.find((t) => String(t.id) === e.target.value);
            if (elegido) usarExistente(elegido);
          }}
        >
          <option value="">— buscar por documento —</option>
          {titulares.map((t) => (
            <option key={t.id} value={String(t.id)}>
              {t.nombre}
            </option>
          ))}
          {existente && !estaEnLista && (
            <option value={String(existente.id)}>{existente.nombre}</option>
          )}
        </select>
      </label>

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
