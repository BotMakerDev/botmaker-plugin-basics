# CLAUDE.md — botmaker-plugin-basics

Guidance for Claude Code working in this module. The umbrella's `CLAUDE.md` is the map of how the
repositories fit together; this file is what is true *here*. How the module got here — `Settings.forPlugin`,
the `PluginData` store, `ParameterStore`, `JdkText`, the grammars, `@Managed` and `@Param` before they moved to
the contract, the bot-safe rule — is in `../docs/refactor/31-umbrella-history.md` (*basics*), and the text this
file carried until 2026-09-28 is `git show ee3690b:CLAUDE.md` in this repository.

## What this module is

**Plugin #2**, id `com.botmaker.basics`: **the JDK value types and their editors, and nothing else.** Nothing
here runs in a bot. The SDK depends on it so a project can edit JDK values, and a project started from the
*Base* template depends on it directly so its Parameters window offers any type at all.

Thirteen types, in `com.botmaker.plugin.basics.values`: `String`, `boolean`, `int`, `double`, `char`,
`java.awt.Color`, `LocalDate`, `LocalTime`, `Duration`, the enums `DayOfWeek` and `Month`, `OffsetTime` and
its `ZoneOffset` (UTC written as `ZoneOffset.UTC` through `ComponentType.constants()`, other offsets as
`ZoneOffset.ofHoursMinutes`). They are nobody's vocabulary in particular; the SDK keeps the types that are
its own.

- **`BasicsPlugin`** is one contract declaration: `StudioPlugin.id(ID).named(NAME).types(() ->
  BasicsTypes.ALL)` on `DeclaredPlugin`. It names no toolkit class.
- **`BasicsTypes`** holds the declarations: each type a `PluginType.value(…)` whose factory is a method
  reference (`LocalDate::of`, `Color::new`) with its parts' accessors, the build derived by invoking it — a
  value out of range builds nothing and is shown as written.
- **`BasicsEditors`** draws them out of the toolkit's shapes (`Fields.committing`, `Fields.stepped`,
  `Modals.form`, `Pills`, `Slots.sourceOr`). `DurationPicker` (presets, spinners that carry, the length in
  words) draws `Duration`, and `TimeDial` (a 24h two-ring clock) draws `LocalTime` and `OffsetTime`.
- **The pickers' rules are pure and tested without a screen**: `PickRules`, `DurationParts`/`DurationText`,
  `ClockDial`/`TimeText`. Both views write only through `BasicsEditors.commit` — what was picked, and only
  when it differs from what was read — so opening a picker and pressing OK leaves the file byte-identical.

## The two rules that decide everything here

**A plugin, not a platform module.** Studio does not ship, resolve or depend on this artifact. That is why
the contract and JavaFX are `provided` (the host has one copy of each and `PluginLoader` is parent-first for
both) and the toolkit is `compile` (it travels onto the project's own classloader). Nothing is
`optional` — `optional` means *not transitive*, which is invisible in the module that has the bug and which
this project has shipped three times.

**Other plugins compile against this one, so it is not freely breakable.** The SDK declares it at
`compile` scope. A plugin's compiled `.class` files cannot be rewritten by anybody, so everything public here
owes **never-delete** and `@ReplacedBy`, exactly as `com.botmaker.sdk.api` does — and the trap that comes
with it is Maven's **nearest-wins** mediation: nothing may declare this module directly beside a plugin that
already brings it, or a bot resolves a version its plugin was never built against and fails with
`NoSuchMethodError` at whichever method moved.

## The types, and the two laws they keep

**The identity is the Java class, not an id.** A project's file says `java.time.Duration` because that is
what the field is declared as.

**A primitive is what is declared**, not its box: `int.class`, not `Integer.class`, because `int` is what a
bot's field is overwhelmingly declared as and a picker offering both would offer one type twice. A field
declared `Integer` resolves to the same declaration.

Two laws, both checked by `BasicsPluginTest` here and by `botmaker plugin validate` over any plugin:

- **`build(components(v))` equals `v`**, and the number of components matches the declared parts. A type
  whose build does not invert its components writes a user's file and reads it back as something else.
- **`fresh()` answers a real value of the declared type, without throwing.** A type that cannot say what a
  fresh one is cannot be offered in a picker.

**Two plugins may not *own* one type** — the host refuses it, because a project that opens differently
depending on which plugin loaded first is not a project. Offering an *editor* for somebody else's type is
not owning it, and the host asks the user which to use. **A type no loaded plugin declares is not an error
either**: the value keeps the expression its author wrote, renders read-only and is never rewritten.

## The SDK overrides one of these

The rule (the maintainer's, 2026-09-27): **basics draws the JDK types, the SDK only its own.** `Color` is the
one exception: it is declared here and drawn plainly here, and the SDK offers an eyedropper that samples a
frozen frame of the capture target — screen capture this plugin does not have. The host asks the user which
editor to use.

## Building and releasing

```bash
mvn verify                                        # here
mvn -pl botmaker-plugin-basics -am install        # from the umbrella root, with its upstreams
```

On `main` the contract and the toolkit resolve at their `main` `-SNAPSHOT` — what a local install of those
repositories produces. A tag's pom pins their released versions (the release commit writes them, umbrella
doc 43), and `flatten-maven-plugin` 1.4.1 bakes the pins' values into the *published* pom. Both plugin pins
(`flatten` 1.4.1, `maven-compiler-plugin` 3.11.0) are held where they are because **JitPack runs Maven
3.6.1** and refuses to execute a plugin whose own prerequisite exceeds it — the tag is then pushed,
permanent, and resolves to nothing. Three release chains have died that way.

Releases are cut from the umbrella only: `./release.sh --plugin-basics`. A `--studio-api` or a
`--plugin-toolkit` in the same run **forces** one here; a release here in turn **forces** `--sdk`.
