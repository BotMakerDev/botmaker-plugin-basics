# botmaker-plugin-basics

The value types a BotMaker project's parameters are made of, the `Settings` a running bot reads them
through, and the project store both are written into.

It is a **plugin**, not a platform module: Studio does not ship it, does not depend on it and does not
resolve it. A project that wants it declares it, and `PluginLoader` puts it on that project's own
classloader beside every other plugin the project resolves.

```xml
<dependency>
    <groupId>com.github.LiQiyeDev</groupId>
    <artifactId>botmaker-plugin-basics</artifactId>
    <version>v0.0.1</version>
</dependency>
```

## What it owns

| | |
|---|---|
| The nine JDK value types | `TEXT`, `YES_NO`, `WHOLE_NUMBER`, `DECIMAL_NUMBER`, `CHARACTER`, `COLOR`, `DATE`, `TIME_OF_DAY`, `DURATION` — nobody's vocabulary in particular, and the SDK's until now only because the SDK was written first. |
| `Settings` | How a **running bot** reads its own parameters. |
| The project store | One file, sectioned by owning plugin id. Reading and writing it is this module's API, and other plugins use it for their own data — the SDK plugin stores its activities, flow and presets through it. |

None of that is here yet. This module currently loads and contributes nothing; the rest arrives phase by
phase, so each one changes a module that already works.

## Depending on it from another plugin

Ordinary Maven — not through the contract, and not through a service lookup. `PluginLoader` builds **one**
`URLClassLoader` over the whole resolved project classpath and runs one `ServiceLoader` pass in it, so
plugin A sees plugin B's classes and Maven has already mediated B to a single version.

Two things follow, and both are prices rather than surprises:

- **This module is not freely breakable.** Your compiled `.class` files cannot be rewritten by anybody, so
  everything public here owes never-delete and `@ReplacedBy`, exactly as `com.botmaker.sdk.api` does.
- **Do not declare it directly beside a plugin that already brings it.** Maven's nearest-wins mediation
  makes the direct entry win, and a bot then runs a version its plugin was never built against —
  a `NoSuchMethodError` at whichever method moved.

## Building

```bash
mvn verify
```

The contract and the toolkit resolve at `0.0.0-SNAPSHOT`, which is what a local `mvn install` of those
repositories (or the umbrella reactor) produces. From the umbrella root:

```bash
mvn -pl botmaker-plugin-basics -am install
```

## Releasing

Never from here. The umbrella's `./release.sh --plugin-basics` cuts the tag, writes `.deps.env` in the
release commit, and this repository's own CI publishes the GitHub Release from that tag. A contract or
toolkit release **forces** one here, because `flatten-maven-plugin` bakes those pins into the published
pom; a release here in turn forces the SDK, which depends on this module.
