import { useEffect, useRef, useState } from 'react';

// Split vertical redimensionable, paridad con el JSplitPane.VERTICAL_SPLIT de
// RecepcionistaFrame (calendario arriba, propuesta de estadia abajo): el usuario
// arrastra el divisor para ajustar el alto y el reparto inicial es 55/45
// (split.setResizeWeight(0.55) en Swing).

const ALTO_MINIMO_SUPERIOR = 180; // Swing: calendario.setMinimumSize(0, 180)
const ALTO_MINIMO_INFERIOR = 140;
const ALTO_DIVISOR = 10;
const PESO_INICIAL = 0.55;
const PASO_TECLADO = 16;

export default function SplitVertical({ arriba, abajo }) {
  const contRef = useRef(null);
  const [alto, setAlto] = useState(null);
  const [arrastrando, setArrastrando] = useState(false);
  const arrastreRef = useRef(null);
  // Una vez que el usuario toca el divisor el alto deja de seguir al 55% al
  // redimensionar la ventana (como un JSplitPane ya movido a mano).
  const fijadoRef = useRef(false);

  function altoContenedor() {
    const el = contRef.current;
    return el ? el.getBoundingClientRect().height : 0;
  }

  function limitar(valor) {
    const total = altoContenedor();
    if (total <= 0) return valor;
    const maximo = Math.max(ALTO_MINIMO_SUPERIOR, total - ALTO_MINIMO_INFERIOR - ALTO_DIVISOR);
    return Math.min(Math.max(ALTO_MINIMO_SUPERIOR, valor), maximo);
  }

  useEffect(() => {
    const el = contRef.current;
    if (!el) return undefined;
    const ajustar = (total) => {
      if (total <= 0) return;
      setAlto((prev) => {
        const base = fijadoRef.current && prev != null ? prev : Math.round(total * PESO_INICIAL);
        return Math.min(
          Math.max(ALTO_MINIMO_SUPERIOR, base),
          Math.max(ALTO_MINIMO_SUPERIOR, total - ALTO_MINIMO_INFERIOR - ALTO_DIVISOR),
        );
      });
    };
    const ro = new ResizeObserver((entries) => {
      ajustar(entries[0]?.contentRect?.height ?? 0);
    });
    ro.observe(el);
    ajustar(el.getBoundingClientRect().height);
    return () => ro.disconnect();
  }, []);

  function iniciarArrastre(e) {
    if (e.button !== 0) return;
    e.preventDefault();
    const total = altoContenedor();
    arrastreRef.current = {
      y: e.clientY,
      alto: alto ?? Math.round(total * PESO_INICIAL),
    };
    fijadoRef.current = true;
    setArrastrando(true);
    if (e.currentTarget.setPointerCapture) e.currentTarget.setPointerCapture(e.pointerId);
  }

  function moverArrastre(e) {
    const inicio = arrastreRef.current;
    if (!inicio) return;
    e.preventDefault();
    setAlto(limitar(inicio.alto + (e.clientY - inicio.y)));
  }

  function terminarArrastre(e) {
    if (!arrastreRef.current) return;
    arrastreRef.current = null;
    setArrastrando(false);
    if (e.currentTarget.hasPointerCapture && e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId);
    }
  }

  function teclaDivisor(e) {
    const actual = alto ?? Math.round(altoContenedor() * PESO_INICIAL);
    let siguiente = null;
    if (e.key === 'ArrowUp') siguiente = actual - PASO_TECLADO;
    else if (e.key === 'ArrowDown') siguiente = actual + PASO_TECLADO;
    else if (e.key === 'Home') siguiente = ALTO_MINIMO_SUPERIOR;
    else if (e.key === 'End') siguiente = altoContenedor();
    if (siguiente == null) return;
    e.preventDefault();
    fijadoRef.current = true;
    setAlto(limitar(siguiente));
  }

  return (
    <div className="split-vertical" ref={contRef}>
      <div
        className="split-arriba"
        style={alto != null ? { height: alto, flex: '0 0 auto' } : undefined}
      >
        {arriba}
      </div>
      <div
        className={`split-divisor${arrastrando ? ' arrastrando' : ''}`}
        role="separator"
        aria-orientation="horizontal"
        aria-label="Ajustar el alto del calendario"
        tabIndex={0}
        onPointerDown={iniciarArrastre}
        onPointerMove={moverArrastre}
        onPointerUp={terminarArrastre}
        onPointerCancel={terminarArrastre}
        onKeyDown={teclaDivisor}
      >
        <span className="split-divisor-grip" />
      </div>
      <div className="split-abajo">{abajo}</div>
    </div>
  );
}
