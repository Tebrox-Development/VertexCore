---
name: Bug
about: Reproduzierbarer Fehler oder Sicherheits-/Datenintegritätsfehler in VertexCore
title: "fix: "
labels: "bug"
assignees: ""
---

Arbeitsstatus: FREI
Worker-ID: —

## Problem

Was ist fehlerhaft?

## Risiko / Auswirkung

Welche Daten-, Concurrency-, API- oder Runtime-Auswirkung besteht?

## Übergeordnetes Epic / Roadmap-Bezug

- Epic: #{{EPIC_ISSUE}}
- Roadmap: `{{ROADMAP_SECTION_OR_ITEM}}`

## Reproduktion / betroffener Pfad

- `{{PATH_OR_COMPONENT}}`

## Nicht-Ziel

Was darf dieser Fix nicht nebenbei verändern?

## Abhängigkeiten

- Blocked by: keine
- Blocking: keine

## Akzeptanzkriterien

- [ ] Fehlerpfad reproduzierbar abgedeckt
- [ ] Regressionstest schlägt vorher fehl und nach Fix erfolgreich
- [ ] keine inkompatible 1.x-API-Änderung

## Erwartete Testtiefe

- [ ] FAST
- [ ] VERIFY
- [ ] INTEGRATION

## NexusVault-/Persistenzrisiko

{{RISK_AND_COMPATIBILITY_NOTE}}

## Vorgesehener Branch

```text
fix/{{ISSUE_NUMBER}}-{{SHORT_SCOPE}}
```
