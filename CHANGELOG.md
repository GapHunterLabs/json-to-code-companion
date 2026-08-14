<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# JSON to Code Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- **Generate Class from JSON**: select a JSON object anywhere, get a
  real Java POJO or Kotlin `data class` with types inferred from the
  actual values -- nested objects, typed `List<T>` arrays, sanitized
  field names with the original JSON key preserved in a comment.
- Never invents a type for `null` or an empty/mixed-shape array --
  honest `Object`/`Any` with a `TODO` comment instead.
- In-memory PSI validation before every write.

[Unreleased]: https://github.com/GapHunterLabs/json-to-code-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/json-to-code-companion/commits/0.1.0
