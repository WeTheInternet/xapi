# Task: Deprecate and remove inert `schemaLocation`

- Status: ready
- Created: 2026-08-05
- Scope: modern settings parser/model/schema docs, root schema, test fixtures

## Current evidence

Modern `XapiSchemaParser` accepts `schemaLocation` and describes it as a generated-schema output location. After parsing, it unconditionally sets the metadata location to the actual input `schema.xapi` path. Remaining reads are diagnostic/logging uses; no modern code generates the promised schema script. The root value points to nonexistent `schema/schema.gradle`.

## Proposed implementation

- Separate diagnostic source-file location from any future output-location concept.
- Deprecate or reject the DSL attribute with a clear migration message, then remove it from root schema and fixtures.
- Update `META-INF/xapi/xapi-schema.xapi` and settings-plugin docs.
- Search external consumers before making rejection immediate; a compatibility phase may simply warn and ignore.

## Verification

Parser/settings tests without `schemaLocation`, explicit compatibility behavior for old schemas, and main schema regeneration.
