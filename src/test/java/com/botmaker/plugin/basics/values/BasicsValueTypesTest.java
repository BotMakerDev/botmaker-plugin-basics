package com.botmaker.plugin.basics.values;

import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registrations, and the two properties that outlive any refactor of them: <b>the ids are what stored
 * projects hold</b>, and <b>a stored value read then written back is unchanged</b>.
 */
class BasicsValueTypesTest {

    private static final ValueCatalog CATALOG = BasicsValueTypes.CATALOG;

    /**
     * The ids are the names the SDK's enum constants had, and the move must be invisible in a project file.
     * Written out rather than derived: deriving them from the constants would assert nothing at all.
     */
    @Test
    void theIdsAreTheOnesEveryStoredProjectAlreadyHolds() {
        assertEquals(List.of("TEXT", "YES_NO", "WHOLE_NUMBER", "DECIMAL_NUMBER", "CHARACTER", "COLOR",
                        "DATE", "TIME_OF_DAY", "DURATION"),
                CATALOG.types().stream().map(ValueType::id).toList());
    }

    @Test
    void aPluginAuthorAsksWithAClassAndNeverWritesAnIdDown() {
        assertEquals(BasicsValueTypes.TEXT, CATALOG.forJava(String.class).orElseThrow());
        assertEquals(BasicsValueTypes.YES_NO, CATALOG.forJava(boolean.class).orElseThrow());
        assertEquals(BasicsValueTypes.WHOLE_NUMBER, CATALOG.forJava(int.class).orElseThrow());
        assertEquals(BasicsValueTypes.DECIMAL_NUMBER, CATALOG.forJava(double.class).orElseThrow());
        assertEquals(BasicsValueTypes.CHARACTER, CATALOG.forJava(char.class).orElseThrow());
        assertEquals(BasicsValueTypes.COLOR, CATALOG.forJava(Color.class).orElseThrow());
        assertEquals(BasicsValueTypes.DATE, CATALOG.forJava(LocalDate.class).orElseThrow());
        assertEquals(BasicsValueTypes.TIME_OF_DAY, CATALOG.forJava(LocalTime.class).orElseThrow());
        assertEquals(BasicsValueTypes.DURATION, CATALOG.forJava(Duration.class).orElseThrow());
    }

    /** A wrapper finds the type its primitive names, which is what lets a boxed list be asked for. */
    @Test
    void aWrapperFindsWhatItsPrimitiveNames() {
        assertEquals(BasicsValueTypes.WHOLE_NUMBER, CATALOG.forJava(Integer.class).orElseThrow());
        assertEquals(BasicsValueTypes.YES_NO, CATALOG.forJava(Boolean.class).orElseThrow());
    }

    /** {@code store(parse(wire))} is a fixed point: the editor shows what the bot will actually get. */
    @Test
    void normalisingIsStableAndCanonical() {
        assertEquals("1m30s", CATALOG.normalize("DURATION", "90 s"));
        assertEquals("1m30s", CATALOG.normalize("DURATION", CATALOG.normalize("DURATION", "90 s")));
        assertEquals("#3366FF", CATALOG.normalize("COLOR", "3366ff"));
        assertEquals("  spaced  ", CATALOG.normalize("TEXT", "  spaced  "));
    }

    /**
     * Every literal is the <em>parsed</em> value, so a generated file holds nothing that can throw while its
     * class initialises — the rule that lets a bot fail to read a value without failing to start.
     */
    @Test
    void theLiteralsAreValuesRatherThanParseCalls() {
        assertEquals("\"hi\\n\"", literal(BasicsValueTypes.TEXT, "hi\n"));
        assertEquals("'x'", literal(BasicsValueTypes.CHARACTER, "xyz"));
        assertEquals("new java.awt.Color(51, 102, 255)", literal(BasicsValueTypes.COLOR, "#3366FF"));
        assertEquals("java.time.LocalDate.of(2026, 8, 25)", literal(BasicsValueTypes.DATE, "2026-08-25"));
        assertEquals("java.time.LocalTime.of(7, 30, 15)", literal(BasicsValueTypes.TIME_OF_DAY, "07:30:15"));
        assertEquals("java.time.Duration.ofMillis(90000L)", literal(BasicsValueTypes.DURATION, "90s"));
        assertEquals("42", literal(BasicsValueTypes.WHOLE_NUMBER, "42"));
    }

    /** Nothing here needs an import: every non-primitive is written fully qualified. */
    @Test
    void noTypeHereAsksForAnImport() {
        for (ValueType type : CATALOG.types()) {
            assertTrue(CATALOG.imports(ValueForm.of(type)).isEmpty(), type.id());
        }
    }

    private static String literal(ValueType type, String wire) {
        return CATALOG.literal(type.id(), wire).orElseThrow().source();
    }
}
