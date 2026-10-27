# botmaker-plugin-basics

**Plugin #2** (`com.botmaker.basics`): the JDK value types a bot's parameters are made of, and their
editors. Nothing in it runs in a bot.

It is a **plugin**, not a platform module: Studio does not ship it, does not depend on it and does not
resolve it. A project that wants it declares it, and `PluginLoader` puts it on that project's own
classloader beside every other plugin the project resolves. The SDK brings it; a project started from the
*Base* template declares it directly, so its Parameters window offers any type at all.

```xml
<dependency>
    <groupId>com.github.BotMakerDev</groupId>
    <artifactId>botmaker-plugin-basics</artifactId>
    <version><!-- latest tag: https://github.com/BotMakerDev/botmaker-plugin-basics/tags --></version>
</dependency>
```

## What it owns

| Types | |
|---|---|
| `String`, `boolean`, `int`, `long`, `double`, `char` | text, a flag, numbers stepped with ▲/▼ |
| `java.awt.Color`, `LocalDate`, `ZoneOffset` | one editor each |
| `LocalTime`, `OffsetTime` | `TimeDial`, a 24-hour two-ring clock |
| `Duration` | `DurationPicker`: presets, spinners that carry, the length in words |
| `DayOfWeek`, `Month` | their constants |

The class is the identity — a project's file says `java.time.Duration` because that is what the field is
declared as — and a primitive is declared as itself (`int`, not `Integer`). Each type is a
`PluginType.value(…)` whose factory is a method reference (`LocalDate::of`, `Color::new`); a value out of
range builds nothing and is shown as written.

Opening a picker and pressing OK without a change leaves the file byte-identical. The SDK offers a second
editor for `Color`, an eyedropper over the capture target; Studio asks which one to use.

## Depending on it from another plugin

Ordinary Maven — not through the contract, and not through a service lookup. `PluginLoader` builds **one**
`URLClassLoader` over the whole resolved project classpath and constructs, in it, the class each plugin jar's
pom names in `botmaker.plugin`, so
plugin A sees plugin B's classes and Maven has already mediated B to a single version.

**Do not declare it directly beside a plugin that already brings it.** Maven's nearest-wins mediation makes
the direct entry win, and a bot then runs a version its plugin was never built against — a
`NoSuchMethodError` at whichever method moved.

## Building

```bash
mvn verify                                   # here: the two laws (build(components(v)) == v, fresh() is real) and the pickers' rules
mvn -pl botmaker-plugin-basics -am install   # from the umbrella root, with the contract and the toolkit
```

## Releasing

From the umbrella only, with `--plugin-basics <version>` plus the downstreams it forces (sdk, and studio
through it); `/release-check` names them. A contract or toolkit release forces one here, because the
published pom bakes those pins in.
