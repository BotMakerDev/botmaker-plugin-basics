package com.botmaker.plugin.basics.params;

import com.botmaker.plugin.api.params.Param;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The annotation's shape, which is a contract between three parties that never meet: the bot's author who
 * writes it, Studio which reads it off the syntax tree, and anything reading a compiled bot by reflection.
 *
 * <p>Nothing here checks what Studio <em>does</em> with a member — that is Studio's own test over its
 * scanner. What is checked is that the members exist, that their defaults are the documented ones, and that
 * the retention lets a reflective reader see them at all.
 */
class ParamAnnotationTest {

    /** A bot, in miniature: the shapes the window has to draw. */
    @SuppressWarnings("unused")
    static final class Fixture {

        @Param
        public static Duration plain = Duration.ofSeconds(3);

        @Param(category = "Limits", description = "How many times to try",
                visibility = Param.PUBLIC)
        public static int described = 10;

        @Param
        public static List<String> many = List.of();

        /** Not a parameter: no annotation. */
        public static String ignored = "";
    }

    @Test
    void itMarksAGroupOrAFieldAndSurvivesToRuntime() {
        Retention retention = Param.class.getAnnotation(Retention.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "a compiled bot is read by reflection as well as by Studio's parser");
        Target target = Param.class.getAnnotation(Target.class);
        assertArrayEquals(new ElementType[] {ElementType.TYPE, ElementType.FIELD}, target.value());
    }

    @Test
    void anUnadornedParameterDefaultsToEverythingUnsaid() throws Exception {
        Param param = Fixture.class.getField("plain").getAnnotation(Param.class);
        assertEquals("", param.category());
        assertEquals("", param.description());
        // The default is the narrower of the two: a parameter nobody classified is not in the Runner.
        assertEquals(Param.EDITOR, param.visibility());
    }

    @Test
    void everyMemberIsReadBackAsWritten() throws Exception {
        Param param = Fixture.class.getField("described").getAnnotation(Param.class);
        assertEquals("Limits", param.category());
        assertEquals("How many times to try", param.description());
        assertEquals(Param.PUBLIC, param.visibility());
    }

    @Test
    void itSaysNothingAboutTheValue() {
        // 2026-10-29: min, max and options went; a value's rules are its type's.
        assertEquals(List.of("category", "description", "visibility"),
                Arrays.stream(Param.class.getDeclaredMethods()).map(Method::getName)
                        .sorted().toList());
    }

    @Test
    void theTwoVisibilitiesAreTheSpellingsStudioCompares() {
        // Constants rather than literals on both sides: this test exists to fail if either string changes,
        // because a project's Java holds whatever was spelled at the time.
        assertEquals("editor", Param.EDITOR);
        assertEquals("public", Param.PUBLIC);
    }

    @Test
    void afieldWithoutTheAnnotationIsNotAParameter() throws Exception {
        assertTrue(Fixture.class.getField("ignored").getAnnotation(Param.class) == null);
    }
}
