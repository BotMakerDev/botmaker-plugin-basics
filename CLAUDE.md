# CLAUDE.md — botmaker-plugin-basics

Guidance for Claude Code working in this module. The umbrella's `CLAUDE.md` is the map of how the eleven
repositories fit together; this file is what is true *here*.

## What this module is

**Plugin #2**, and the first plugin in the project that is not the SDK. Its id is `com.botmaker.basics`.

It owns three things, none of which is in it yet — the module currently loads and contributes nothing,
which is deliberate: the platform and the feature should not be proved in the same commit.

1. **The nine JDK value types** — `TEXT`, `YES_NO`, `WHOLE_NUMBER`, `DECIMAL_NUMBER`, `CHARACTER`,
   `COLOR`, `DATE`, `TIME_OF_DAY`, `DURATION`. They are nobody's vocabulary in particular, and they are the
   SDK's today only because the SDK was written first. The SDK keeps its own eight (`ImageTemplate`,
   `Precision`, `Point`, `Rect`, `Size`, `Direction`, `Key`, `MouseButton`), which genuinely are its.
2. **`Settings` and `ValueGrammar`** — how a *running bot* reads its own parameters. They arrive here from
   `com.botmaker.plugin.toolkit.config`, where they landed on 2026-09-09 and lasted a week.
3. **The project store** — one file, sectioned by owning plugin id. Reading and writing it is this module's
   API and is used by other plugins for their own data; the SDK plugin stores its activities, flow and
   presets through it rather than beside it.

## The three rules that decide everything here

**A plugin, not a platform module.** Studio does not ship, resolve or depend on this artifact. That is why
the contract and JavaFX are `provided` (the host has one copy of each and `PluginLoader` is parent-first for
both) and everything else is `compile` (it travels onto the project's own classloader). Nothing is
`optional` — `optional` means *not transitive*, which is invisible in the module that has the bug and which
this project has shipped three times.

**Other plugins compile against this one, so it is not freely breakable.** The SDK will declare it at
`compile` scope. A plugin's compiled `.class` files cannot be rewritten by anybody, so everything public
here owes **never-delete** and `@ReplacedBy`, exactly as `com.botmaker.sdk.api` does — and the trap that
comes with it is Maven's **nearest-wins** mediation: nothing may declare this module directly beside a
plugin that already brings it, or a bot resolves a version its plugin was never built against and fails
with `NoSuchMethodError` at whichever method moved. That is precisely the landmine
`MavenService.TOOLKIT_FALLBACK_VERSION` was, and why that constant is deleted.

**Half of this module runs in a bot, and that half may name neither the contract nor JavaFX.** A bot's
classpath has no contract on it and no scene graph in it. `Settings` and the grammar are read by the bot
itself; the plugin half (the catalog, the editors, the parameters) is read by an editor. When the store
lands, the source scan that holds the line comes with it — a *source* scan rather than a classpath one,
because a `provided` dependency is present in the module that declares it and so a classpath check passes
for a jar that cannot load anywhere else.

## Value type ids

**The id is the identity and it never changes.** It is what a project's stored data holds, and it is what
the plugin registry refuses to admit twice. The nine keep the ids the SDK's enum constants had (`TEXT`,
`YES_NO`, …) when they move, so every project ever written keeps its meaning — the move must be invisible
in a project file.

Two rules from the contract apply to every type registered here. **Two types may not claim one Java type**:
`ValueCatalog.Builder.add` throws when one author contradicts themselves, and `javaClashesWith` *reports*
when two plugins disagree, first registration winning and the loser keeping its id so stored values still
parse. And **an unregistered id is not an error**: a project opened without this plugin keeps its values as
raw text, renders them read-only and declines to emit them.

## Building and releasing

```bash
mvn verify                                        # here
mvn -pl botmaker-plugin-basics -am install        # from the umbrella root, with its upstreams
```

The contract and the toolkit resolve at `0.0.0-SNAPSHOT` — what a local install of those repositories
produces. `jitpack.yml` injects the released tags from `.deps.env` at build time, and `flatten-maven-plugin`
1.4.1 is what makes an injected pin reach the *published* pom rather than only that build. Both plugin pins
(`flatten` 1.4.1, `maven-compiler-plugin` 3.11.0) are held where they are because **JitPack runs Maven
3.6.1** and refuses to execute a plugin whose own prerequisite exceeds it — the tag is then pushed,
permanent, and resolves to nothing. Three release chains have died that way.

Releases are cut from the umbrella only: `./release.sh --plugin-basics`. A `--studio-api` or a
`--plugin-toolkit` in the same run **forces** one here; a release here in turn **forces** `--sdk`.
