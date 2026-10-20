# Archive Report: reserva-efectiva-por-referencia

| Field | Value |
|---|---|
| Change | `reserva-efectiva-por-referencia` |
| Archived | 2026-10-17 |
| Archive path | `openspec/changes/archive/2026-10-17-reserva-efectiva-por-referencia/` |
| Artifact store | openspec |
| Status at close | `pass_with_warnings` — archived |
| Verify verdict | `pass_with_warnings` |
| Blockers | 0 |
| Critical findings | 0 |
| Delta requirements | 11/11 |
| Delta scenarios | 34/34 |
| Merged main spec | 14 requirements, 40 scenarios |
| Review mode | RDD review off; no review gate applied |
| Committed | No (archive only; working tree unchanged in `src/` and `data/`) |

## Final State at Close

`ContratoReserva` is the only persisted 1:1 link between `Solicitud` and `Reserva`:

- `contrato_reserva.solicitud_id` and `contrato_reserva.reserva_id` are each NOT NULL and UNIQUE; the old `(solicitud_id, reserva_id)` pair constraint is gone.
- `Reserva.efectiva`, `isEfectiva()`, `setEfectiva(boolean)`, `Solicitud.promoverEfectiva(Reserva)`, the `RESERVA.EFECTIVA` column, and the four `setEfectiva(true)` writes are removed.
- `reserva.titular_id` is NOT NULL; `solicitud.titular_id` stays nullable for anonymous requests.
- `crearReserva` rejects zero consumed blocks with `IllegalStateException` inside the creation transaction.
- `reemplazarReserva` persists the new reserva, re-points the existing contract, migrates `Pago` rows, then removes the old reserva in one transaction; direct paid deletion remains blocked.
- Cleanup order is contract -> solicitud/reserva in `Main.limpiarDatos`, `TestUtil.limpiarBase`, and `eliminarReserva`.

## Test and Build Evidence

| Check | Command | Result |
|---|---|---|
| Test suite | `mvn -B test` | exit 0; 226 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` |
| Build | `mvn -B -DskipTests package` | exit 0; `BUILD SUCCESS`; `target/pms-1.0.0.jar` present (212,334 bytes, last write 2026-10-17 05:50:10) |

Verified evidence hashes from `verify-report.md`:

- Test output: `sha256:84a57ddf12cd03b9f441789fb8bc0bc3c81e48f694002f759b6a4683cc085e8f`
- Build output: `sha256:4448ecdcfe5cead1e22605970ab68aef09124e9e45e6a61b78e69d9ab3dadfeb`
- Evidence revision: `sha256:669af2fe8b8049b3cffb191ef072f6889292a3a7451458392c3d94b8619f3c51`

The final `package` invocation reported "Nothing to compile - all classes are up to date" and did not rewrite the jar. The jar artifact exists from the corrective pass; a clean-room rebuild remains optional (S8).

## Spec Sync

### Domain: `solicitud-contrato-reserva`

| Action | Count | Requirements |
|---|---:|---|
| ADDED | 5 | Effective-reserva surface is absent; Ephemeral reserva candidates; Payment migration on reserva replacement; Schema reflects final cardinalities; Test-first coverage for every behavior change |
| MODIFIED | 6 | Solicitud persisted; Repository access; ContratoReserva association class; Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva; Both persistence units register new entities; Test cleanup FK order |
| REMOVED | 2 | Reserva.efectiva flag; At most one effective per Solicitud |
| RENAMED | 1 (carried by MODIFIED) | `Creation paths persist Solicitud + ContratoReserva and mark the new Reserva effective` -> `Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva` |

Merge notes:

- The rename above was carried by a MODIFIED block because the delta has no `RENAMED Requirements` section. Applying the block in place of the old heading keeps one requirement per subject; leaving both would have produced a contradictory duplicate. No requirement text was invented.
- The removed requirements carried `(Reason: ...)` and `(Migration: ...)` notes in the delta, which were deleted together with the matching baseline blocks.
- `(Previously: ...)` annotations were retained verbatim from the delta's MODIFIED blocks for traceability.
- The spec `Purpose` paragraph was updated to remove the stale "effective-reserva marker" clause and state that `ContratoReserva` is the single persisted link. This is a coherence edit, not a new requirement.
- The five ADDED requirements were appended after the existing requirements, preserving baseline order for everything not in the delta.

Source of truth updated: `openspec/specs/solicitud-contrato-reserva/spec.md`.

## Artifacts Archived

| Artifact | Status |
|---|---|
| `proposal.md` | present |
| `specs/solicitud-contrato-reserva/spec.md` | present |
| `design.md` | present |
| `tasks.md` | present (25/25 checked, 0 unchecked) |
| `verify-report.md` | present |
| `apply-progress.md` | present |

## Residual Warnings

**W5 — `RepositorioReservas.guardar` remains a public, contract-less escape hatch.**
It is documented in the merged requirement as a lower-level seam for fixtures and legacy callers; the exactly-one-contract invariant is scoped to controller creation paths. The residual risk is a future production caller bypassing the seam. Accepted design decision, not a spec contradiction.

Non-blocking suggestions carried from verification:

- S1: add a direct reserva-count assertion to `crearReservaMiraSoloBloquesConsumidos`.
- S2: add a reflection assertion for the "no duplicated reserva state" scenario.
- S3: resolved by this archive — `verified_test_count` updated from 209 to 226.
- S4: stale "efectiva" terminology remains in some test names and Javadoc.
- S5: `data/pms.trace.db` retains an earlier failed metadata query.
- S6: `Main.limpiarDatos` constructs two `RepositorioSolicitudes` instances.
- S7: optional later failure point in the mid-transaction rollback test.
- S8: optional clean-room package build.
- S9: the empty-input W6 test should reset the pending-block queue explicitly.

## Process Deviation

The corrective pass ran outside the native attempt ledger because the ledger was in a contradictory state: `begin` reported budget exhausted, `reset` reported a non-terminal attempt, and `finish` reported no active attempt. The maintainer explicitly authorized the pass outside the ledger. No attempt ID or ledger transition exists; `apply-progress.md` is the authoritative record. This is a process deviation, not a code defect, and does not affect the archived verdict.

## Files Touched

| File | Action |
|---|---|
| `openspec/specs/solicitud-contrato-reserva/spec.md` | Merged delta (5 added, 6 modified, 2 removed, 1 renamed) |
| `openspec/config.yaml` | `testing.verified_test_count` 209 -> 226; `last_verified` kept at 2026-10-17 |
| `openspec/changes/archive/2026-10-17-reserva-efectiva-por-referencia/` | Change folder moved here |
| `openspec/changes/archive/2026-10-17-reserva-efectiva-por-referencia/archive-report.md` | Created (this report) |

No `src/` or `data/` file was modified by this archive pass. No commit was created.

## Mechanical Readback

The archived tree was compared against a pre-move recursive snapshot with `diff -r`:

```text
DIFF_EXIT=0
DIFF_OUTPUT_BEGIN

DIFF_OUTPUT_END
```

Empty diff output is the passing evidence. `archive-report.md` is additive and excluded from the comparison because it did not exist in the source snapshot. `git mv` was unavailable because `openspec/` is untracked in this repository; the move used `Move-Item` after the snapshot.

## Verdict

The change is archived. The SDD cycle for `reserva-efectiva-por-referencia` is complete: planned, implemented, verified as `pass_with_warnings`, spec-synced, and closed with one documented residual warning (W5) and no blockers.
