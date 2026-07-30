package com.tacz.guns.util;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

/**
 * Builds the sandboxed Lua environment used for every script that originates from a gun pack.
 * <p>
 * Gun packs are untrusted input: they are ordinary zip files a player drops into the config folder.
 * Never use {@code JsePlatform.standardGlobals()} for them — it installs {@code luajava}, {@code io},
 * {@code os} and {@code debug}, any one of which turns a gun pack into arbitrary code execution.
 */
public final class LuaSandbox {
    private LuaSandbox() {
    }

    public static Globals createGlobals() {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new JseStringLib());
        // No CoroutineLib
        globals.load(new JseMathLib());
        // No JseIoLib
        // No JseOsLib
        // No LuajavaLib
        // No DebugLib
        // Modules are resolved through package.preload, which is populated from the resource manager.
        // Cutting the resource finder disables loadfile/dofile and the filesystem searcher of require,
        // so a script cannot reach outside the pack it came from.
        globals.finder = filename -> null;
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }
}
