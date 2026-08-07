# net.wti.core Guide

This is the first routine bootstrap build and publishes foundational `net.wti.core:*:0.5.1` artifacts used by modern Gradle tooling and the main build.

- Run commands from this directory with its Gradle 8.11.1 wrapper.
- Preserve the Java 8 toolchain/source compatibility unless a dedicated fork-retirement plan changes the constraint.
- Use focused subproject tests for code changes; broad scripts normally compile but skip tests.
- Publication is to the root XApi `repo/` through `gradle/xapi-modern.gradle`; leaf `xapiPublish` reaches every publication targeting `xapiLocal`, and root `publishRequired` aggregates all leaf `xapiPublish` tasks.
- Parser/language and functional-utility changes can affect the settings parser, Gradle plugins, generators, and many consumers. Trace concrete downstream use and avoid opportunistic API cleanup.
