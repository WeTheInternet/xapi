# Task: Escape the customized GWT/compiler fork

- Status: backlog
- Created: 2026-08-05
- Scope: customized GWT/Eclipse compiler binaries, magic-method injection, Java 8 lock-in, replacement architecture

## Problem

Legacy magic-method-generator behavior depends on customized GWT/compiler jars whose source changes were lost. This locks relevant code to Java 8-era tooling and makes clean bootstrap and upgrades unsafe.

## Current migration policy

Package the hacked binaries reproducibly in the portable bootstrap artifact for now. Move fork-dependent GWT code behind `net.wti.legacy`; working GWT support is not required for the active desktop/Android core during that split.

## Preferred direction

Replace GWT magic-method injection with transitive source transpilation:

- generic/base Java modules expose transitive source-module trees;
- those trees are fed into a Java compiler plugin;
- static analysis can trigger Java-to-Java source transformations;
- stock GWT/J2CL versions consume the transformed Java rather than requiring a maintained fork.

## Secondary recovery direction

Perform bytecode-to-bytecode comparison between stock and customized Eclipse compiler classes to reconstruct the lost source changes. Use this as forensic documentation or a migration bridge, not a renewed commitment to maintaining a fork.

## Required investigation

Inventory hacked coordinates/classes, magic-method entry points, consumer coverage, and the minimum behavior that a source-transpilation replacement must preserve.
