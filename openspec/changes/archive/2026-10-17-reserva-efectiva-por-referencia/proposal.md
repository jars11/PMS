# Proposal: Contract as the single Solicitud-Reserva link

## Intent

Replace the flag-plus-demotion encoding of "at most one effective reserva per solicitud" (`Reserva.efectiva`, `isEfectiva`/`setEfectiva`, `Solicitud.promoverEfectiva`, four `setEfectiva(true)` sites) with structural cardinalities in `ContratoReserva`. The contract becomes the only solicitud<->reserva link. The earlier `Solicitud.reserva_efectiva_id` pointer design is discarded. Candidates stay ephemeral; only the selected reserva is persisted.

## Scope

### In Scope
- Remove the effective-reserva field, accessors, promotion method, and flag writes.
- Make `ContratoReserva` the sole link: both FKs NOT NULL, each UNIQUE (replacing the pair constraint).
- `reserva.titular_id` NOT NULL; `solicitud.titular_id` nullable (anonymous requests).
- Creation persists titular -> reserva -> solicitud -> contract in one transaction.
- Improvement rule: migrate `Pago`s to the new reserva before deleting the old one, lifting the payment block for that flow only.
- Contract-based repository lookups, cleanup order, tests, draft DB recreation.

### Out of Scope
- UI, a new improvement workflow (none exists; the rule only constrains it), handler/DTO changes, UML edits, migration tooling.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `solicitud-contrato-reserva`: cardinalities, contract uniqueness, removal of the effective flag, payment migration on improvement.

## Approach

Move the invariant from mutable state into the schema: contract uniqueness per side limits each solicitud and reserva to at most one contract, which is the only link. Creation writes the four rows atomically; losing candidates never reach the database. Deletion removes contracts first. `hbm2ddl.auto=update` cannot drop the old pair constraint or `RESERVA.EFECTIVA`, so `data/pms.mv.db` is recreated.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `entities/Reserva.java` | Modified | Remove `efectiva`; titular NOT NULL |
| `entities/Solicitud.java` | Modified | Remove `promoverEfectiva` |
| `entities/ContratoReserva.java` | Modified | Unique per FK side |
| `controllers/ControladorReservas.java` | Modified | Creation, payment migration, delete order |
| `persistence/RepositorioSolicitudes.java` | Modified | Contract-based lookups |
| `Main.java` | Modified | Cleanup order |
| `test/.../SolicitudContratoReservaTest.java` | Modified | Cardinality tests |

UI: not affected.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `update` leaves old constraint/column | High | Recreate draft DB; schema tests |
| Exactly-one-per-reserva is only at-most-one in SQL | Med | Single creation path; integration test |
| Improvement deletes a paid reserva before migration | Med | Migration precedes delete in one transaction |

## Rollback Plan

Revert and recreate `data/pms.mv.db`; harmless on a draft database.

## Dependencies

- Draft database recreation (`data/pms.mv.db`); `mvn -B test`.

## Success Criteria

- [ ] `mvn -B test` green.
- [ ] No `EFECTIVA` column and no effective-reserva API remain.
- [ ] Each creation path yields one reserva, solicitud, contract; failures persist nothing.
- [ ] Deletion/cleanup produce no FK violations.
