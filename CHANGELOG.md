# Changelog

All notable changes to `botmaker-plugin-basics` are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the versions are the git
tags the umbrella's `release.sh` cuts. Write under `## [Unreleased]`: the version number is not knowable
while the prose is being written — it is what the decide pass computes — and the release stamps it onto the
heading in that module's own release commit.

## [Unreleased]

No source changes since v0.0.5; re-released for updated upstream pins.

No source changes since v0.0.4; re-released for updated upstream pins.

No source changes since v0.0.3; re-released for updated upstream pins.

No source changes since v0.0.2; re-released for updated upstream pins.

### Changed

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
