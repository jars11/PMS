# Apply Progress: Contract as the single Solicitud-Reserva link

**Change**: `reserva-efectiva-por-referencia`
**Artifact store**: openspec
**Mode**: Strict TDD (`mvn -B test`, `strict_tdd: true`)
**Task count**: **25/25** tasks complete (Phase 1: 8, Phase 2: 3, Phase 3: 6, Phase 4: 4, Phase 5: 4)
**Baseline before the change**: 209 tests green (first apply report).
**First apply result**: 223 tests green.
**Corrective pass result**: 226 tests green, `mvn -B -DskipTests package` green.

## Ledger Deviation (explicit)

The native attempt ledger was contradictory:

- `begin` reported the budget as exhausted.
- `reset` reported a non-terminal attempt.
- `finish` reported no active attempt.

The maintainer explicitly authorized this corrective pass **outside the ledger**. No attempt ID was minted and no ledger transition was performed. This artifact is the authoritative record for the corrective pass. The ledger contradiction is recorded here instead of being silently retried.

## Startup / Preconditions

- Read `verify-report.md`, `proposal.md`, `design.md`, `tasks.md`, the delta spec, `ControladorReservas.java`, and `SolicitudContratoReservaTest.java`.
- Strict TDD active: no production behavior was changed before a failing focused test existed. The only production behavior change in this pass is the W6 zero-block guard.
- Repo conventions preserved: Spanish comments/strings, English identifiers.

## Corrective Pass Summary

| ID | Correction | Type | Result |
|---|---|---|---|
| C1 | Added a real mid-transaction rollback test for `reemplazarReserva`. | Test only | Done — `reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato` |
| W2 | Fixed the delta scenario plus the design/tasks literals: the contract is re-pointed, not deleted. | Spec wording | Done |
| W3 | Made `seleccionAbandonadaNoPersisteNada` non-vacuous with a negative control. | Test only | Done |
| W4 | Persisted this TDD evidence artifact; corrected W1 from 29 to 25 tasks. | Documentation | Done |
| W5 | Narrowed the exactly-one-contract invariant to controller creation paths and documented the repository escape hatch. | Spec wording | Done |
| W6 | Added a zero-consumed-block guard in `crearReserva` plus all-skipped and empty-input tests. | Production + tests | Done |

### C1 — real mid-transaction rollback test

- Kept the existing pre-validation test `reemplazoFallidoConservaReservaPagosYContrato` as a separate case (it fails at the null-titular guard before the transaction).
- Added `reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato`:
  - creates a real reserva with one payment and its contract;
  - creates a real `Habitacion`, captures its id, then deletes the room;
  - builds a transient improved `Reserva` whose `RHPropuesta` references the deleted room id;
  - calls `reemplazarReserva`; `em.persist(nueva)` assigns an id, then the flush violates `FK1MFLCHHB8V0U6VBUF4LHTHYF` (`RHPROPUESTA.HABITACION_ID -> HABITACION.ID`);
  - asserts `PersistenceException`, `nueva.getId() != null` (proof the failure happened after `em.persist`), old reserva present, payment still on the old reserva, and the same contract id still pointing to the old reserva;
  - asserts only the old reserva remains.
- Production transaction was **not** weakened.

### W2 / W5 — spec wording

- `paid reserva replaced`: now says "the old reserva is deleted and its contract row is re-pointed to the new reserva".
- `ContratoReserva association class`: now scopes exactly-one-contract to "each reserva created through the controller creation paths" and documents `RepositorioReservas.guardar` as a lower-level escape hatch that may persist a contract-less reserva for fixtures/legacy callers.
- `every created reserva has one contract` scenario now says "controller creation path".
- `design.md` U4 testing row now says the contract is re-pointed instead of "contract gone".
- `tasks.md` 3.4 wording was corrected from "deletes old reserva/contract" to "re-points the contract, deletes the old reserva". Task 3.6 now also names the zero-consumed-block guard. Both remain checked.

### W3 — non-vacuous abandoned selection

- The test now persists a deliberate reserva and a deliberate solicitud, asserts the repositories see them (negative control), then cleans up and asserts zero.
- It then constructs the two transient candidates and asserts zero reservas, zero solicitudes, and zero contracts.
- This proves the repository queries can detect rows, so the abandoned-selection assertion is no longer vacuous.

### W6 — zero-block guard

- `crearReserva` now counts consumed blocks and throws `IllegalStateException` inside `JpaUtil.enTransaccion` when `consumidos == 0`, before `persistirReservaYSolicitud`.
- Added:
  - `crearReservaSinBloquesPendientesNoPersisteNada` (empty input);
  - `crearReservaConTodosLosBloquesSaltadosNoPersisteNada` (all blocks skipped by the capacity filter).
- Both assert `IllegalStateException` plus zero reservas, zero solicitudes, and zero contracts.
- Added a new delta scenario `no consumed blocks` under the creation requirement.
- Existing callers that queue feasible blocks are unaffected; the full suite confirms it.

## TDD Cycle Evidence (first apply)

Safety net for the whole change: **209 baseline tests green** before the first apply. Phase RED/GREEN counts below are the real counts reported by the first apply.

| Task | Test file | Layer | Safety net | RED evidence | GREEN evidence | Triangulation | Refactor |
|---|---|---|---|---|---|---|---|
| 1.1 Reflection for absent effective surface | `SolicitudContratoReservaTest` | Unit (reflection) | 209 green | Phase 1 RED batch: 33 tests, **8 failures** | `reservaNoDeclaraFlagEfectiva`, `solicitudNoDeclaraPromoverEfectiva` green in the same class run | H2 metadata test + source grep | None |
| 1.2 H2 metadata cardinalities | `SolicitudContratoReservaTest` | Integration/schema | 209 green | Phase 1 RED batch: 33 tests, **8 failures** | `esquemaSinColumnaEfectiva`, `esquemaConCardinalidadesFinales` green | File-DB metadata query in Phase 5 | None |
| 1.3 Duplicate/titular rejection | `SolicitudContratoReservaTest` | Integration | 209 green | Phase 1 RED batch: 33 tests, **8 failures** | `contratoRechazaSegundoContratoParaLaMismaSolicitud`, `contratoRechazaSegundoContratoParaLaMismaReserva`, `reservaSinTitularEsRechazada`, `solicitudSinTitularSePersiste` green | Metadata nullability assertions | None |
| 1.4 `Reserva` flag removal + NOT NULL titular | `Reserva.java` | Integration | 209 green | RED owned by 1.1/1.3 batch | Same class run green after the entity change | `esquemaConCardinalidadesFinales` | None |
| 1.5 `Solicitud.promoverEfectiva` removal | `Solicitud.java` | Unit (reflection) | 209 green | RED owned by 1.1 batch | `solicitudNoDeclaraPromoverEfectiva` green | Compile coupling with the controller writes | None |
| 1.6 Per-side UNIQUE + `setReserva` | `ContratoReserva.java` | Integration/schema | 209 green | RED owned by 1.2/1.3 batch | Duplicate tests + metadata green | Pair-constraint absence assertion | None |
| 1.7 Adapt seven raw fixtures | `MatrizOcupacionTest`, `SolicitudContratoReservaTest` | Integration | 209 green | Phase 1 RED batch; fixture adaptation landed with the cardinality changes | Full suite green after fixture reservas got contracts | Full `mvn -B test` | Fixture helper cleanup only |
| 1.8 Document repository escape hatch | `RepositorioReservas` Javadoc | Source inspection | N/A | No RED (documentation) | Javadoc present; W5 spec wording added in this pass | W5 spec wording | None |
| 2.1 Add lookup seams | `SolicitudContratoReservaTest` | Integration | 209 green | No separate Phase 2 batch; lookups were renamed inside the 33-test class run | `repositorioResuelveReservaYContratoPorContrato`, `repositorioSinContratoDevuelveNull` green | Design D5 single join fetch | None |
| 2.2 RED contract-linked lookups | `SolicitudContratoReservaTest` | Integration | 209 green | Same 33-test class run (flag-era lookups failed after the flag removal) | Both lookup tests green | Cross-entity-field reflection test | None |
| 2.3 One join fetch, delete flag-era methods | `RepositorioSolicitudes.java` | Integration | 209 green | Same 33-test class run | Both lookup tests green; no flag predicate in source | Source inspection | None |
| 3.1 Creation tests assert one contract | `SolicitudContratoReservaTest` | Integration | 209 green | Phase 3 RED batch: 33 tests, **1 failure + 1 error** | `crearReservaPara...`, `crearReservaAutomatica...`, `crearReservaMiraSoloBloquesConsumidos`, `persistirPropuestas...` green | Per-path contract counts | None |
| 3.2 Drop four `setEfectiva(true)` writes | `ControladorReservas.java` | Integration | 209 green | RED owned by 3.1 batch | Creation tests green | Reflection test proves the surface is gone | None |
| 3.3 Replacement stub | `ControladorReservas.java` | Compile seam | 209 green | N/A (stub throws `UnsupportedOperationException`) | Compiles | N/A | None |
| 3.4 Replacement RED | `SolicitudContratoReservaTest` | Integration | 209 green | Phase 3 RED batch: 33 tests, **1 failure + 1 error** | `reemplazarReservaMigraPagosYConservaSolicitudPerfilYTitular`, `reemplazoFallidoConservaReservaPagosYContrato`, `eliminarReservaConPagosDevuelveFalseYConservaReservaYPagos` green | C1 mid-transaction rollback test added in this corrective pass | None |
| 3.5 Replacement GREEN | `ControladorReservas.reemplazarReserva` | Integration | 209 green | RED owned by 3.4 batch | Replacement tests green | C1 asserts rollback of reserva/pagos/contract | None |
| 3.6 Guards (one contract, abandoned selection) | `SolicitudContratoReservaTest` | Integration | 209 green | Phase 3 RED batch | Creation tests + `seleccionAbandonadaNoPersisteNada` green | W3 negative control added; W6 zero-block guard added | Test structure only |
| 4.1 Cleanup-order source test | `SolicitudContratoReservaTest` | Source inspection | 209 green | Phase 4 RED batch: 35 tests, **1 failure** | `mainLimpiarDatosBorraContratosYSolicitudesAntesQueReservas` green | Source order in `Main.limpiarDatos` | None |
| 4.2 `eliminarContratos` + `Main` order | `Main.java`, `RepositorioSolicitudes.java` | Source + integration | 209 green | RED owned by 4.1 batch | Source test green; full suite has no FK violations | `Main.limpiarDatos` source assertion | None |
| 4.3 `TestUtil.limpiarBase` order | `TestUtil.java` | Integration | 209 green | RED owned by 4.1 batch | Full suite seeds/clears contracts without FK errors | Full `mvn -B test` | None |
| 4.4 `eliminarReserva` contract-first guard | `ControladorReservas.eliminarReserva` | Integration | 209 green | RED owned by 4.1 batch | `eliminarReservaBorraContratoYReservaPeroConservaSolicitud` green | Paid-delete test remains green | None |
| 5.1 Delete draft DB | `data/pms.mv.db` | Ops | N/A | No RED by design (D8/U7 migration) | File absent before rebuild | First-apply evidence | None |
| 5.2 Rebuild draft DB | Runtime harness | Runtime | N/A | No RED by design (D8/U7) | Direct `java -cp` invocation of `com.pms.Main` recreated the file (first apply; not re-run in this pass) | Verify-report metadata | None |
| 5.3 Query recreated schema | H2 `RunScript` read-only | Runtime SQL | N/A | No RED by design (D8/U7) | Metadata query confirmed FKs/UNIQUE/nullability (first apply) | Verify-report DB evidence | None |
| 5.4 Full suite + package | `mvn -B test`, `mvn -B -DskipTests package` | Integration + build | 209 green | Final first-apply run | **223 green**, package success (first apply) | Corrective pass re-ran both at **226 green** | None |

## Corrective Pass Test Evidence (exact commands)

All commands ran from `C:\Users\agust\OneDrive\Documents\PMS`.

```text
> mvn -B test -Dtest=SolicitudContratoReservaTest
exit code: 0
Tests run: 35, Failures: 0, Errors: 0  (pre-change baseline)

> mvn -B test -Dtest=SolicitudContratoReservaTest   (after adding the two W6 RED tests, before the guard)
exit code: 1
Tests run: 37, Failures: 2, Errors: 0
crearReservaSinBloquesPendientesNoPersisteNada: Expected IllegalStateException, nothing was thrown
crearReservaConTodosLosBloquesSaltadosNoPersisteNada: Expected IllegalStateException, nothing was thrown

> mvn -B test -Dtest=SolicitudContratoReservaTest   (after the W6 guard)
exit code: 0
Tests run: 37, Failures: 0, Errors: 0

> mvn -B test -Dtest=SolicitudContratoReservaTest   (after C1 + W3)
exit code: 0
Tests run: 38, Failures: 0, Errors: 0
C1 log evidence: FK1MFLCHHB8V0U6VBUF4LHTHYF RHPROPUESTA(HABITACION_ID) -> HABITACION(ID) violation

> mvn -B test
exit code: 0
Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

> mvn -B -DskipTests package
exit code: 0
Building jar: C:\Users\agust\OneDrive\Documents\PMS\target\pms-1.0.0.jar
BUILD SUCCESS
```

## Files Changed in the Corrective Pass

| File | Action | What changed |
|---|---|---|
| `src/main/java/com/pms/controllers/ControladorReservas.java` | Modified | W6: consumed-block counter and zero-block `IllegalStateException` inside the creation transaction |
| `src/test/java/com/pms/SolicitudContratoReservaTest.java` | Modified | C1 rollback test + helper overload; W3 negative control; W6 empty/all-skipped tests; `contarContratos` helper |
| `openspec/changes/reserva-efectiva-por-referencia/specs/solicitud-contrato-reserva/spec.md` | Modified | W2 contract re-point wording; W5 controller-path scope + escape hatch; W6 no-consumed-blocks scenario |
| `openspec/changes/reserva-efectiva-por-referencia/design.md` | Modified | W2: U4 testing row now says the contract is re-pointed instead of "contract gone" |
| `openspec/changes/reserva-efectiva-por-referencia/tasks.md` | Modified | 3.4 contract wording; 3.6 zero-block guard wording (all 25 boxes remain checked) |
| `openspec/changes/reserva-efectiva-por-referencia/apply-progress.md` | Created | This evidence artifact |

Prior apply files remain modified and are unchanged by this pass except `ControladorReservas.java` and the test file above.

## Deviations

1. **C1 RED is coverage-only, not a behavior change.** The production transaction was already correct. The new test passes once added; its non-vacuity is proven by the FK violation occurring after `em.persist(nueva)` and by `nueva.getId() != null`. The previous test remains as the pre-transaction guard case. No production rollback logic was touched.
2. **W3 is test-quality-only.** The negative control passes with the existing production code; it strengthens the assertion rather than changing behavior.
3. **W6 is the only production behavior change** in this pass. It adds a guard inside the existing transaction; it does not weaken the transaction.
4. **Task count corrected.** The first apply report's "29/29" was wrong. `tasks.md` contains exactly 25 checkboxes; this artifact reports **25/25**.
5. **No database rebuild in this pass.** No entity or schema changed, so `data/pms.mv.db` was not recreated. The Phase 5 evidence remains from the first apply.
6. **No commit, no PR, no AI attribution** — per the corrective-pass instructions.

## Risks / Follow-ups

- `crearReserva` now rejects an empty request. Any caller that intentionally created a zero-block solicitud will now receive `IllegalStateException`; this is the intended `1..* RHSolicitada` behavior.
- `RepositorioReservas.guardar` remains a public escape hatch by design; the spec now documents it instead of claiming an unconditional exactly-one invariant.
- `openspec/config.yaml` still caches `verified_test_count: 209`; update it when archiving so the next preflight does not use a stale baseline.
- The draft-DB evidence query and first-apply RED logs were not re-run in this pass; they are cited from the first apply and the verify report.

## Status

**25/25 tasks complete.** Corrective pass C1, W2, W3, W4, W5, and W6 addressed. `mvn -B test` is green with **226 tests**, and `mvn -B -DskipTests package` succeeds. Ready for re-verification.
