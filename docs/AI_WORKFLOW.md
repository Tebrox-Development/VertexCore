# VertexCore – AI-Entwicklungsworkflow

Dieses Dokument ist die verbindliche Arbeitsgrundlage für parallele Entwicklung in `Tebrox/VertexCore`.

## 1. Branch- und Rollenmodell

```text
master
  ├── development   # NexusVault-Kompatibilitätsstand
  └── next          # aktiver Integrationsbranch
       ├── feat/<issue>-<scope>
       ├── fix/<issue>-<scope>
       ├── refactor/<issue>-<scope>
       ├── test/<issue>-<scope>
       └── docs/<issue>-<scope>
```

- `master` ist stabil/releasefähig.
- `development` ist der von NexusVault direkt konsumierte Kompatibilitätsbranch.
- `next` ist der gemeinsame Integrationsbranch für neue VertexCore-Arbeit.
- Neue funktionale Arbeit basiert auf dem aktuellen `next`.
- Maximal drei Entwickler-Unterchats dürfen gleichzeitig aktiv arbeiten.
- Änderungen werden erst nach bestandenem VertexCore-/NexusVault-Kompatibilitäts-Gate von `next` nach `development` promotet.

### Menschlicher Maintainer

Der menschliche Maintainer entscheidet endgültig über Merges, Promotions nach `development`, Releases, Branch Protection und andere kritische Repository-Aktionen.

### Hauptchat / Maintainer-Reviewer

Der Hauptchat plant Arbeitsrunden, schneidet kleine Issues, bereitet Branches und Draft-PRs vor, prüft fertige Änderungen und hält Roadmap sowie Maintainer-Status aktuell.

### Entwickler-Unterchat

Ein Entwickler-Unterchat übernimmt genau einen vorbereiteten freien Draft-PR gegen `next`, arbeitet nur im dokumentierten Scope, testet seine Änderung, aktualisiert die PR-Beschreibung und setzt den vorhandenen PR danach auf Ready for Review. Er merged und promotet nicht selbst.

## 2. Workflow-Status

Der Textstatus in Issue und PR ist die verbindliche Reservierungsquelle:

```text
Arbeitsstatus: FREI
Worker-ID: —
```

Nach Reservierung:

```text
Arbeitsstatus: IN ARBEIT
Worker-ID: <eindeutige Worker-ID>
```

Nach Abschluss:

```text
Arbeitsstatus: READY FOR REVIEW
Worker-ID: <dieselbe Worker-ID>
```

Falls Workflow-Labels vorhanden sind, werden `in arbeit`, `ready for review` und `ready to merge` synchron auf Issue und PR gepflegt. Fehlende Labels blockieren den Textstatus-Workflow nicht.

## 3. Verbindlicher Vorbereitungsablauf

Der Hauptchat bereitet eine Aufgabe so vor:

1. aktuellen `next`-SHA prüfen,
2. offene Issues/PRs auf Überschneidungen und Abhängigkeiten prüfen,
3. kleines klar abgegrenztes Issue anlegen,
4. Arbeitsbranch frisch von `next` anlegen,
5. leeren Initial-Commit erzeugen,
6. Draft-PR gegen `next` erstellen,
7. vollständigen Arbeitsauftrag in der PR-Beschreibung hinterlegen,
8. PR und Issue mit `Arbeitsstatus: FREI` und `Worker-ID: —` kennzeichnen,
9. freie PRs in `docs/MAINTAINER_STATUS.md` eintragen.

## 4. Reservierung eines freien Draft-PRs

Ein Unterchat darf nur einen offenen Draft-PR gegen `next` übernehmen, dessen PR-Beschreibung ausdrücklich `Arbeitsstatus: FREI` und `Worker-ID: —` enthält.

Reihenfolge:

1. PR unmittelbar vor Reservierung erneut vollständig lesen.
2. PR verlustfrei auf `IN ARBEIT` und eigene eindeutige Worker-ID setzen.
3. PR erneut lesen; nur die weiterhin sichtbare eigene Worker-ID gewinnt die Reservierung.
4. verknüpftes Issue auf denselben Status und dieselbe Worker-ID setzen und erneut prüfen.
5. erst danach Issue und vollständige PR-Beschreibung als Arbeitsauftrag auswerten und implementieren.

Ein Unterchat reserviert niemals mehrere PRs gleichzeitig.

## 5. PR-Beschreibung ist der verbindliche Arbeitsauftrag

Sie enthält mindestens:

```text
Arbeitsstatus: FREI | IN ARBEIT | READY FOR REVIEW
Worker-ID: — | <id>
Issue: #<nummer>
Epic: keines | #<nummer>
Branch: <branch>
Basis: next
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

Wenn Issue und PR unterschiedlich detailliert sind, begrenzt die PR-Beschreibung den tatsächlichen Implementierungsscope.

## 6. Regeln während der Arbeit

Der Unterchat:

- verwendet ausschließlich vorbereiteten Branch und PR,
- erstellt keinen zweiten Branch/PR für dieselbe Aufgabe,
- ändert die Basis nicht eigenmächtig,
- rebased oder force-pusht nicht ohne ausdrückliche menschliche Freigabe,
- verändert keine fremden Arbeitsbranches,
- hält den Scope klein und refactort keine Nachbarbaustellen,
- verändert `ROADMAP.md` und `docs/MAINTAINER_STATUS.md` nicht,
- checkt keine Secrets, lokalen Serverdaten oder Build-Artefakte ein,
- behauptet keine Tests als erfolgreich, die nicht tatsächlich ausgeführt wurden,
- erhält 1.x-API-Kompatibilität zu NexusVault, sofern der Auftrag nicht ausdrücklich einen kompatiblen Adapter/Migrationspfad definiert.

Vor dem Überschreiben einer bestehenden Datei muss ihr aktueller Stand im Zielbranch erneut gelesen werden.

## 7. Kritische VertexCore-Bereiche

Besondere Vorsicht und ausdrückliche Review-Hinweise gelten für:

- `pom.xml` und `.github/workflows/*`,
- `VertexCore.java` / `VertexCoreApi.java`,
- öffentliche Command-/Config-/Database-APIs und Annotationen,
- `DatabaseService`, Queue-/Timeout-/Lifecycle-Logik,
- Flatfile/JDBC-Backends und persistierte Dateinamen/Tabellen/Feldnamen,
- Migrationen und Quelllöschung,
- Concurrency, Write-Fence und Reconciliation,
- Änderungen, die NexusVault-Kompilation oder gespeicherte Daten beeinflussen können.

## 8. Kompatibilitätsregeln zu NexusVault

- `development` wird während normaler Arbeit auf `next` nicht verändert.
- Eine interne Neustrukturierung verwendet in 1.x bei Bedarf Compatibility Adapter statt abruptem API-Bruch.
- Persistierte Legacy-Daten brauchen rückwärtskompatiblen Reader oder explizite Migration.
- Promotion `next -> development` ist ein eigener Maintainer-Schritt nach bestandenem Gate und menschlicher Freigabe.
- NexusVault wird nicht nebenbei auf `next-SNAPSHOT` umgestellt.

## 9. Tests

Mindestens:

```text
FAST: mvn -B -ntp test
VERIFY: mvn -B -ntp verify
```

Bei Persistenz, Migration, Concurrency oder Backends sind passende Regression-/Integrationstests erforderlich. Externe Testkommandos dürfen nur als erfolgreich dokumentiert werden, wenn sie tatsächlich gelaufen sind.

## 10. Ready for Review

Vor Ready for Review aktualisiert der Unterchat die PR-Beschreibung mit aktuellem Head-SHA, Änderungen, betroffenen Dateien, Architektur-/Persistenzauswirkungen, ausgeführten und nicht ausgeführten Tests, manuellen Prüfungen, offenen Punkten und besonders kritischen Review-Stellen.

Danach:

1. Issue und PR auf `Arbeitsstatus: READY FOR REVIEW` setzen,
2. Status erneut prüfen,
3. vorhandenen Draft-PR auf GitHub Ready for Review setzen,
4. nicht mergen.

## 11. Maintainer-Review

Der Hauptchat prüft mindestens Scope, finalen Diff, Merge-Base/Ahead/Behind, öffentliche Contracts, Persistenz/Concurrency, Fehlerfälle, Testangaben, CI, Review-Threads, parallele Überschneidungen und finalen Head-SHA.

Ergebnis genau eines von:

- `MERGE READY`
- `MERGE READY WITH NOTES`
- `NOT MERGE READY`

Merge nach `next` erfolgt erst nach ausdrücklicher menschlicher Freigabe. Promotion nach `development` ist davon getrennt und benötigt zusätzlich das NexusVault-Kompatibilitäts-Gate.

## 12. Ohne menschliche Freigabe unzulässig

- Merge
- Promotion/Merge nach `development` oder `master`
- Force-Push / History-Rewrite
- Löschung gemeinsam genutzter Branches
- Branch-Protection-Änderungen
- Release/Deployment
- destruktive Migration produktiver Daten
- Secret-/Credential-Änderungen mit Außenwirkung
