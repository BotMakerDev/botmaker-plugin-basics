package com.botmaker.plugin.basics.managed;

import com.botmaker.plugin.api.managed.Managed;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a bot's {@code main} gets for naming a plugin's values class, and what it is spared.
 *
 * <p>Every test claims an <b>id of its own</b> rather than resetting the registry between them. The claims
 * are static because a plugin registers them from a static initialiser, so a reset method would exist for
 * the tests alone — and the thing worth asserting is that two plugins' ids do not collide, which distinct
 * ids assert by construction.
 */
class ManagedValuesTest {

    /** A plugin's values class as a bot holds one: {@code public static}, no arguments, one expression. */
    static final class Values {

        @Managed("test.flow")
        public static String flow() {
            return "the flow";
        }

        @Managed("test.capture")
        public static String capture() {
            return "the capture source";
        }
    }

    @Test
    void everyManagedMethodReachesWhoeverClaimedItsId() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim("test.flow", value -> taken.add("flow=" + value));
        ManagedValues.claim("test.capture", value -> taken.add("capture=" + value));

        ManagedValues.install(Values.class);

        // Name order, which is what install sorts by: capture before flow. Reflection promises no order of
        // its own, so this is the assertion that two bots install in the same sequence.
        assertEquals(List.of("capture=the capture source", "flow=the flow"), taken);
    }

    static final class Unclaimed {

        @Managed("test.nobody-reads-this")
        public static String value() {
            return "ignored";
        }
    }

    @Test
    void anIdNoPluginClaimsIsIgnoredRatherThanThrown() {
        // The ordinary state of a bot whose pom no longer names the plugin that reads this value. The file
        // is still in the project and still compiles; the value simply goes nowhere.
        ManagedValues.install(Unclaimed.class);
    }

    @SuppressWarnings("unused")
    static final class NotValues {

        /** Not public: the bot can call it, nothing installs it. */
        @Managed("test.not-public")
        static String hidden() {
            return "no";
        }

        /** Not static: a plugin's value belongs to the bot, not to an instance. */
        @Managed("test.not-static")
        public String perInstance() {
            return "no";
        }

        /** Takes an argument: a value is not computed from arguments. */
        @Managed("test.takes-arguments")
        public static String computed(String argument) {
            return argument;
        }

        /** No annotation at all — an ordinary helper in the same file. */
        public static String helper() {
            return "no";
        }
    }

    @Test
    void onlyAPublicStaticNoArgumentMethodIsAValue() {
        List<String> taken = new ArrayList<>();
        for (String id : List.of("test.not-public", "test.not-static", "test.takes-arguments")) {
            ManagedValues.claim(id, value -> taken.add(id));
        }

        ManagedValues.install(NotValues.class);

        // The same three rules the editor applies before it will rewrite a @Managed body. A method that
        // breaks one is the author's own code, and installing it would be running something nobody declared
        // as a value.
        assertTrue(taken.isEmpty(), () -> "installed something that is not a value: " + taken);
    }

    /** The other shape of {@code @Managed}: a class of constants the bot names at its use sites. */
    @Managed("test.pictures")
    static final class Pictures {

        public static final String COLLECT = "collect.png";
    }

    @Test
    void managedOnATypeInstallsNothing() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim("test.pictures", value -> taken.add(String.valueOf(value)));

        ManagedValues.install(Pictures.class);

        // Pictures.COLLECT is read where it is written. There is no value to hand anybody, and handing over
        // the class would be inventing a meaning the annotation does not have on a type.
        assertTrue(taken.isEmpty(), () -> "installed a type-level @Managed: " + taken);
    }

    static final class OneThrows {

        @Managed("test.throws")
        public static String broken() {
            throw new IllegalStateException("the bot author's own code");
        }

        @Managed("test.survives")
        public static String fine() {
            return "installed anyway";
        }
    }

    @Test
    void aValueThatThrowsCostsOnlyItself() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim("test.throws", value -> taken.add("throws=" + value));
        ManagedValues.claim("test.survives", value -> taken.add("survives=" + value));

        ManagedValues.install(OneThrows.class);

        // One plugin's broken value must not stop the rest of the bot being wired up — the same rule every
        // other pass over plugin code here follows.
        assertEquals(List.of("survives=installed anyway"), taken);
    }

    @Test
    void namingNoValuesClassAtAllIsLegal() {
        // What a bot with no plugins installed writes, and what run(anchor, goHome) passes.
        ManagedValues.install();
        ManagedValues.install((Class<?>[]) null);
    }
}
