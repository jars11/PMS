import { useLayoutEffect, useRef, useState } from 'react';

// Split vertical redimensionable, paridad con el JSplitPane.VERTICAL_SPLIT de
// RecepcionistaFrame (calendario arriba, propuesta de estadia abajo): el usuario
// arrastra el divisor para ajustar el alto y el reparto inicial es 55/45
// (split.setResizeWeight(0.55) en Swing). El panel de abajo mide lo que mide su
// contenido (sin scrollbar propia) y la pagina scrollea; solo el alto del
// calendario es fijo, asi que la referencia del reparto es el alto visible de la
// ventana, no el del contenedor (que crece con el contenido).

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

  /**
   * Alto visible disponible para el split: desde el borde superior del panel
   * hasta el fondo de la ventana. No se mide el alto del contenedor porque el
   * panel de abajo crece con su contenido y el calculo se realimentaria.
   */
  function altoDisponible() {
    const el = contRef.current;
    if (!el) return window.innerHeight;
    const top = Math.max(0, el.getBoundingClientRect().top);
    return Math.max(0, window.innerHeight - top);
  }

  function limitar(valor) {
    const disponible = altoDisponible();
    if (disponible <= 0) return valor;
    const maximo = Math.max(ALTO_MINIMO_SUPERIOR, disponible - ALTO_MINIMO_INFERIOR - ALTO_DIVISOR);
    return Math.min(Math.max(ALTO_MINIMO_SUPERIOR, valor), maximo);
  }

  useLayoutEffect(() => {
    const ajustar = () => {
      const disponible = altoDisponible();
      if (disponible <= 0) return;
      setAlto((prev) => {
        const base = fijadoRef.current && prev != null ? prev : Math.round(disponible * PESO_INICIAL);
        return Math.min(
          Math.max(ALTO_MINIMO_SUPERIOR, base),
          Math.max(ALTO_MINIMO_SUPERIOR, disponible - ALTO_MINIMO_INFERIOR - ALTO_DIVISOR),
        );
      });
    };
    ajustar();
    window.addEventListener('resize', ajustar);
    return () => window.removeEventListener('resize', ajustar);
  }, []);

  function iniciarArrastre(e) {
    if (e.button !== 0) return;
    e.preventDefault();
    const disponible = altoDisponible();
    arrastreRef.current = {
      y: e.clientY,
      alto: alto ?? Math.round(disponible * PESO_INICIAL),
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
    const actual = alto ?? Math.round(altoDisponible() * PESO_INICIAL);
    let siguiente = null;
    if (e.key === 'ArrowUp') siguiente = actual - PASO_TECLADO;
    else if (e.key === 'ArrowDown') siguiente = actual + PASO_TECLADO;
    else if (e.key === 'Home') siguiente = ALTO_MINIMO_SUPERIOR;
    else if (e.key === 'End') siguiente = altoDisponible();
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
