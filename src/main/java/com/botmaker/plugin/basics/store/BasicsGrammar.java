package com.botmaker.plugin.basics.store;

import com.botmaker.plugin.basics.values.JdkText;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The nine JDK value types, spelled for a running bot.
 *
 * <p>Declared in {@code META-INF/services/com.botmaker.plugin.basics.store.ValueGrammar}, so a bot with this
 * plugin on its classpath — which every bot with the SDK has, since the SDK depends on it — can write
 * {@code Settings.load("wait", Duration.class)} and nothing else has to be arranged.
 *
 * <h2>Every reader is one {@link JdkText} call, and that is the point</h2>
 *
 * <p>{@code JdkText} has two readers: an editor, through {@code BasicsValueTypes}' {@code ValueCodec}s, and a
 * running bot, through this class. Writing the parsers a second time here is the exact drift the design
 * refuses — the two halves would disagree about what {@code "3s500ms"} means, and the disagreement would be
 * invisible until a bot behaved differently from what the Parameters window showed.
 *
 * <h2>Two grammars, which is what the mechanism was built for</h2>
 *
 * <p>The SDK ships {@code SdkGrammar} for its own eight types and this ships these nine, and
 * {@link Settings} indexes both off the classpath. That arrangement was theoretical until 2026-09-09: there
 * was one plugin, so there was one grammar, and the rule <i>two grammars may not claim one type</i> had
 * nothing to refuse. It has two now, and they are disjoint by construction — the same split as the two
 * {@code ValueCatalog}s.
 *
 * <p><b>No contract type appears here.</b> {@code ValueType} and {@code ValueCodec} are
 * {@code com.botmaker.plugin.api.value}, which is not on a bot's classpath, and naming one would make this
 * class unloadable in exactly the process it exists for.
 */
public final class BasicsGrammar implements ValueGrammar {

    @Override
    public List<Reader<?>> readers() {
        return List.of(
                new Reader<>(String.class, JdkText::text, s -> s, ""),
                new Reader<>(Boolean.class, JdkText::flag, String::valueOf, false),
                new Reader<>(Integer.class, JdkText::whole, String::valueOf, 0),
                new Reader<>(Double.class, JdkText::decimal, String::valueOf, 0.0),
                new Reader<>(Character.class, JdkText::letter, String::valueOf, 'a'),
                new Reader<>(Color.class, JdkText::color, JdkText::spellColor, Color.WHITE),
                new Reader<>(LocalDate.class, JdkText::date, LocalDate::toString, JdkText.date("")),
                new Reader<>(LocalTime.class, JdkText::time, LocalTime::toString, LocalTime.MIDNIGHT),
                new Reader<>(Duration.class, JdkText::duration,
                        d -> JdkText.spellDuration(d.toMillis()), Duration.ZERO));
    }
}
