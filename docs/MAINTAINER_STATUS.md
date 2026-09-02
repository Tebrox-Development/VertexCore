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
3. NexusVault als realen Consumer gegen den neuen VertexCore-Stand prüfen.
4. erst nach erfolgreichem VertexCore-Abschluss NexusVault separat auf Paper 26.2 / aktuellen BentoBox-Stand migrieren.

## Vorbereitete PRs

### PR #42 | Java 25 und Paper 26.2 als Build-Basis etablieren

```text
Status: FREI
Worker-ID: —
Issue: #41
Branch: infra/41-java25-paper26-platform
Scope: Java-25-Toolchain, Paper-26.2-API, CI auf Organization Self-Hosted Runner; keine Features
Abhängigkeit: keine
```

## Geplante Folge-Slices

Diese Issues sind bewusst noch **nicht** als Draft-PR vorbereitet. Der Maintainer erstellt den jeweiligen Branch/PR erst nach Integration der Voraussetzung frisch vom dann aktuellen `development`.

### Issue #43 | Paper-26.2-Runtime-Smoke

```text
Status: BLOCKIERT
Abhängigkeit: #41 integriert
Danach: Branch + Draft-PR frisch von development vorbereiten
```

### Issue #44 | NexusVault Consumer Compatibility Gate

```text
Status: BLOCKIERT
Abhängigkeit: #41 und #43 integriert
Danach: Branch + Draft-PR frisch von development vorbereiten
```

## Maintainer-Regel

- maximal drei parallele Worker,
- Plattform-/Build-Slices vor Runtime-/Consumer-Slices,
- abhängige PRs nicht vor ihrer Voraussetzung auf veralteter Basis vorbereiten,
- kein Worker merged selbst,
- keine NexusVault-Codeänderung aus VertexCore-PRs heraus,
- keine API-Breaks im 1.x-Kompatibilitätspfad.
