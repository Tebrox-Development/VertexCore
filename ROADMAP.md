# VertexCore Roadmap

VertexCore wird schrittweise gehärtet, ohne den von NexusVault konsumierten `development`-Stand unkontrolliert zu verändern.

## Branch- und Release-Grenze

- `master`: stabiler Release-Stand.
- `development`: NexusVault-Kompatibilitätsstand; wird nur nach bestandenem Kompatibilitäts-Gate aktualisiert.
- `next`: aktiver Integrationsbranch für die folgende VertexCore-Entwicklung.
- Feature-/Fix-/Test-Branches starten von `next` und zielen per PR auf `next`.

Öffentliche APIs, die NexusVault bereits verwendet, werden in 1.x nicht absichtlich inkompatibel geändert. Persistierte Legacy-Formate dürfen nur mit rückwärtskompatiblem Reader oder expliziter Migration verändert werden.

## 1.1.0 – Safety & NexusVault Compatibility

- Cross-Project-Kompatibilitäts-Gate für VertexCore und NexusVault.
- Migration mit identischer Quelle und identischem Ziel sicher verhindern.
- Flatfile-IDs kollisionsfrei und verlustfrei abbilden, inklusive Legacy-Kompatibilität.
- Queue-/Timeout-Semantik korrigieren, ohne öffentliche Aufruf-API zu brechen.
- Build-/Dokumentationsgrundlagen bereinigen (`release=21`, unterstützten Paper-Bereich dokumentieren, README korrigieren).

## 1.1.1 – Command Framework Hardening

- Methodensignaturen beim Registrieren fail-fast validieren.
- Primitive/Wrappers konsistent auflösen; Resolver-Erweiterung sauber öffentlich anbieten.
- Suggestions erst nach Permission-/Visibility-Prüfung ausführen.
- Alias-/Label-Kollisionen deterministisch erkennen und Registrierung atomar machen.

## 1.1.2 – Config Reliability

- Runtime-Config cachen und expliziten Reload unterstützen.
- Konfigurationswrites atomar ausführen und vor Reparaturen sichern.
- Typkonvertierung strikt validieren; fehlerhafte Boolean-Werte nicht stillschweigend akzeptieren.
- Defaults, Maps und Nullwerte robust behandeln.

## 1.2.0 – Lifecycle & API Stability

- Owner-scoped Cleanup für Commands, Queues, Backends und Registries.
- `Database.close()`-Semantik sicherer machen, in 1.x kompatibel/deprecated statt abrupt brechen.
- `VertexCoreApi`-Lifecycle mit explizitem Shutdown/Reset härten.
- tote oder doppelte Timeout-Infrastruktur entfernen bzw. zentralisieren.

## 1.3.0 – Stable Persistence Identity

- explizite stabile Tabellenidentitäten mit Legacy-Fallback.
- explizite persistierte Feldnamen und Aliase; Vererbung berücksichtigen.
- Schema-/Versionsinformationen für persistierte VertexCore-Objekte ermöglichen.

## 1.4.0 – Migration 2.0

- strukturierter `MigrationReport` mit SUCCESS/PARTIAL_FAILURE/FAILED.
- echtes Paging/Cursor/Streaming statt vollständigem `loadAllRaw`.
- Quelllöschung nur nach verifiziert erfolgreichem Zielwrite.
- vollständiger Dry-Run und sichere Wiederholbarkeit.

## 1.5.0 – Quality & Observability

- breite Persistenz-/Concurrency-Regressionsmatrix.
- reale MariaDB/MySQL-Integrationstests, bevorzugt mit Testcontainers.
- Diagnoseoberfläche ohne Secrets für Backends, Pools, Queues und Pending Writes.
- belastbare Metriken für Latenz, Timeouts, Reconciliation und Migrationen.

## 2.0.0 – API / Implementation Split

- Architektur und Migrationspfad für eine mögliche Trennung in `vertexcore-api` und `vertexcore-paper`.
- geplante Breaking Cleanups ausschließlich mit dokumentiertem Migrationspfad.
- NexusVault wird erst bewusst auf 2.x migriert, niemals implizit durch einen beweglichen Snapshot.

## Verbindliches Kompatibilitäts-Gate

Ein VertexCore-PR darf öffentliche APIs, die NexusVault verwendet, nicht inkompatibel ändern. Vor Promotion von `next` nach `development` müssen mindestens gelten:

1. VertexCore FAST-Tests und CI erfolgreich.
2. Relevante VertexCore-Integrationstests erfolgreich.
3. aktueller NexusVault-`dev` kompiliert und testet gegen den zu promotenden VertexCore-Kandidaten.
4. Persistenzänderungen besitzen einen rückwärtskompatiblen Reader oder eine explizite sichere Migration.
5. menschliche Freigabe für die Promotion liegt vor.
