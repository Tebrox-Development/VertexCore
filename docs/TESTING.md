# VertexCore – Teststrategie

Diese Datei definiert die Mindestgates für autonome VertexCore-Arbeit.

## FAST

Für normale Codeänderungen mindestens:

```text
mvn -B -ntp verify
```

Der Build muss auf der für den jeweiligen Scope festgelegten Java-Toolchain laufen.

## Plattform-Kompatibilität

Für den Wechsel auf Paper 26.2 / Java 25 sind folgende Gates verbindlich:

1. Maven-Build mit Java 25 erfolgreich.
2. Kompilierung gegen Paper API 26.2 erfolgreich.
3. vorhandene Unit-/Regressionstests erfolgreich.
4. VertexCore-JAR lässt sich auf einer reproduzierbaren Paper-26.2-Runtime laden.
5. Server erreicht einen eindeutig erkennbaren erfolgreichen Startup-Zustand ohne VertexCore-Enable-Fehler.
6. kontrollierter Shutdown endet ohne VertexCore-Lifecycle-Fehler.
7. der aus dem aktuellen NexusVault-`dev` abgeleitete Consumer-Contract kompiliert und testet erfolgreich gegen den geprüften VertexCore-Stand.

## Runtime Smoke

Der Runtime-Smoke ist automatisierbar und reproduzierbar und darf keine produktiven Serverdaten, Secrets oder externen Dienste benötigen.

Der Smoke prüft mindestens:

```text
Paper 26.2 startet
→ VertexCore wird geladen
→ VertexCore wird erfolgreich aktiviert
→ keine uncaught VertexCore-Exception im Startup
→ Server wird kontrolliert beendet
→ VertexCore wird sauber deaktiviert
```

Die CI verwendet dafür `.github/workflows/runtime-smoke.yml` und `scripts/runtime-smoke.sh`.

Der Harness:

- baut zuerst den aktuellen PR-Head mit `mvn -B -ntp verify`,
- lädt ausschließlich den explizit gepinnten stabilen Paper-Stand `26.2` Build `121` über den offiziellen PaperMC-Fill-Service,
- verwendet das gerade gebaute `target/vertexCore-1.1.0-SNAPSHOT.jar`,
- startet eine frische temporäre Runtime unter `target/runtime-smoke`,
- wartet fail-closed auf den Paper-Ready-Marker und `VertexCore enabled.`,
- wertet typische VertexCore-Load-/Enable-Exceptions als Fehler,
- sendet anschließend kontrolliert `stop`,
- verlangt einen erfolgreichen Server-Exit sowie `VertexCore disabled.`,
- behandelt Disable-/Lifecycle-Exceptions als Fehler.

Der Smoke benötigt Netzwerkzugriff ausschließlich zum offiziellen PaperMC-Download-Service und erzeugt keine dauerhaften externen Zustände.

Sobald fachliche Runtime-Smokes existieren, werden zusätzlich repräsentative Wege für Config, Commands und Persistenz geprüft.

## Datenbank-/Persistenzänderungen

Bei Änderungen an Datenbank-, Migration-, Queue- oder Persistenzcode sind passende Regressionstests Pflicht. Kritische Fälle umfassen insbesondere:

- identische Source-/Target-Backends,
- Partial/Unknown Write Outcomes,
- Timeout,
- parallele Writes,
- Shutdown/Drain,
- Legacy-Datenformate,
- atomische bzw. fail-closed Übergänge.

## Consumer-Kompatibilität

VertexCore 1.x bleibt kompatibilitätsorientiert. Ein grüner VertexCore-Build ist notwendig, aber bei Änderungen an öffentlicher API nicht hinreichend.

Das automatisierte Gate verwendet `.github/workflows/nexusvault-consumer.yml`, `scripts/nexusvault-consumer-gate.sh` und `NexusVaultConsumerCompatibilityTest`.

Der Consumer-Check:

- baut den aktuellen VertexCore-PR-Head unter Java 25,
- verwendet keinen NexusVault-Checkout und benötigt deshalb keine Cross-Repo-Credentials,
- hält den geprüften realen Consumer-Ausgangspunkt als vollständigen NexusVault-`dev`-SHA fest,
- bildet die von diesem NexusVault-Stand tatsächlich verwendeten VertexCore-1.x-Typen, Konstruktoren und Methoden als Java-Compile-Contract im VertexCore-Testbaum ab,
- lässt diesen Contract im normalen Maven-`test-compile` gegen exakt die VertexCore-Quellen des aktuellen PR-Heads kompilieren,
- führt anschließend den zugehörigen JUnit-Gate-Test aus,
- schlägt fail-closed fehl, sobald eine von NexusVault verwendete VertexCore-Signatur nicht mehr source-kompatibel ist,
- schreibt weder in das NexusVault-Repository noch in externe dauerhafte Zustände.

Aktueller Snapshot für den Plattform-Slice #44:

```text
Tebrox-Development/NexusVault dev
412becf44e6de104cfb0804f7735ff012516c0cb
```

Der Snapshot wurde für die Gate-Pflege ausschließlich read-only über GitHub gelesen. Wenn sich NexusVault-`dev` oder dessen VertexCore-Nutzung ändert, muss der Contract gegen den dann aktuellen `dev`-SHA read-only aktualisiert werden. Das Gate ersetzt bewusst keinen späteren NexusVault-Paper-/BentoBox-Migrationslauf und verändert NexusVault nicht.

Bei relevanten API-Änderungen muss der PR zusätzlich dokumentieren:

- welche öffentliche API betroffen ist,
- ob NexusVault diese API verwendet,
- warum die Änderung source-/binary-kompatibel bleibt oder welcher Übergangsadapter vorhanden ist.

## Testangaben in PRs

Jeder Worker dokumentiert getrennt:

```text
Ausgeführt:
- <Befehl/Test> -> <Ergebnis>

Nicht ausgeführt:
- <Test> -> <Grund>
```

Nicht ausgeführte Tests dürfen niemals als erfolgreich dargestellt werden.
