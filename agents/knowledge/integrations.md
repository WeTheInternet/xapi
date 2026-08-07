# Integration Boundaries

## `/opt/wti`

The original XApi consumer and an important compatibility boundary. Trace concrete consumption before changing published coordinates, Gradle behavior, injection, model, GWT, or generated-source semantics.

## Kukunochi

Consumes selected XApi artifacts and `net.wti.gradle.modern:xapi-gradle-settings-plugin:0.5.1` from XApi's `repo/`. Work originating from Kukunochi authorizes only the smallest demonstrated XApi compatibility change and corresponding local publication/consumer verification.

Kukunochi and `/opt/wti-ui` define the first real dependency closure for separating the actively maintained modern core from `net.wti.legacy`. Desktop/Android behavior is higher priority than retaining GWT during that split.

## `/opt/wti-ui`

Consumes the modern settings/schema generator and local XApi artifacts. Generator changes should receive settings-plugin regressions before republishing and downstream regeneration/POM verification.

## `/opt/collide`

Historical, low-priority consumer that may no longer compile. Preserve potentially relevant compatibility knowledge, but do not make it a routine verification target without explicit scope.

Do not edit consumer repositories unless the user explicitly places them in scope. Use the two-file `/tmp/codex-handoffs/` protocol for independent cross-repository sessions.
