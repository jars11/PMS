export default function ReservasLista({ reservas, onEditar }) {
  return (
    <table className="tabla">
      <thead>
        <tr>
          <th>ID</th>
          <th>Titular</th>
          <th>Habitación</th>
          <th>Check-in</th>
          <th>Check-out</th>
          <th>Estado</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {reservas.map((r) => (
          <tr key={r.id}>
            <td>{r.id}</td>
            <td>{r.titular}</td>
            <td>{r.numerosHabitacion.join(', ')}</td>
            <td>{r.checkIn}</td>
            <td>{r.checkOut}</td>
            <td>{r.estado}</td>
            <td>
              <button onClick={() => onEditar(r)}>Editar</button>
            </td>
          </tr>
        ))}
        {reservas.length === 0 && (
          <tr>
            <td colSpan={7} className="vacio">
              No hay reservas.
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}
