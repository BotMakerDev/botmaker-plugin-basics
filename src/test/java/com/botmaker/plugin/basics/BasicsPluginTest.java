package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;
import com.botmaker.plugin.basics.values.BasicsTypes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a host asks a plugin: that it can find it, construct it, and get a well-formed answer to each of
 * its questions — nine types since 2026-09-09, and empty for the rest.
 *
 * <p>The discovery half runs {@link ServiceLoader} on this module's own classpath rather than through
 * {@code PluginLoader}. That is deliberate: {@code botmaker-plugin-host} is a <em>host's</em> dependency,
 * and a plugin taking it — even at test scope — would be modelling the wrong relationship in the one module
 * whose job is to be an example of the right one. What a real host adds on top is the classloader, and that
 * is {@code PluginLoaderTest}'s subject in the module that owns it; what is checked here is the part that
 * belongs to this jar, and the part that actually goes wrong — a {@code META-INF/services} file naming a
 * class that has been renamed or moved fails exactly here and nowhere else in a compile.
 */
class BasicsPluginTest {

    private final BasicsPlugin plugin = new BasicsPlugin();

    @Test
    void the_service_declaration_names_this_class() {
        List<String> found = new ArrayList<>();
        for (StudioPlugin discovered : ServiceLoader.load(StudioPlugin.class)) {
            found.add(discovered.id());
        }
        assertTrue(found.contains(BasicsPlugin.ID),
                "ServiceLoader found " + found + " — check META-INF/services/"
                        + StudioPlugin.class.getName());
    }

    @Test
    void the_id_and_the_display_name_are_the_registered_ones() {
        assertEquals("com.botmaker.basics", plugin.id());
        assertEquals("BotMaker Basics", plugin.displayName());
    }

    @Test
    void every_contribution_is_well_formed_and_none_of_them_is_null() {
        // An empty catalog with no problems() is what a host reads as "this plugin offers no blocks",
        // which is a supported state — not the same thing as a malformed one, which would still have to
        // let the project open.
        assertTrue(plugin.catalog().problems().isEmpty(), plugin.catalog().problems().toString());
        assertTrue(plugin.toolbarItems().isEmpty());
    }

    /**
     * No slot editors, and that is the shape to notice: an editor for a type this plugin declares lives on
     * the type, so this surface is for the two things basics does not do — claiming a slot by the call
     * around it, and overriding somebody else's type.
     */
    @Test
    void it_overrides_nobody_and_claims_no_call() {
        assertTrue(plugin.slotEditors().isEmpty());
    }

    @Test
    void the_types_are_the_nine_this_plugin_declares() {
        // Through the plugin rather than through BasicsTypes.ALL directly: what a host gets is the memoised
        // buildTypes() hook, and a plugin that declares types nobody can reach is the bug.
        assertEquals(BasicsTypes.ALL, plugin.types());
        assertEquals(9, plugin.types().size(), plugin.types().toString());
        assertSame(plugin.types(), plugin.types(), "the build hook must run at most once");
    }

    /**
     * Every declaration answers a class and a fresh value without throwing, and every composite one
     * round-trips — the two laws {@code botmaker plugin validate} checks over any plugin.
     */
    @Test
    void every_declared_type_answers_a_class_and_a_fresh_value() {
        for (PluginType<?> type : plugin.types()) {
            assertTrue(type.type() != null, type.getClass().getName() + " declares no class");
            Object fresh = type.fresh();
            assertTrue(fresh != null, type.type() + " has no fresh value");
            assertTrue(boxed(type.type()).isInstance(fresh),
                    type.type() + " answered a fresh " + fresh.getClass());
        }
    }

    @Test
    void every_composite_type_round_trips_its_fresh_value() {
        for (PluginType<?> type : plugin.types()) {
            if (!(type instanceof ComponentType<?> composite)) continue;
            Object fresh = type.fresh();
            assertEquals(fresh, composite.build(composite.componentsOf(fresh)),
                    type.type() + " does not read back what it writes");
            assertEquals(composite.componentTypes().size(), composite.componentsOf(fresh).size(),
                    type.type() + " promises a different number of components than it answers");
        }
    }

    /** A primitive's declaration answers a boxed value, which is the only thing a {@code T} can be. */
    private static Class<?> boxed(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
