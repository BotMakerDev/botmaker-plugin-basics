# Changelog

All notable changes to `botmaker-plugin-basics` are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the versions are the git
tags the umbrella's `release.sh` cuts. Write under `## [Unreleased]`: the version number is not knowable
while the prose is being written — it is what the decide pass computes — and the release stamps it onto the
heading in that module's own release commit.

## [Unreleased]

### Added

- The module. `com.botmaker.basics`, generated from the shape `botmaker-plugin-archetype` produces:
  `BasicsPlugin` extending the toolkit's `AbstractStudioPlugin`, its `META-INF/services` declaration, and a
  test that a host can find it, construct it and get an empty but well-formed answer to each of its four
  questions.
- Its place in the platform: a reactor slot between `botmaker-plugin-toolkit` and `botmaker-sdk`, a
  `.deps.env` pinning the contract and the toolkit, a `jitpack.yml` that refuses to build without both, a
  `ci.yml` that resolves both from source, and a `--plugin-basics` flag in the umbrella's `release.sh` and
  in `com.botmaker.cli.release`.

- **The nine JDK value types**, moved out of `botmaker-sdk` — `TEXT`, `YES_NO`, `WHOLE_NUMBER`,
  `DECIMAL_NUMBER`, `CHARACTER`, `COLOR`, `DATE`, `TIME_OF_DAY`, `DURATION`. `BasicsValueTypes` registers
  them through the same `ValueCatalog.builder()` any plugin uses and `BasicsPlugin.buildValueTypes()`
  contributes them; `JdkText` is the grammar their stored text is read and written in. **The ids are the
  ones the SDK's enum constants had**, so no stored project changes meaning, and `botmaker-sdk`'s `WireText`
  delegates to `JdkText` rather than keeping a second copy of the grammar.

- **`com.botmaker.plugin.basics.store` — the project store, and how a bot reads its own parameters.**

  ```java
  Duration   wait   = Settings.load("wait", Duration.class);
  int        health = Settings.load("minHealth", int.class);
  List<Rect> zones  = Settings.loadAll("zones", Rect.class);
  boolean    on     = Settings.enabled("Mining");
  ```

  `PluginData` is the layout — a plugin's data is a folder of its own files,
  `plugins/<id prefix>/<last segment>/<name>.json` inside the project's resources, created on demand, with
  the name normalised so one file cannot become two under two spellings. `ProjectStore` is one of those
  files, read totally and written whole. `ProjectValues` reads one as untyped text, and
  `ProjectValues.forPlugin(id)` is how a bot reaches another plugin's — **resolving one classpath path and
  enumerating nothing**, which is what makes a folder tree readable from inside a jar. `ValueGrammar` is
  `Class<T> → parse/store/fallback`, found by `ServiceLoader`; `Settings` resolves one against the other.
  **Ship a `ValueGrammar` beside any value type you register**, and a bot reads your type by name exactly as
  it reads a `Duration`.

  Every read is total: an undeclared name, text that will not parse, a name declared as another type and a
  missing file all answer the type's own fallback. **One thing throws** — a type no grammar on the classpath
  claims, which is a packaging mistake rather than a bad file and has no value to fall back to. Two grammars
  claiming one type is refused by name rather than resolved by jar order. Writing throws too: a save that
  silently did not happen is the one failure a user cannot see.

  These arrived from `com.botmaker.plugin.toolkit.config`, where they had spent one day and never shipped
  (and from `botmaker-shared` the two days before that). A widget kit owns no value types, so it could hold
  the mechanism only by promising never to use it, and a plugin wanting to read one parameter had to resolve
  a widget kit and a JSON parser to do it. This module owns nine value types and ships `BasicsGrammar` for
  them, which is the arrangement every other plugin is offered.

- **`ParameterStore` — how any plugin declares parameters**, generalised out of `botmaker-sdk`, where it was
  `SdkParameters` and was plugin #1's last storage privilege. A plugin declares a `ParameterGroup`, holds a
  store over its own `PluginData`, and hands the host back what `rows(groupId)` answers; `apply(edit)` takes
  a changed value and answers the row **as stored**. Beside those two are the declaration verbs a parameters
  window performs — `declare`, `remove`, `rename`, `retype`, `setOptions`, `setBounds`, `setCategory`,
  `setVisibility`, `setDescription` — and the coercion that comes with being the editor: canonicalise
  through the owning type's codec, clamp to a declared `Range`, prune a value to the options still on offer,
  seed a fresh one with the type's default. A name is unique within a group and only there, so two plugins —
  and two groups of one plugin — may both offer a `timeout`, and a group only ever touches its own rows.

`JdkText`, `BasicsGrammar` and the rest of `store` name nothing but the JDK and Jackson, because they run in
a bot: this module reaches a bot's classpath through the SDK's `compile`-scope dependency on it, where the
contract and JavaFX — both `provided` — are absent. `BasicsIsBotSafeTest` scans the source and exempts only
`BasicsPlugin`, `BasicsValueTypes` and `ParameterStore`, so a new class is checked by default.

What is left for a later phase is giving the SDK plugin's activities and flow files of their own in the tree
rather than the top level of `activities.json` they still live at.
