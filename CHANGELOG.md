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

`JdkText` names nothing but the JDK, because it runs in a bot: this module reaches a bot's classpath
through the SDK's `compile`-scope dependency on it, where the contract and JavaFX — both `provided` — are
absent. `BasicsValueTypes` names both and is editor-side.

What it will own next — `Settings`, the grammar a bot reads its parameters through, and the project store —
arrives in the phases after this one.
