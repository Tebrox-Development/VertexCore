# VertexCore – Maintainer Status

Diese Datei ist die operative Übersicht für vorbereitete und laufende autonome Arbeit.

Verbindliche Regeln stehen in [`docs/AI_WORKFLOW.md`](AI_WORKFLOW.md).

## Status

```text
FREI             = vorbereiteter Draft-PR kann reserviert werden
IN ARBEIT        = durch genau einen Worker reserviert
READY FOR REVIEW = Worker ist fertig, Maintainer-Review ausstehend
READY TO MERGE   = Maintainer-Review erfolgreich; Merge weiterhin nur nach menschlicher Freigabe
```

## Aktueller Plattform-Umbau

Zielplattform:

```text
Java 25
Paper 26.2
```

Reihenfolge:

1. Build-/CI-Plattform auf Java 25 und Paper 26.2 heben.
2. reproduzierbaren Paper-26.2-Runtime-Smoke etablieren.
3. VertexCore-Funktionsbereiche auf der neuen Runtime gezielt smoken/regressionssichern.
4. erst nach erfolgreichem VertexCore-Abschluss NexusVault separat nachziehen.

## Vorbereitete PRs

Noch keine. Neue vorbereitete PRs werden hier nach dem Schema eingetragen:

```text
PR #<nr> | <titel>
Status: FREI | IN ARBEIT | READY FOR REVIEW | READY TO MERGE
Worker-ID: — | <id>
Issue: #<nr>
Branch: <branch>
Scope: <kurzbeschreibung>
Abhängigkeit: keine | PR/Issue
```

## Maintainer-Regel

- maximal drei parallele Worker,
- Plattform-/Build-Slices vor Runtime-/Consumer-Slices,
- kein Worker merged selbst,
- keine NexusVault-Codeänderung aus VertexCore-PRs heraus,
- keine API-Breaks im 1.x-Kompatibilitätspfad.
