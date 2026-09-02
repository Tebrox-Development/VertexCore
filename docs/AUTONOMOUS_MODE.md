# VertexCore – Autonomous Mode

Dieses Dokument definiert die Zustandsmaschine für den ausdrücklich autorisierten `VertexCore Autopilot`.

Ergänzend gelten [`AI_WORKFLOW.md`](AI_WORKFLOW.md), [`TESTING.md`](TESTING.md) und [`MAINTAINER_STATUS.md`](MAINTAINER_STATUS.md).

## Aktueller Auftrag

VertexCore wird zuerst vollständig auf folgende Plattform migriert:

```text
Java 25
Paper 26.2
```

Die aktuelle Reihenfolge ist verbindlich:

```text
#41 / PR #42  Build-/CI-Plattform
      ↓
#43           Paper-26.2-Runtime-Smoke
      ↓
#44           NexusVault Consumer Compatibility Gate
      ↓
VertexCore-Plattformmigration abgeschlossen
      ↓
erst danach NexusVault separat migrieren
```

## Zustandsmaschine pro Lauf

1. aktuellen `development`-Head lesen,
2. diese Datei, `AI_WORKFLOW.md`, `TESTING.md` und `MAINTAINER_STATUS.md` lesen,
3. Notion `VertexCore Development` gegen den tatsächlichen `development`-Stand abgleichen,
4. offene Issues, PRs, Review-Threads und CI frisch lesen,
5. Concurrent-Worker/Projekt-Lock prüfen,
6. dann genau den nächsten sicheren Zustand bearbeiten.

Priorität:

```text
IN ARBEIT / Nachbesserung
→ READY FOR REVIEW frisch reviewen
→ READY TO MERGE in einem späteren frischen Lauf erneut prüfen und mergen
→ freien vorbereiteten PR reservieren
→ nach Integration einer Voraussetzung den nächsten blockierten Roadmap-Slice frisch vorbereiten
```

## Reservierung

Freie PRs werden ausschließlich nach `AI_WORKFLOW.md` reserviert. Ein Worker hält höchstens einen Slice gleichzeitig.

## Implementierung

- nur PR-Scope,
- keine zusätzlichen Features,
- keine unnötigen Refactorings,
- keine absichtlichen VertexCore-1.x-API-Breaks,
- keine NexusVault-Codeänderungen,
- keine Persistenzformatänderungen innerhalb der Plattformmigration.

## Draft → Ready for Review

VertexCore besitzt `.github/workflows/ready-for-review-bridge.yml`.

Nach vollständiger Implementierung und Prüfung setzt der Worker in Issue und PR den kanonischen Status auf:

```text
**READY FOR REVIEW**
```

oder als exakte Statuszeile:

```text
Arbeitsstatus: READY FOR REVIEW
```

Die Bridge übernimmt GitHub Draft → Ready for Review. Nicht zusätzlich die fehleranfällige Connector-/GraphQL-Mutation über `Repository.fullDatabaseId` verwenden.

Voraussetzung für den produktiven Übergang ist das Repository-/Organization-Secret:

```text
VERTEXCORE_READY_FOR_REVIEW_TOKEN
```

Wenn dieses Secret fehlt oder die Bridge fehlschlägt, den konkreten Blocker dokumentieren und nicht über einen Ersatz-PR umgehen.

## Ready → Draft bei Nachbesserung

Issue und PR zuerst auf `IN ARBEIT`/Nachbesserung zurücksetzen, Branch und Worker-ID beibehalten und anschließend den Workflow `Set PR to Draft` mit der vorhandenen PR-Nummer verwenden. Kein Ersatz-PR.

## Fresh Review

Ein READY-FOR-REVIEW-PR wird gegen den dann aktuellen `development`-Stand vollständig geprüft:

- unveränderter Head,
- Merge-Base/Drift,
- finaler Diff/Scope,
- öffentliche API-/Persistenz-/Lifecycle-Auswirkungen,
- CI auf exakt dem Head,
- offene Review-Threads,
- bekannte Probleme/nicht ausgeführte Tests.

Bei Befund: gleicher PR zurück auf Nachbesserung.

Ohne Befund: Issue/PR auf `READY TO MERGE` setzen. Im selben Zustandsübergang noch nicht mergen.

## Autonomer Merge

Für **diese ausdrücklich vom Nutzer autorisierte VertexCore-Plattformmigration** darf der `VertexCore Autopilot` einen `READY TO MERGE`-PR in einem späteren frischen Lauf per Squash nach `development` mergen, wenn erneut gilt:

- Head unverändert,
- Base/Scope weiterhin sauber,
- alle erforderlichen Gates grün,
- keine offenen Review-Threads,
- keine neue fachliche Entscheidung nötig.

Andere Releases, `main`, Force-Pushes, History-Rewrites, Branch-Protection-, Secret- oder produktive Deployment-Änderungen bleiben verboten.

## Nach einem Merge

1. `development` neu lesen,
2. Status/Issue/Notion nur soweit nötig synchronisieren,
3. wenn dadurch #43 bzw. #44 freigeworden ist, dessen Branch und Draft-PR **frisch vom neuen `development`** vorbereiten,
4. sichere Arbeit im selben Lauf fortsetzen, solange kein echtes Gate/Blocker entgegensteht.

## Abschluss der Plattformmigration

VertexCore gilt erst als fit für den OneBlock-/NexusVault-Folgeschritt, wenn alle drei Gates erfüllt sind:

```text
Java 25 / Paper 26.2 Build       ✅
Paper 26.2 Runtime Smoke         ✅
NexusVault Consumer Gate         ✅
```

Erst danach wird die NexusVault-Plattformmigration im NexusVault-Projekt freigegeben.
