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
3. **The project store** — `PluginData` and `ProjectStore`. A plugin's data is a **folder of its own files**,
   `plugins/<id prefix>/<last segment>/<name>.json` inside the project's resources, so `com.botmaker.sdk`
   writes under `plugins/com.botmaker/sdk/`: a folder per author and a folder per plugin, derived from the
   id alone with no new metadata. `ProjectStore` is one of those files, read totally and written whole;
   `PluginData` is the layout, the creation on demand and the name normalisation that stops `Activities` and
   `activities` becoming two files. **A bot enumerates nothing** — `PluginData.resource(id, name)` resolves
   one classpath path, which is what makes a tree readable from inside a jar.

   It was **one file sectioned by owning plugin id** until 2026-09-10, with a `withSection` that carried
   every other plugin's section through so an editor without a plugin installed could not save that
   plugin's data away. The maintainer withdrew it. Two of the single file's four defences survive and are
   why the tree is better rather than merely different: no code opens another plugin's file at all, so the
   carry-through becomes unnecessary rather than merely correct, and a merge conflict lands in one plugin's
   file instead of in one shared document. Two are lost and are stated plainly: the single atomic write goes
   — a crash mid-save can leave one plugin saved and another not — and a project is a directory to copy
   rather than a file. **There is no migration**: nothing reads the legacy `activities.json` as plugin data,
   so a project written before the tree reads as empty; nothing deletes it either, so a converter is
   writable later.

4. **`ParameterStore`** — how *any* plugin declares parameters, added 2026-09-10 and generalised out of the
   SDK, where it was `SdkParameters`. Rows in, rows out, and the coercion between: canonicalise through the
   owning type's codec, clamp to a declared `Range`, prune a value to the options still on offer, seed a
   fresh one with the type's default. A plugin declares a `ParameterGroup`, holds one of these over its own
   `PluginData`, and hands the host back what `rows(groupId)` answers. It names the contract, so it is
   editor-only and exempted in `BasicsIsBotSafeTest`; the same file is read by a bot through
   `ProjectValues.forPlugin(id)`, as untyped text. Giving the SDK plugin's activities and flow their own
   files is a later phase.

5. **`@Param` and `PluginStore`**, added 2026-09-17, and together they are the split that matters now.
   `com.botmaker.plugin.basics.params.Param` is how a **user parameter** is declared: a `public static`
   field in the *bot's own Java*, which Studio reads off the syntax tree and whose initializer the value
   cell rewrites. `PluginStore` (and `Settings.forPlugin` on the bot side) is how a **plugin's own state**
   is stored: a record in, a record out, over `PluginData`'s tree.

   **The rule to hold on to: a user parameter is Java; a plugin's state is JSON.** `Settings.load(name,
   Class)` and `ParameterStore` still exist, still work and are never deleted, and what they carry is a
   plugin's rows — an activity's enable flag, a plugin's own settings. They stopped being how a bot reads
   *its* parameters because a name is a string on both sides: a typo compiled and answered the type's
   fallback, and the declaration lived where the bot's author could not see it.

   `Param` is bot-safe like the rest, which is why its members are strings (`visibility`, `min`, `max`,
   `options`): the contract's `Visibility` and the value types are off a bot's classpath, and the value's
   own grammar parses the text exactly as it parses what a user types into the cell. It declares
   `Param.EDITOR`/`Param.PUBLIC` so neither Studio nor a bot spells those two strings itself.

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
