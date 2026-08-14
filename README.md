# JSON to Code Companion

IntelliJ-family plugin. Select a JSON object — in a `.json` file, a
string literal, anywhere — run **Generate Class from JSON**, and get a
real Java POJO or Kotlin `data class` with field types inferred from
the actual JSON values, including nested objects (their own nested/
sibling classes) and arrays (`List<T>`, typed from the first element).

## Why it exists

Ports a pattern that's genuinely popular elsewhere ("Paste JSON as
Code"-style tools, widely used across editors) with no real equivalent
anywhere in JetBrains Marketplace (confirmed by search before building
this, not assumed). A deliberate "port a proven concept" bet — see
`CONSTITUTION.md` §1 for the documented-exception discipline this
follows (same treatment as Refactor Simulator/Bean Copy Companion/
Turbo Log Companion/Change Case Companion).

## Why built this way

- **Never guesses past what the JSON actually shows.** A `null` value,
  or an array that's empty or whose elements don't all share the same
  shape, becomes an honest `Object`/`Any` field with a `// TODO`
  comment — never an invented type.
- **"Same type" for a list of objects means "same shape"**, not merely
  "both objects" — a JSON array of objects with genuinely different
  fields is correctly treated as unable to share one element class,
  not silently merged into a wrong one.
- **A JSON key that isn't a valid identifier gets sanitized**, with
  the original key preserved in a comment on that field — no forced
  Gson/Jackson dependency to add an annotation, same "never force a
  framework dependency" discipline as Bean Copy Companion's Lombok
  handling.
- **Trusts the platform's own JSON parser**, never a hand-rolled one —
  the selection is re-parsed as fresh JSON via the same JSON language
  support already proven in JSON Schema Companion, so it's not fooled
  by anything a real JSON parser wouldn't be.
- **In-memory PSI validation before every write.** The whole generated
  file is parsed in a throwaway PSI copy and checked for syntax errors
  before anything touches disk.
- **Real Java AND Kotlin support**, auto-detected from the invoking
  file when possible, asked explicitly otherwise.
- **100% local** — no network call, no account, no telemetry.

## Usage

Select a JSON object anywhere → right-click → **JSON to Code Companion
→ Generate Class from JSON** → name the root class.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us
at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
