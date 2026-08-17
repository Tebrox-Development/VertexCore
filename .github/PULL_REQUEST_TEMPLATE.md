# Arbeits- und Übergabeakte

Arbeitsstatus: FREI
Worker-ID: —

## Zuordnung

- Issue: #{{ISSUE}}
- Epic: keines / #{{EPIC}}
- Branch: `{{BRANCH}}`
- Basis: `next`
- Base-SHA: `{{BASE_SHA}}`
- Head-SHA: `{{HEAD_SHA}}`

## Abhängigkeiten

- Blocked by: keine / #{{ISSUE}}
- Blocking: keine / #{{ISSUE}}

## Ziel

{{WHAT_SHOULD_WORK_AFTER_THIS_PR}}

## Nicht-Ziel

{{WHAT_MUST_NOT_BE_CHANGED_OR_SOLVED_HERE}}

## Vor Beginn lesen

- `docs/AI_WORKFLOW.md`
- `{{PATH}}`

## Nicht verändern

- `development`
- `master`
- `ROADMAP.md`
- `docs/MAINTAINER_STATUS.md`
- NexusVault-Abhängigkeit auf `development-SNAPSHOT`, sofern nicht ausdrücklich Teil eines Maintainer-Kompatibilitätstests

## Akzeptanzkriterien

- [ ] {{CRITERION_1}}
- [ ] {{CRITERION_2}}
- [ ] {{CRITERION_3}}

## Änderungen

- noch nicht implementiert

## Betroffene Dateien

- noch nicht implementiert

## Architektur- / Schnittstellenänderungen

- [ ] keine
- [ ] vorhanden und 1.x-kompatibel dokumentiert

## Konfigurations- / Persistenzänderungen

- [ ] keine
- [ ] vorhanden; Legacy-Kompatibilität/Migration dokumentiert

## NexusVault-Kompatibilität

- [ ] keine relevante Auswirkung
- [ ] additive kompatible Änderung
- [ ] Cross-Project-Test erforderlich

## Teststufe

- [ ] FAST (`mvn -B -ntp test`)
- [ ] VERIFY (`mvn -B -ntp verify`)
- [ ] INTEGRATION
- [ ] begründete Abweichung

### Tatsächlich ausgeführt

```text
noch nicht ausgeführt
```

### Nicht ausgeführt

```text
noch zu dokumentieren
```

## Manuell geprüft

- noch nicht

## Bekannte Probleme / offene Punkte

- keine bekannt vor Arbeitsbeginn

## Besonders kritisch für das Review

- {{HOT_FILE_STATE_TRANSITION_SECURITY_PERSISTENCE_ETC}}

## Ready-for-Review-Check

- [ ] Issue-Scope abgeschlossen oder Abweichung offen dokumentiert
- [ ] PR-Beschreibung aktuell
- [ ] Head-SHA aktuell
- [ ] relevante Tests tatsächlich ausgeführt
- [ ] nicht ausgeführte Tests begründet
- [ ] keine unbeabsichtigten temporären Dateien im Diff
- [ ] bekannte Probleme dokumentiert

---

## Maintainer-Review

- [ ] Issue und Abhängigkeiten geprüft
- [ ] finalen Diff gegen aktuellen `next` geprüft
- [ ] Merge-Base / Ahead / Behind geprüft
- [ ] Scope und 1.x-API-Kompatibilität geprüft
- [ ] Persistenz / Migration / Concurrency geprüft, falls betroffen
- [ ] Tests und CI geprüft
- [ ] offene Review-Threads geprüft
- [ ] NexusVault-Kompatibilitätsauswirkung geprüft
- [ ] finalen Head-SHA erneut geprüft

### Merge-Status

- [ ] `MERGE READY`
- [ ] `MERGE READY WITH NOTES`
- [ ] `NOT MERGE READY`

### Maintainer-Notizen

{{REVIEW_NOTES}}

Merge nach `next` erfolgt nur nach ausdrücklicher menschlicher Freigabe. Promotion `next -> development` ist ein separater Maintainer-Schritt nach bestandenem Cross-Project-Gate.
