package com.tacz.guns.util;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gun packs are untrusted zip files a player drops into the config folder, and a server sends
 * attachment scripts to every client that connects. These tests exist so that a future change back
 * to {@code JsePlatform.standardGlobals()} fails the build instead of quietly reintroducing
 * arbitrary code execution.
 */
class LuaSandboxTest {
    @Test
    void dangerousLibrariesAreNotReachable() {
        Globals globals = LuaSandbox.createGlobals();
        for (String library : new String[]{"io", "os", "luajava", "debug", "coroutine"}) {
            assertTrue(globals.get(library).isnil(), library + " must not be reachable from a gun pack script");
        }
    }

    @Test
    void dofileCannotReachTheFilesystem() {
        Globals globals = LuaSandbox.createGlobals();
        LuaValue chunk = globals.load("dofile('build.gradle')", "test");
        assertThrows(LuaError.class, chunk::call);
    }

    @Test
    void loadfileCannotReachTheFilesystem() {
        Globals globals = LuaSandbox.createGlobals();
        globals.load("ok, err = loadfile('build.gradle')", "test").call();
        assertTrue(globals.get("ok").isnil(), "loadfile must not resolve a path outside the pack");
    }

    @Test
    void requireCannotFallBackToTheFilesystem() {
        Globals globals = LuaSandbox.createGlobals();
        LuaValue chunk = globals.load("require('os')", "test");
        assertThrows(LuaError.class, chunk::call);
    }

    @Test
    void theLibrariesScriptsActuallyNeedArePresent() {
        Globals globals = LuaSandbox.createGlobals();
        globals.set("x", LuaValue.valueOf(3.4));
        globals.load("y = math.floor(x) .. string.rep('!', 2) .. #({1, 2, 3})", "test").call();
        assertEquals("3!!3", globals.get("y").tojstring());
    }

    @Test
    void moduleLookupGoesThroughPreload() {
        Globals globals = LuaSandbox.createGlobals();
        globals.get("package").get("preload").set("greeting", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf("hello");
            }
        });
        globals.load("y = require('greeting')", "test").call();
        assertEquals("hello", globals.get("y").tojstring());
    }
}
