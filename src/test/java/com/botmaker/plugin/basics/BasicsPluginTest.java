package com.botmaker.plugin.basics;

import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.basics.values.BasicsValueTypes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a host asks a plugin: that it can find it, construct it, and get a well-formed answer to each of its
 * four questions — nine value types since 2026-09-09, and empty for the other three.
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
        assertTrue(plugin.catalog(null).problems().isEmpty(), plugin.catalog(null).problems().toString());
        assertTrue(plugin.slotEditors().isEmpty());
        assertTrue(plugin.toolbarItems().isEmpty());
    }

    @Test
    void the_value_types_are_the_nine_this_plugin_registers() {
        // Through the plugin rather than through BasicsValueTypes.CATALOG directly: what a host gets is the
        // memoised buildValueTypes() hook, and a plugin that registers types nobody can reach is the bug.
        assertEquals(BasicsValueTypes.CATALOG.types(), plugin.valueTypes().types());
        assertEquals(9, plugin.valueTypes().types().size(), plugin.valueTypes().types().toString());
        assertSame(plugin.valueTypes(), plugin.valueTypes(), "the build hook must run at most once");
    }

    @Test
    void a_pinned_version_changes_nothing_yet() {
        // The parameter stays on the contract because another plugin may ship per-version curation. This
        // one does not, and says so rather than leaving the question open.
        assertEquals(plugin.catalog(null).facades(), plugin.catalog("v1.0.0").facades());
    }
}
