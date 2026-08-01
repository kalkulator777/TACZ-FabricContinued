package com.tacz.guns.util;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.DebugLib;
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
 * <p>
 * <b>This is not a complete sandbox, and the gap is not in the library list.</b> Every script entry
 * point hands the script a live Java object through {@code CoerceJavaToLua.coerce(...)}. luaj builds
 * a coerced object's method table from {@code Class.getMethods()} filtered only on
 * {@code Modifier.isPublic}, so the inherited {@code getClass()} is exposed, and coercing the
 * {@code Class} it returns yields the members of {@code java.lang.Class} — reflection reaches the
 * whole JVM from there, without needing any of the libraries left out below. Closing that means
 * handing scripts an explicit wrapper that exposes only whitelisted methods and never returns a
 * bare {@code JavaInstance}; it would break any third-party pack relying on reflection today, so it
 * is written up in §7 of the roadmap rather than done here.
 * <p>
 * What is handled here is the other half: a script cannot run forever.
 */
public final class LuaSandbox {
    private LuaSandbox() {
    }

    /**
     * Aborts a script that keeps executing Lua without pause. Scripts run synchronously on the game
     * thread, once per tick and once per frame, so {@code while true do end} in a pack is a hang
     * with no recovery — and with no {@link DebugLib} installed there is no hook to interrupt it
     * with, which is why one is installed (and its global removed again).
     * <p>
     * The budget is wall clock, not an instruction count, and it resets itself on any pause in
     * execution rather than on entering a call. Tracking call depth would be the obvious way to
     * know where one invocation ends, but {@code onReturn} is not called when a {@code LuaError}
     * unwinds, so a depth counter drifts upward and never comes back to zero — after the first
     * error every later script would be killed. A gap between two checks means Lua was not running,
     * which is true exactly when one invocation has ended and the next has not begun, and is true
     * regardless of how the previous one ended.
     */
    private static final class ExecutionBudget extends DebugLib {
        /** Continuous Lua execution allowed before the script is killed. No honest script is close. */
        private static final long BUDGET_NS = 500_000_000L;
        /** A gap this long between checks means execution paused, so the next run starts fresh. */
        private static final long IDLE_RESET_NS = 2_000_000L;
        /** Reading the clock per instruction would cost more than the check is worth. */
        private static final int CHECK_INTERVAL = 4096;

        private int sinceCheck;
        private long lastCheckNs;
        private long startNs;

        @Override
        public void onInstruction(int pc, Varargs varargs, int top) {
            super.onInstruction(pc, varargs, top);
            if (++sinceCheck < CHECK_INTERVAL) {
                return;
            }
            sinceCheck = 0;
            long now = System.nanoTime();
            if (now - lastCheckNs > IDLE_RESET_NS) {
                startNs = now;
            } else if (now - startNs > BUDGET_NS) {
                lastCheckNs = now;
                startNs = now;
                throw new LuaError("script exceeded its execution budget and was aborted");
            }
            lastCheckNs = now;
        }
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
        /* DebugLib is installed for its hook mechanism only — it is what lets setHook abort a
         * runaway script — and the global it registers is removed again immediately, so a pack
         * cannot reach debug.getinfo, debug.sethook or debug.setmetatable. */
        globals.load(new ExecutionBudget());
        globals.set("debug", LuaValue.NIL);
        // Modules are resolved through package.preload, which is populated from the resource manager.
        // Cutting the resource finder disables loadfile/dofile and the filesystem searcher of require,
        // so a script cannot reach outside the pack it came from.
        globals.finder = filename -> null;
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }
}
