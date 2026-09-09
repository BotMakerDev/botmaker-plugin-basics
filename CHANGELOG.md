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

  `ProjectStore` is the file itself, **sectioned by owning plugin id**: `section(id)` is a plugin's own data
  and `withSection` carries every other plugin's through untouched, so an editor without a plugin installed
  cannot save that plugin's data away. A file with no `plugins` object is a legacy file and answers its root
  for every id, which is what every project written so far is. `ProjectValues` reads one section as untyped
  text; `ValueGrammar` is `Class<T> → parse/store/fallback`, found by `ServiceLoader`; `Settings` resolves
  one against the other. **Ship a `ValueGrammar` beside any value type you register**, and a bot reads your
  type by name exactly as it reads a `Duration`.

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

`JdkText`, `BasicsGrammar` and everything under `store` name nothing but the JDK and Jackson, because they
run in a bot: this module reaches a bot's classpath through the SDK's `compile`-scope dependency on it,
where the contract and JavaFX — both `provided` — are absent. `BasicsIsBotSafeTest` scans the source and
exempts only `BasicsPlugin` and `BasicsValueTypes`, so a new class is checked by default.

What is left for a later phase is giving the SDK plugin's activities, flow and presets their own section of
the store rather than the unsectioned top level they still live at.
