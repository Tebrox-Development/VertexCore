# VertexCore – AI-Entwicklungsworkflow

Dieses Dokument ist die verbindliche Arbeitsgrundlage für autonome und parallele Entwicklung in `Tebrox-Development/VertexCore`.

## Branch- und Rollenmodell

```text
main
  └── development
       ├── feat/<issue>-<scope>
       ├── fix/<issue>-<scope>
       ├── refactor/<issue>-<scope>
       ├── test/<issue>-<scope>
       ├── infra/<issue>-<scope>
       └── docs/<issue>-<scope>
```

- `main` bleibt stabil/releasefähig.
- `development` ist der gemeinsame Integrationsbranch.
- Neue Arbeit basiert auf dem jeweils aktuellen `development`.
- Ein Worker übernimmt genau einen vorbereiteten Draft-PR gleichzeitig.
- Worker mergen niemals selbst.
- Merge, Force-Push, History-Rewrite und Releases benötigen menschliche Freigabe.

## Source of Truth

- **Notion:** langfristige Roadmap, Architekturziele, Konzepte und Entscheidungen.
- **GitHub Issues:** konkrete umsetzbare Arbeit und Abhängigkeiten.
- **Draft-PR-Beschreibung:** verbindlicher Arbeitsauftrag für den Worker.
- **`docs/MAINTAINER_STATUS.md`:** operative Übersicht über vorbereitete/aktive Arbeiten.
- **`development`:** tatsächlicher integrierter Code- und Dokumentationsstand.

## Vorbereitungsablauf des Maintainers

1. aktuellen `development`-SHA prüfen,
2. offene Issues/PRs auf Überschneidung prüfen,
3. kleinen, unabhängigen Scope als Issue anlegen,
4. Arbeitsbranch frisch von `development` erstellen,
5. Draft-PR gegen `development` vorbereiten,
6. PR-Beschreibung mit vollständigem Auftrag füllen,
7. Issue und PR mit `Arbeitsstatus: FREI` und `Worker-ID: —` kennzeichnen,
8. PR in `docs/MAINTAINER_STATUS.md` eintragen.

## Reservierung durch einen Worker

Ein Worker darf nur einen offenen Draft-PR gegen `development` übernehmen, dessen Beschreibung exakt enthält:

```text
Arbeitsstatus: FREI
Worker-ID: —
```

Reservierung:

1. PR unmittelbar vor Reservierung erneut lesen.
2. PR auf `Arbeitsstatus: IN ARBEIT` und eine eindeutige eigene `Worker-ID` setzen.
3. PR erneut lesen und sicherstellen, dass weiterhin exakt die eigene Worker-ID steht.
4. Verknüpftes Issue auf denselben Status und dieselbe Worker-ID setzen.
5. Erst danach Issue und vollständige PR-Beschreibung als Arbeitsauftrag lesen und implementieren.

Bei verlorener Reservierung: nichts implementieren und einen anderen freien PR suchen.

## Verbindlicher PR-Auftrag

Jeder vorbereitete PR enthält mindestens:

```text
Arbeitsstatus: FREI | IN ARBEIT | READY FOR REVIEW
Worker-ID: — | <id>
Issue: #<nummer>
Branch: <branch>
Basis: development
Base-SHA: <sha>
Head-SHA: <sha>

Ziel:
...

Nicht-Ziel:
...

Vor Beginn lesen:
- ...

Nicht verändern:
- ...

Akzeptanzkriterien:
- ...

Erwartete Tests:
- ...
```

Die PR-Beschreibung begrenzt den tatsächlichen Scope. Nebensächliche Refactorings oder zusätzliche Features sind nicht erlaubt.

## Regeln während der Arbeit

- ausschließlich den vorbereiteten Branch/PR verwenden,
- keinen zweiten PR für denselben Auftrag erstellen,
- keine fremden Arbeitsbranches verändern,
- kein Rebase/Force-Push ohne Freigabe,
- `ROADMAP.md` und `docs/MAINTAINER_STATUS.md` nicht eigenmächtig verändern,
- keine Secrets, lokalen Serverdaten oder Build-Artefakte committen,
- Tests nur als erfolgreich dokumentieren, wenn sie wirklich ausgeführt wurden,
- vor Überschreiben bestehender Dateien deren aktuellen Branch-Stand erneut lesen.

## VertexCore-Architekturgrenzen

VertexCore ist Infrastruktur, kein Gameplay- oder NexusVault-Domain-Plugin.

VertexCore darf gemeinsame technische Mechanik besitzen für:

- Konfiguration,
- Commands,
- Persistenz-/Datenbankabstraktion,
- Async-/Queue-/Lifecycle-Infrastruktur,
- Migrationen,
- Diagnose technischer Infrastruktur.

Nicht nach VertexCore verschoben werden dürfen fachliche Regeln von Consumer-Plugins wie NexusVault oder OneBlockCore.

### VertexCore 1.x Kompatibilitätsregel

- keine absichtlichen Breaking Changes an bereits veröffentlichten oder von NexusVault verwendeten öffentlichen APIs,
- interne Umbauten hinter bestehenden Contracts oder über kompatible Übergangsadapter,
- persistierte Formate nur rückwärtskompatibel oder mit expliziter Migration ändern,
- API-Breaks werden für eine bewusst geplante Major-Version gebündelt.

## Plattformziel 2026

Verbindliches Ziel der aktuellen Kompatibilitätsarbeit:

```text
Runtime-JVM: Java 25
Server:      Paper 26.2
Build:       Maven
```

Der Plattformwechsel ist ein Compatibility-Scope. Er darf nicht mit fachlichen Feature-Änderungen vermischt werden.

## Besonders kritische Bereiche

- `pom.xml`,
- `.github/workflows/*`,
- Plugin-Bootstrap/Lifecycle,
- öffentliche Consumer-APIs,
- Config- und Persistenzformate,
- Queue-/Async-/Timeout-Verhalten,
- Migrationen,
- Shutdown/Drain,
- Datenbank-Backends.

Änderungen dort müssen in der PR-Übergabe ausdrücklich genannt werden.

## Ready for Review

Vor Abschluss aktualisiert der Worker die PR-Beschreibung mindestens auf:

```text
Arbeitsstatus: READY FOR REVIEW
Worker-ID: <id>
Head-SHA: <aktueller sha>

Geändert:
- ...

Betroffene Dateien:
- ...

Architektur-/API-Auswirkungen:
- keine | ...

Persistenz-/Konfigurationsauswirkungen:
- keine | ...

Ausgeführt:
- <test> -> <ergebnis>

Nicht ausgeführt:
- <test> -> <grund>

Bekannte Probleme / offene Punkte:
- keine | ...

Besonders kritisch für Review:
- ...
```

Danach wird dasselbe Issue auf `READY FOR REVIEW` gesetzt und der bestehende Draft-PR auf GitHub Ready for Review gestellt. Der Worker merged nicht.

## Maintainer-Review

Der Maintainer prüft mindestens:

1. aktuellen `development`-Stand und Merge-Base,
2. finalen Diff und Scope,
3. Architektur/API-/Persistenzauswirkungen,
4. CI und Testangaben,
5. offene Review-Threads,
6. parallele PR-Überschneidungen,
7. bekannte Probleme und nicht ausgeführte Tests.

Ergebnis: `MERGE READY`, `MERGE READY WITH NOTES` oder `NOT MERGE READY`.

Merge erfolgt erst nach ausdrücklicher menschlicher Freigabe.
