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
2. **`Settings.forPlugin`**, in `com.botmaker.plugin.basics.store` — how a *running bot* reads a plugin's
   own file, one plugin id plus one document name to one path.

   **The reading half and the whole grammar layer went on 2026-09-21.** `Settings.load`/`loadAll`/`enabled`/
   `declares`/`use`, `ValueGrammar`, this module's `BasicsGrammar` and the `ServiceLoader` that found them
   are deleted. They existed to answer *what does this stored text mean* for a value a bot read out of
   JSON, and a bot reads no such value any more: a user parameter is a `@Param` field in the bot's own Java
   (2026-09-17) and a plugin's values are Java the plugin ships (2026-09-21). `Settings.enabled` in
   particular had to go rather than be deprecated — it read `activities.json`, so with that file gone it
   could only ever have answered `false`, which is a method that compiles, runs, and switches every
   activity off. **Deleting beats deprecating whenever a kept method has no data source left**: the method
   still answers, and what it answers is a fallback.

   `JdkText` stays and is still the grammar for this module's nine types — it is what parses the text a
   user types into a cell, which is a live question. What is gone is the registry that let a *bot* look one
   up by name.
3. **The project store** — `PluginData`, and since 2026-09-22 it is the whole of it. A plugin's data is a
   **folder of its own files**, `plugins/<id prefix>/<last segment>/<name>.json` inside the project's
   resources, so `com.botmaker.sdk` writes under `plugins/com.botmaker/sdk/`: a folder per author and a
   folder per plugin, derived from the id alone with no new metadata. `PluginData` is the layout, the
   creation on demand, the name normalisation that stops `Activities` and `activities` becoming two files,
   and the four operations over one file: `read` (a `JsonNode` from a directory), `write`, `load` (the same
   document off a bot's classpath) and `convert` (a document as a record). **A bot enumerates nothing** —
   `PluginData.resource(id, name)` resolves one classpath path, which is what makes a tree readable from
   inside a jar.

   It was **three classes until 2026-09-22**: `ProjectStore` held one document and did the I/O,
   `PluginStore` wrapped it for typed reads and writes, and this named the file. That layering was built
   for a store with several customers and ended with one and a half — `FlowLayout`'s card positions, and
   `Settings.forPlugin` on the bot side. Everything else moved into the bot's own Java: a parameter is a
   `@Param` field, a plugin's value is a `@Managed` method, the flow is a `Flow`, and the capture source
   went the same way in the change that collapsed these. **Reading is total and writing throws** — those
   answers are `ProjectStore`'s and `PluginStore`'s, kept verbatim rather than re-decided.

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

4. **`ParameterStore` stood here from 2026-09-10 to 2026-09-22 and is deleted, with `StoredForms` and
   `PluginData.PARAMETERS`.** It was how *any* plugin was to declare parameters — rows in, rows out, and the
   coercion between: canonicalise through the owning type's codec, clamp to a declared `Range`, prune a value
   to the options still on offer, seed a fresh one with the type's default. 582 lines, generalised out of the
   SDK where it was `SdkParameters`, and defended at length the week it landed.

   **Nothing ever called `declare`.** Not this module, not the SDK, not Studio. So the only rows it ever
   returned were whatever a project written before 2026-09-17 already had in
   `plugins/<prefix>/<segment>/parameters.json`, and nothing wrote that file again. *A converter is a second
   reader of a format nothing writes* — the umbrella `CLAUDE.md` forbids it by name, and this was one. It is
   the same failure shape as `ValueCodec.wireOfLiteral`, which this file already records: **the half nobody is
   forced to write is the half that rots.** Here the rotted half was the writing half, so the reading half had
   nothing to read.

   **What replaced it had already replaced it.** A user parameter became a `@Param` field in the bot's own
   Java on 2026-09-17, read and written off the syntax tree. The case this store was kept for — *a plugin's
   own row, an activity's enable flag, a capture target* — has the better answer on the same terms: **the
   plugin puts a `@Param` field in the file it ships**, and the host's ordinary walk of the bot's sources
   finds it. One file format, one editor, and a row the bot's author can read.

   The contract half went with it (`ParameterGroup`, `ParameterEdit`, `StudioPlugin.parameters(String)`,
   `parameterRows(String)`, `parameterEdited(…)`); `ParameterRow` stays as the shape one row crosses in.
   `ValueCatalog.initializerOfWires`, `wiresOfInitializer`, `defaultItem` and `normalize` lost their nine
   call sites here, which is the porting phase that was owed and is now moot.

5. **`@Managed`**, in `com.botmaker.plugin.basics.managed`, added 2026-09-21. One member, the plugin-local
   id; `@Target({TYPE, METHOD})`. On a `public static` method it says *the host owns the expression this
   returns* — one fixed value the plugin shipped, rewritten in place and never added to or deleted. On a
   type it says *the host owns the members* — an open set the user grows, which is what 🖼 Manage Pictures
   does to `Pictures`.

   **It is here and not in the contract for the same reason `@Param` is**: a bot has no contract jar
   (`provided`), and a bot's own source is where these land. It replaced `ManagedField` and
   `StudioPlugin.managedFields()`, which matched on a *declared type* and guessed that a class of nothing
   but managed constants was managed whole — two inferences from shape, where this is a statement.

6. **`@Param` and `Settings.forPlugin`**, and together they are the split that matters now.
   `com.botmaker.plugin.basics.params.Param` (2026-09-17) is how a **user parameter** is declared: a
   `public static` field in the *bot's own Java*, which Studio reads off the syntax tree and whose
   initializer the value cell rewrites. `Settings.forPlugin` — over `PluginData.load`/`convert` — is how a
   bot reads a **plugin's own state**: a record out, off one resolved classpath path. The editor's side of
   the same file is `PluginData.read`/`write`. `PluginStore` was the typed wrapper over both halves until
   2026-09-22; it folded into `PluginData` when its last customer moved into Java.

   **The rule to hold on to, as it now stands: a user parameter is Java, a plugin's values are Java, and
   what is left in JSON is what a bot does not read.** It was *"a user parameter is Java; a plugin's state
   is JSON"* until 2026-09-21, and the second half moved: an activity's enable flag is part of
   `com.botmaker.sdk.api.flow.Flow`, in the bot's own source, because it is part of what the bot does.
   Capture targets and the flow editor's card positions stay JSON, because a bot reads neither.

   The reason is the same one both times: a name is a string on both sides, so a typo compiled and answered
   the type's fallback, and the declaration lived where the bot's author could not see it. `@Managed` is
   that argument applied to the plugin's own values — see `Managed`'s javadoc and
   `docs/refactor/33-plugin-java.md`.

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
