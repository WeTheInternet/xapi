# Task: Run the portable bootstrap on native Windows

- Status: ready
- Created: 2026-08-05
- Scope: bootstrap extraction, PowerShell verification, wrapper paths, full publication

## Goal

On the target Windows machine, clone XApi onto an NTFS path, extract
`xapi-bootstrap-seed-0.5.1.zip` into the checkout, set `XAPI_JAVA8_HOME`, and run:

```powershell
& .\bootstrap\verify-bundle.ps1
& .\fullBuild.ps1 -Shadow
```

Confirm that checkout, extraction, schema indexing, all five build stages, and local Maven
publication complete without illegal filenames, path-separator assumptions, or wrapper
distribution failures. Record PowerShell/Windows version, Java 17 and Java 8 distributions,
commands, and any path-length policy required.

Linux clean-fixture verification and archive-name auditing already pass. This task is the
remaining native-platform proof, not authorization for unrelated Gradle modernization.
