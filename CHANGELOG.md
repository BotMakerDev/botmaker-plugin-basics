# Changelog

All notable changes to `botmaker-plugin-basics` are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the versions are the git
tags the umbrella's `release.sh` cuts. Write under `## [Unreleased]`: the version number is not knowable
while the prose is being written — it is what the decide pass computes — and the release stamps it onto the
heading in that module's own release commit.

## [Unreleased]

### Added

- **`managed/ManagedValues`** — hands a bot's `@Managed` values to the plugins that own them, so no bot
  writes an `install()`. `claim(id, sink)` is a plugin's library half saying it takes an id; `install(Class…)`
  invokes every `public static` no-argument `@Managed` method on each class named and dispatches what it
  returns. It sits beside `@Managed` because that is where the annotation lives and both are on every bot's
  classpath.
  An unclaimed id, a method that throws and a values class that cannot be read are each one line on
  `System.err` and never a throw — the first is the ordinary state of a bot whose pom no longer names that
  plugin. `@Managed` on a *type* installs nothing: it marks constants the bot names at its use sites.
  **Claim ordering needs no registry file**: `install` invokes a method before dispatching its value, and
  invoking links the declared return type, so a plugin claiming from that type's static initialiser is
  always registered in time.

### Removed

- **`store/ParameterStore` (582 lines) and `store/StoredForms`**, with `PluginData.PARAMETERS`. A plugin was
  to declare its rows through `ParameterStore.declare` and the host was to read them back through the
  contract. **`declare` had no caller anywhere** — not in this module, not in the SDK, not in Studio — so the
  only rows the store ever returned were whatever sat in a project written before 2026-09-17, and the file it
  kept them in was never written again. A second reader of a format nothing writes is what the umbrella
  `CLAUDE.md` forbids by name; it is the same failure shape as `ValueCodec.wireOfLiteral`, and here the half
  nobody wrote was the writing half.
  A parameter is a `@Param` static field in the bot's own Java, read and written off the syntax tree.
  **A plugin that wants a row of its own puts a `@Param` field in the file it ships** — the host's walk of
  the bot's sources finds it with no store, no file format and no contract surface.
  `Settings` and `ValueGrammar` are untouched: a plugin's own flags are not rows, and a running bot still
  reads them.

### Changed

- **Recompiled against the contract's new packages** — imports only, no behaviour change. See
  `botmaker-studio-api`'s changelog for the old → new table.

## [0.0.7] — 2026-09-21

### Changed

- **Recompiled against the contract's new packages** — imports only, no behaviour change. See
  `botmaker-studio-api`'s changelog for the old → new table.

## [0.0.6] — 2026-09-21

### Removed

- **`Settings.load`, `loadAll`, `enabled`, `declares` and `use`, with `ValueGrammar`, `BasicsGrammar` and
  the `ServiceLoader` that found them.** They read a bot's parameters and its activity enable flags out of
  `activities.json`. Neither lives there any more: a user parameter is a `@Param` field in the bot's own
  Java and an activity's enable flag is part of the `Flow` value the bot installs, so both are typed by
  javac and neither can be a name that silently matches nothing. Deleted rather than deprecated, because a
  kept method whose only data source has been removed answers a fallback on every call — which is a bot
  that runs on defaults and never says so, and is strictly worse than a compile error naming the line.

- **`ProjectValues` whole**, and `ProjectStore.RESOURCE`, `FILE`, `current()` and `use(…)` with it. They
  were the reader of that file and the one place `ProjectStore` knew a file name.

### Kept

- **`Settings.forPlugin(id)`** — a bot reading a plugin's own stored state, resolved from the id and the
  name off its classpath. Unchanged, and now the whole of what `Settings` is.

- **`PluginData`'s paths and `ProjectStore`'s I/O.** A plugin's own JSON under
  `plugins/<id prefix>/<last segment>/` is untouched by any of this.

No source changes since v0.0.4; re-released for updated upstream pins.

No source changes since v0.0.3; re-released for updated upstream pins.

No source changes since v0.0.2; re-released for updated upstream pins.

### Added

- **`@Managed("id")`**, in the new `com.botmaker.plugin.basics.managed` package, beside `@Param` and for the
  same reason: a bot has no contract jar, so the marker a plugin's values are found by has to live where a
  bot can see it. On a **method** it says the expression that method returns is a value the plugin's own
  window edits — BotMaker rewrites that one expression and nothing else, so the file's comments, helpers and
  formatting survive a save. On a **class** it says the whole class is the plugin's, which is the shape for
  a set the user grows (one constant per captured picture). A body that is not exactly one
  `return <expression>;` is shown read-only with the reason and never rewritten.

  This is where `plugins/<id prefix>/<last segment>/<name>.json` goes. A name renamed in Java is a compile
  error; the same rename against JSON was a silently empty value three screens into a run. `Settings` stays
  for a plugin's flags and its own files.

### Changed

- **`ParameterStore`'s verbs take a `ValueForm`.** `declare`, `retype`, `normalize`, `normalizeOptions` and
  `defaultValue` took a `ValueChoice`, which the contract deleted on 2026-09-20. Two rules read differently
  as a result and both read better: declared options now survive a change of *container* over one leaf type
  rather than a change of shape, which is the same rule said in terms of what the options are values of; and
  whether a set is declared is no longer asked of the type at all — a row with options has them, a row
  without does not, and no type ever knew which. **The file format is unchanged**, including the `shape` and
  `list` fields, which are still written so that a Studio built before this release opens a project saved by
  one built after it.

- **`StoredForms`** — the one place that knows how a type is spelled in a JSON file written before
  `ValueForm`: `{"type":"DURATION","shape":"ANY_OF","list":true}` read back to a form, and written out
  again. Total, with the totality the deleted `ValueShape.fromWire` carried — an id nothing registers is an
  unknown type and a shape a newer writer invented is one free value. It is here rather than in each reader
  because both readers of that spelling are files, this plugin's `parameters.json` and the SDK plugin's
  `activities.json`, and the SDK depends on this module: one decoder, on the side that owns the vocabulary
  the files are written in. Editor-side, and exempted in `BasicsIsBotSafeTest` for the same reason
  `ParameterStore` is. It goes when the files do.

- **`ParameterStore` keeps the stored form beside each row.** A `ParameterRow`'s value is the Java
  initialiser it is written from now, and `parameters.json` still holds the stored items the coercion rules
  — canonicalise, clamp, prune — are written over. The two are joined at this store's boundary and nowhere
  else. The wires are kept on the entry rather than derived back out of the row, because every read decodes
  **every** group's rows to write the siblings back untouched, and a row of a type this plugin's catalog
  cannot read has no initialiser at all: deriving it would empty another window's value the first time this
  one saved. The file format is unchanged.

- **The nine codecs are built with `Codecs.of` and read text through `Source`.** Each answers a *value*
  rather than a stored string, following the contract's `ValueCodec.valueOfLiteral`, which replaces
  `wireOfLiteral`; the string and character inverses are the toolkit's `Source.stringValue`/
  `characterValue` — the same file as the escaping they undo, rather than a second copy of it here. One
  thing changes behaviour: a pasted control character is written as `\u0007` and now reads back, where the
  private reader this replaces refused that escape, so exactly the values nobody can see were the ones
  written and then shown read-only.

### Added

- **`@Param`** (`com.botmaker.plugin.basics.params.Param`) — a user parameter is a field in the bot's own
  Java now, not a row in a JSON file read back by name:

  ```java
  @Param(category = "Limits", min = "1", max = "50")
  public static int maxAttempts = 10;
  ```

  The bot reads `Parameters.maxAttempts`, so a misspelling is a compile error and the type is the type.
  Studio reads the same fields off the syntax tree and draws the Parameters window from them. The members
  are `category` (free text — the six SDK categories were a vocabulary), `description`, `visibility`
  (`Param.EDITOR` or `Param.PUBLIC`, strings because the contract's `Visibility` is off a bot's classpath),
  `min`, `max` and `options`.
- **`PluginStore`** — a plugin's own state as the plugin's own records, one line each way:
  `store.write("capture", targets)` and `store.read("capture", Targets.class)`, over `PluginData`'s file
  tree with Jackson. Reading is total (absent, unparseable, wrongly shaped and `{}` all read as empty);
  writing throws, because a save that silently did not happen is the one failure a user cannot see. A field
  the record no longer declares is ignored, so a plugin can still read what an older version of itself
  wrote.
- **`Settings.forPlugin(id)`** — the same files from inside a running bot, read by one resolved classpath
  path. `read(name, Class)` and `readAll(name, Class)`, same rules.
- **The nine types read their own Java back** (`ValueCodec.wireOfLiteral`, new in the contract). A
  `@Param` field's value *is* its initialiser, so a type that can write `java.time.Duration.ofMillis(3000L)`
  and not read it would be one the editor writes and then refuses to edit. Each inverse is written in the
  same expression as the literal it undoes, and recognises **only** what this plugin emits:
  `Duration.ofSeconds(3)` means the same thing and is declined, because the author wrote that on purpose and
  the window shows it as written rather than rewriting it on open.

### Changed

- **`ParameterStore.declared(ParameterDeclaration)` is gone**, with the contract method it implemented
  (studio-api, 2026-09-17). The nine verbs it reconciled through — `declare`, `remove`, `rename`, `retype`,
  `setOptions`, `setBounds`, `setCategory`, `setVisibility`, `setDescription` — are unchanged and still
  public: a plugin declares its own rows by calling them, which is what they were the implementation of all
  along. What is gone is the wire form for a *host* asking, because a user parameter is a `@Param` field in
  the bot's own Java now and the host edits it there.
- **`Settings.load`/`loadAll`/`declares` are for a *plugin's* rows now** — activity enable flags and
  whatever a plugin declares for itself. They are unchanged and never deleted (a bot compiled against them
  cannot be rewritten); what changed is that a *user* parameter is no longer one of them. The javadoc says
  so at the top of the class.

## [0.0.5] — 2026-09-19

No source changes since v0.0.4; re-released for updated upstream pins.

No source changes since v0.0.3; re-released for updated upstream pins.

No source changes since v0.0.2; re-released for updated upstream pins.

### Added

- **`@Param`** (`com.botmaker.plugin.basics.params.Param`) — a user parameter is a field in the bot's own
  Java now, not a row in a JSON file read back by name:

  ```java
  @Param(category = "Limits", min = "1", max = "50")
  public static int maxAttempts = 10;
  ```

  The bot reads `Parameters.maxAttempts`, so a misspelling is a compile error and the type is the type.
  Studio reads the same fields off the syntax tree and draws the Parameters window from them. The members
  are `category` (free text — the six SDK categories were a vocabulary), `description`, `visibility`
  (`Param.EDITOR` or `Param.PUBLIC`, strings because the contract's `Visibility` is off a bot's classpath),
  `min`, `max` and `options`.
- **`PluginStore`** — a plugin's own state as the plugin's own records, one line each way:
  `store.write("capture", targets)` and `store.read("capture", Targets.class)`, over `PluginData`'s file
  tree with Jackson. Reading is total (absent, unparseable, wrongly shaped and `{}` all read as empty);
  writing throws, because a save that silently did not happen is the one failure a user cannot see. A field
  the record no longer declares is ignored, so a plugin can still read what an older version of itself
  wrote.
- **`Settings.forPlugin(id)`** — the same files from inside a running bot, read by one resolved classpath
  path. `read(name, Class)` and `readAll(name, Class)`, same rules.
- **The nine types read their own Java back** (`ValueCodec.wireOfLiteral`, new in the contract). A
  `@Param` field's value *is* its initialiser, so a type that can write `java.time.Duration.ofMillis(3000L)`
  and not read it would be one the editor writes and then refuses to edit. Each inverse is written in the
  same expression as the literal it undoes, and recognises **only** what this plugin emits:
  `Duration.ofSeconds(3)` means the same thing and is declined, because the author wrote that on purpose and
  the window shows it as written rather than rewriting it on open.

### Changed

- **`ParameterStore.declared(ParameterDeclaration)` is gone**, with the contract method it implemented
  (studio-api, 2026-09-17). The nine verbs it reconciled through — `declare`, `remove`, `rename`, `retype`,
  `setOptions`, `setBounds`, `setCategory`, `setVisibility`, `setDescription` — are unchanged and still
  public: a plugin declares its own rows by calling them, which is what they were the implementation of all
  along. What is gone is the wire form for a *host* asking, because a user parameter is a `@Param` field in
  the bot's own Java now and the host edits it there.
- **`Settings.load`/`loadAll`/`declares` are for a *plugin's* rows now** — activity enable flags and
  whatever a plugin declares for itself. They are unchanged and never deleted (a bot compiled against them
  cannot be rewritten); what changed is that a *user* parameter is no longer one of them. The javadoc says
  so at the top of the class.

## [0.0.4] — 2026-09-19

No source changes since v0.0.3; re-released for updated upstream pins.

No source changes since v0.0.2; re-released for updated upstream pins.

### Added

- **`@Param`** (`com.botmaker.plugin.basics.params.Param`) — a user parameter is a field in the bot's own
  Java now, not a row in a JSON file read back by name:

  ```java
  @Param(category = "Limits", min = "1", max = "50")
  public static int maxAttempts = 10;
  ```

  The bot reads `Parameters.maxAttempts`, so a misspelling is a compile error and the type is the type.
  Studio reads the same fields off the syntax tree and draws the Parameters window from them. The members
  are `category` (free text — the six SDK categories were a vocabulary), `description`, `visibility`
  (`Param.EDITOR` or `Param.PUBLIC`, strings because the contract's `Visibility` is off a bot's classpath),
  `min`, `max` and `options`.
- **`PluginStore`** — a plugin's own state as the plugin's own records, one line each way:
  `store.write("capture", targets)` and `store.read("capture", Targets.class)`, over `PluginData`'s file
  tree with Jackson. Reading is total (absent, unparseable, wrongly shaped and `{}` all read as empty);
  writing throws, because a save that silently did not happen is the one failure a user cannot see. A field
  the record no longer declares is ignored, so a plugin can still read what an older version of itself
  wrote.
- **`Settings.forPlugin(id)`** — the same files from inside a running bot, read by one resolved classpath
  path. `read(name, Class)` and `readAll(name, Class)`, same rules.
- **The nine types read their own Java back** (`ValueCodec.wireOfLiteral`, new in the contract). A
  `@Param` field's value *is* its initialiser, so a type that can write `java.time.Duration.ofMillis(3000L)`
  and not read it would be one the editor writes and then refuses to edit. Each inverse is written in the
  same expression as the literal it undoes, and recognises **only** what this plugin emits:
  `Duration.ofSeconds(3)` means the same thing and is declined, because the author wrote that on purpose and
  the window shows it as written rather than rewriting it on open.

### Changed

- **`ParameterStore.declared(ParameterDeclaration)` is gone**, with the contract method it implemented
  (studio-api, 2026-09-17). The nine verbs it reconciled through — `declare`, `remove`, `rename`, `retype`,
  `setOptions`, `setBounds`, `setCategory`, `setVisibility`, `setDescription` — are unchanged and still
  public: a plugin declares its own rows by calling them, which is what they were the implementation of all
  along. What is gone is the wire form for a *host* asking, because a user parameter is a `@Param` field in
  the bot's own Java now and the host edits it there.
- **`Settings.load`/`loadAll`/`declares` are for a *plugin's* rows now** — activity enable flags and
  whatever a plugin declares for itself. They are unchanged and never deleted (a bot compiled against them
  cannot be rewritten); what changed is that a *user* parameter is no longer one of them. The javadoc says
  so at the top of the class.

## [0.0.3] — 2026-09-18

No source changes since v0.0.2; re-released for updated upstream pins.

### Added

- **`@Param`** (`com.botmaker.plugin.basics.params.Param`) — a user parameter is a field in the bot's own
  Java now, not a row in a JSON file read back by name:

  ```java
  @Param(category = "Limits", min = "1", max = "50")
  public static int maxAttempts = 10;
  ```

  The bot reads `Parameters.maxAttempts`, so a misspelling is a compile error and the type is the type.
  Studio reads the same fields off the syntax tree and draws the Parameters window from them. The members
  are `category` (free text — the six SDK categories were a vocabulary), `description`, `visibility`
  (`Param.EDITOR` or `Param.PUBLIC`, strings because the contract's `Visibility` is off a bot's classpath),
  `min`, `max` and `options`.
- **`PluginStore`** — a plugin's own state as the plugin's own records, one line each way:
  `store.write("capture", targets)` and `store.read("capture", Targets.class)`, over `PluginData`'s file
  tree with Jackson. Reading is total (absent, unparseable, wrongly shaped and `{}` all read as empty);
  writing throws, because a save that silently did not happen is the one failure a user cannot see. A field
  the record no longer declares is ignored, so a plugin can still read what an older version of itself
  wrote.
- **`Settings.forPlugin(id)`** — the same files from inside a running bot, read by one resolved classpath
  path. `read(name, Class)` and `readAll(name, Class)`, same rules.
- **The nine types read their own Java back** (`ValueCodec.wireOfLiteral`, new in the contract). A
  `@Param` field's value *is* its initialiser, so a type that can write `java.time.Duration.ofMillis(3000L)`
  and not read it would be one the editor writes and then refuses to edit. Each inverse is written in the
  same expression as the literal it undoes, and recognises **only** what this plugin emits:
  `Duration.ofSeconds(3)` means the same thing and is declined, because the author wrote that on purpose and
  the window shows it as written rather than rewriting it on open.

### Changed

- **`ParameterStore.declared(ParameterDeclaration)` is gone**, with the contract method it implemented
  (studio-api, 2026-09-17). The nine verbs it reconciled through — `declare`, `remove`, `rename`, `retype`,
  `setOptions`, `setBounds`, `setCategory`, `setVisibility`, `setDescription` — are unchanged and still
  public: a plugin declares its own rows by calling them, which is what they were the implementation of all
  along. What is gone is the wire form for a *host* asking, because a user parameter is a `@Param` field in
  the bot's own Java now and the host edits it there.
- **`Settings.load`/`loadAll`/`declares` are for a *plugin's* rows now** — activity enable flags and
  whatever a plugin declares for itself. They are unchanged and never deleted (a bot compiled against them
  cannot be rewritten); what changed is that a *user* parameter is no longer one of them. The javadoc says
  so at the top of the class.

## [0.0.2] — 2026-09-17

### Added

- **`@Param`** (`com.botmaker.plugin.basics.params.Param`) — a user parameter is a field in the bot's own
  Java now, not a row in a JSON file read back by name:

  ```java
  @Param(category = "Limits", min = "1", max = "50")
  public static int maxAttempts = 10;
  ```

  The bot reads `Parameters.maxAttempts`, so a misspelling is a compile error and the type is the type.
  Studio reads the same fields off the syntax tree and draws the Parameters window from them. The members
  are `category` (free text — the six SDK categories were a vocabulary), `description`, `visibility`
  (`Param.EDITOR` or `Param.PUBLIC`, strings because the contract's `Visibility` is off a bot's classpath),
  `min`, `max` and `options`.
- **`PluginStore`** — a plugin's own state as the plugin's own records, one line each way:
  `store.write("capture", targets)` and `store.read("capture", Targets.class)`, over `PluginData`'s file
  tree with Jackson. Reading is total (absent, unparseable, wrongly shaped and `{}` all read as empty);
  writing throws, because a save that silently did not happen is the one failure a user cannot see. A field
  the record no longer declares is ignored, so a plugin can still read what an older version of itself
  wrote.
- **`Settings.forPlugin(id)`** — the same files from inside a running bot, read by one resolved classpath
  path. `read(name, Class)` and `readAll(name, Class)`, same rules.
- **The nine types read their own Java back** (`ValueCodec.wireOfLiteral`, new in the contract). A
  `@Param` field's value *is* its initialiser, so a type that can write `java.time.Duration.ofMillis(3000L)`
  and not read it would be one the editor writes and then refuses to edit. Each inverse is written in the
  same expression as the literal it undoes, and recognises **only** what this plugin emits:
  `Duration.ofSeconds(3)` means the same thing and is declined, because the author wrote that on purpose and
  the window shows it as written rather than rewriting it on open.

### Changed

- **`ParameterStore.declared(ParameterDeclaration)` is gone**, with the contract method it implemented
  (studio-api, 2026-09-17). The nine verbs it reconciled through — `declare`, `remove`, `rename`, `retype`,
  `setOptions`, `setBounds`, `setCategory`, `setVisibility`, `setDescription` — are unchanged and still
  public: a plugin declares its own rows by calling them, which is what they were the implementation of all
  along. What is gone is the wire form for a *host* asking, because a user parameter is a `@Param` field in
  the bot's own Java now and the host edits it there.
- **`Settings.load`/`loadAll`/`declares` are for a *plugin's* rows now** — activity enable flags and
  whatever a plugin declares for itself. They are unchanged and never deleted (a bot compiled against them
  cannot be rewritten); what changed is that a *user* parameter is no longer one of them. The javadoc says
  so at the top of the class.

## [0.0.1] — 2026-09-16

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
  `setVisibility`, `setDescription`, reachable individually and, as `declared(ParameterDeclaration)`, as the
  one call the contract makes: the host states the row it wants and this reconciles it, which is where those
  nine live as an implementation rather than as a vocabulary — and the coercion that comes with being the
  editor: canonicalise
  through the owning type's codec, clamp to a declared `Range`, prune a value to the options still on offer,
  seed a fresh one with the type's default. A name is unique within a group and only there, so two plugins —
  and two groups of one plugin — may both offer a `timeout`, and a group only ever touches its own rows.

`JdkText`, `BasicsGrammar` and the rest of `store` name nothing but the JDK and Jackson, because they run in
a bot: this module reaches a bot's classpath through the SDK's `compile`-scope dependency on it, where the
contract and JavaFX — both `provided` — are absent. `BasicsIsBotSafeTest` scans the source and exempts only
`BasicsPlugin`, `BasicsValueTypes` and `ParameterStore`, so a new class is checked by default.

What is left for a later phase is giving the SDK plugin's activities and flow files of their own in the tree
rather than the top level of `activities.json` they still live at.
