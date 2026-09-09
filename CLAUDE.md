# CLAUDE.md — botmaker-plugin-basics

Guidance for Claude Code working in this module. The umbrella's `CLAUDE.md` is the map of how the eleven
repositories fit together; this file is what is true *here*.

## What this module is

**Plugin #2**, and the first plugin in the project that is not the SDK. Its id is `com.botmaker.basics`.

It owns three things, all three landed on 2026-09-09.

1. **The nine JDK value types** — `TEXT`, `YES_NO`, `WHOLE_NUMBER`, `DECIMAL_NUMBER`, `CHARACTER`,
   `COLOR`, `DATE`, `TIME_OF_DAY`, `DURATION`, in `com.botmaker.plugin.basics.values`. They are nobody's
   vocabulary in particular, and they were the SDK's only because the SDK was written first. The SDK keeps
   its own eight (`ImageTemplate`, `Precision`, `Point`, `Rect`, `Size`, `Direction`, `Key`,
   `MouseButton`), which genuinely are its. **Two classes, and the split is the bot-safety rule**:
   `JdkText` is the grammar — what a stored value's text means — and names nothing but the JDK, because a
   running bot calls it (`com.botmaker.sdk.authoring.WireText` delegates to it, so there is one grammar
   rather than two); `BasicsValueTypes` is the registration, the labels and the Java literals, and names the
   contract and the toolkit, both of which a bot does not have.
2. **`Settings` and `ValueGrammar`**, in `com.botmaker.plugin.basics.store` — how a *running bot* reads its
   own parameters, plus `BasicsGrammar`, this module's own nine readers. They came from
   `com.botmaker.plugin.toolkit.config`, where they landed earlier the same day and never shipped, and from
   `botmaker-shared` for the two days before that. Each move was right about the one before it; what
   settled it is that **a widget kit owns no value types**, so it could hold the mechanism only under a
   promise never to use it, while this module owns nine and ships a grammar for them like any plugin.
3. **The project store** — `ProjectStore`, one file **sectioned by owning plugin id**. `section(id)` is a
   plugin's own data; `withSection` writes one section and carries every other one through, so an editor
   without a plugin installed cannot save that plugin's data away. A file with no `plugins` object is a
   legacy file and answers its root for every id, which is what every project written so far is — the
   fallback names no plugin id, deliberately, because an id in the mechanism is the mechanism knowing its
   first two customers. Giving the SDK plugin's activities, flow and presets their own section is a later
   phase.

## The three rules that decide everything here

**A plugin, not a platform module.** Studio does not ship, resolve or depend on this artifact. That is why
the contract and JavaFX are `provided` (the host has one copy of each and `PluginLoader` is parent-first for
both) and everything else is `compile` (it travels onto the project's own classloader). Nothing is
`optional` — `optional` means *not transitive*, which is invisible in the module that has the bug and which
this project has shipped three times.

**Other plugins compile against this one, so it is not freely breakable.** The SDK declares it at
`compile` scope since 2026-09-09. A plugin's compiled `.class` files cannot be rewritten by anybody, so everything public
here owes **never-delete** and `@ReplacedBy`, exactly as `com.botmaker.sdk.api` does — and the trap that
comes with it is Maven's **nearest-wins** mediation: nothing may declare this module directly beside a
plugin that already brings it, or a bot resolves a version its plugin was never built against and fails
with `NoSuchMethodError` at whichever method moved. That is precisely the landmine
`MavenService.TOOLKIT_FALLBACK_VERSION` was, and why that constant is deleted.

**Most of this module runs in a bot, and that part may name neither the contract nor JavaFX.** A bot's
classpath has no contract on it and no scene graph in it. `BasicsIsBotSafeTest` scans the **source** —
rather than the classpath, where a `provided` dependency is present in the module that declares it, so the
check would pass for a jar that cannot load anywhere else — and it works by **exemption**: everything is
checked, and only `BasicsPlugin` and `BasicsValueTypes` are named as editor-side. That is the inverse of the
rule it replaced (`ToolkitConfigIsBotSafeTest` named the one package that had to be safe), and the direction
matters: a class added tomorrow is checked by default, so the mistake is a red test here rather than a
`NoClassDefFoundError` in a stranger's bot.

## Value type ids

**The id is the identity and it never changes.** It is what a project's stored data holds, and it is what
the plugin registry refuses to admit twice. The nine kept the ids the SDK's enum constants had (`TEXT`,
`YES_NO`, …) when they moved, so every project ever written keeps its meaning — the move is invisible in a
project file, which `BasicsValueTypesTest` asserts by writing the nine ids out rather than deriving them.

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
