# VertexCore – Maintainer Status

Diese Datei ist die menschlich lesbare Übersicht für vorbereitete und aktive Arbeits-PRs gegen `next`. Verbindlich für Reservierungen bleiben die unmittelbar erneut gelesenen PR-/Issue-Beschreibungen.

## Branches

- stabil: `master`
- NexusVault-Kompatibilität: `development`
- aktive Integration: `next`

## Aktuelle Arbeitsrunde

Die erste Runde des Epics #3 (`1.1.0 – Safety & NexusVault Compatibility`) ist für maximal drei parallele Entwickler-Unterchats vorbereitet.

Gemeinsamer Vorbereitungs-Base-SHA: `018edbed0ac40dec4833712ebb2b50e511cc3f1c`

Die Arbeitsbranches wurden vor diesem reinen Maintainer-Bookkeeping-Commit erstellt. Dadurch können sie gegenüber dem aktuellen `next` ausschließlich um diese Statusdokumentation zurückliegen; das ist keine funktionale Abhängigkeit und darf nicht durch eigenmächtiges Rebase/Force-Push korrigiert werden.

## Freie vorbereitete Draft-PRs

| PR | Issue | Epic | Branch | Head-SHA | Arbeitsstatus |
| --- | --- | --- | --- | --- | --- |
| #38 | #11 | #3 | `fix/11-reject-same-migration-target` | `43661085de488f5e16d5de79514fea8c58602826` | FREI |
| #39 | #12 | #3 | `fix/12-flatfile-id-encoding` | `499578a5217188500f259f94db35c162cbcfad08` | FREI |
| #40 | #13 | #3 | `fix/13-queue-timeout-scope` | `4a45133848837d7a73f3cf296196daa07ccc90b2` | FREI |

Reservierung erfolgt ausschließlich nach `docs/AI_WORKFLOW.md`: zuerst PR auf `IN ARBEIT` + eigene Worker-ID, erneut prüfen, danach verknüpftes Issue synchronisieren und erneut prüfen.

## In Arbeit

Keine.

## Ready for Review

Keine.

## Ready to Merge

Keine.

## Nächste vorbereitete Issues nach dieser Runde

Noch ohne Arbeitsbranch/Draft-PR, damit die Grenze von höchstens drei aktiven Unterchats eindeutig bleibt:

- #14 – NexusVault-Kompatibilitäts-Gate
- #15 – Java-21-Build-/Dokumentationsabgleich
- danach Epic #4 gemäß dessen Abhängigkeiten

Der Hauptchat bereitet weitere Branches und Draft-PRs erst vor, wenn Kapazität frei ist und der aktuelle `next`-Stand erneut geprüft wurde.

## Promotion `next -> development`

Eine Promotion ist nur zulässig, wenn:

1. VertexCore-Tests/CI für den finalen Kandidaten erfolgreich sind,
2. aktueller NexusVault-`dev` gegen genau diesen Kandidaten kompiliert/testet,
3. Persistenzkompatibilität bzw. notwendige Legacy-Pfade geklärt sind,
4. keine offenen Blocker aus dem zu promotenden Scope bestehen,
5. die ausdrückliche menschliche Freigabe vorliegt.

Normale Entwickler-Unterchats mergen weder nach `next` noch promoten sie nach `development`.
