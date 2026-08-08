# Handoff

Written at the end of a cloud session, for whoever picks this up next — most
likely the same work continuing on a local machine with a real GPU. Read this
first, then `ROADMAP.md` §7 for detail.

`ROADMAP.md` is the plan and the record of findings. `CHANGELOG.md` is what
shipped. This file is the short version plus the things that only make sense to
say once, at a handover.

---

## Where the work stands

Branch `1.21.11`, everything pushed. It builds, it runs, and it has been played
on real hardware.

| | |
|---|---|
| Toolchain | Gradle 9.5.1, Loom 1.17-SNAPSHOT, Java 21, Fabric loader 0.19.3 |
| Target | Minecraft 1.21.11, Fabric API 0.141.6 |
| Phase 1 | done — toolchain |
| Phase 2 | done — the port itself, 1.21.1 → 1.21.11 |
| Phase 3 | not started — item data storage and the datafixer |
| Phase 4 | not started — 26.1 |

One open bug from the play test, six decisions waiting, and the phase 3 work.
All three lists are below.

## The single most important thing about moving to a local machine

**This environment could not see.** Software rendering under llvmpipe at
854x480 and about 12 frames per second. Four rendering "defects" were called
from screenshots in one session and exactly one was real: the scope mask was
misread three times from the same picture before a state dump found the actual
cause, the muzzle flash and the bullet holes were never broken at all, and the
gun smith table preview was called blank, then called fine, and was in fact
upside down — visible only after cropping the panel out and raising the
brightness.

On a real GPU most of that difficulty disappears. So the first thing worth doing
locally is not new work — it is looking at the things that were fixed but never
seen:

- the gun smith table preview, right-click the block (the automation here never
  managed the click)
- shell casings, which live inside a short timer like the muzzle flash did
- the ported `ammo_box` model with its `range_dispatch` thresholds
- the scope reticle bug below, which needs a smooth frame rate to show at all

Keep the habit anyway: **measure, do not infer.** Dump state, print numbers,
crop and magnify. Every wrong call in this project came from reading a picture
and every right one came from reading bytecode or a log line.

## Running it

```
./gradlew runClient
./gradlew build          # the jar lands in build/libs
```

Dev affordances that exist, all added during the port:

| Flag | What it does |
|---|---|
| `-PquickPlay=<save>` | straight into a save, no menu |
| `-PclientProps=a=1,b=2` | passes system properties to the client JVM (`-D` on Gradle does **not** reach the client) |
| `-Dtacz.renderDebug=true` | `RenderDebug`: framebuffer and stencil attachment state, scope circle geometry, deduplicated one-shot notes. Free when off |
| `-Dtacz.muzzleFlashMs=<ms>` | widens the 50 ms muzzle flash window; honoured only with the diagnostic on |

Iris is `modCompileOnly` and deliberately **not** in the runtime. Sodium 0.8.13
declares `breaks iris <=1.10.7`, and 1.10.7 is Iris's newest 1.21.11 release, so
the two current versions cannot both be loaded. That is upstream's problem, not
ours; do not "fix" it by adding Iris back, it will just fail mod resolution.

Verifying a rendering change means an actual client run. There is no shortcut,
and screenshots at low frame rates are not evidence.

## Conventions that matter

- Commits are authored `Claude <noreply@anthropic.com>`, with **no**
  `Claude-Session:` or `Co-Authored-By:` trailers.
- Comments in this tree explain *why*, in the language of the surrounding file
  (much of the original is Chinese; ported code follows suit). A comment that
  restates the code is noise; a comment that records what the old version did
  and why it could not survive is the point.
- Do not report something as fixed without saying how it was verified.

---

## TODO

### 1. Open bug from the play test

**The scope reticle floats.** A Mark 5 — the eyepiece path,
`renderScope` → `renderOcularAndDivision` — and the reticle drifts *bodily*
during the breathing animation, not thinning at the rim.

Ruled out, by reading bytecode rather than reasoning:

- the alpha cutout — `NO_DEPTH_TEST_PIPELINE` matches vanilla `ENTITY_CUTOUT`
  (`ENTITY_SNIPPET` + `ALPHA_CUTOUT 0.1` + `Sampler1`)
- mipmapping — nothing in the mod registers textures, packs load through vanilla
- that pipeline entirely — it is only used by `renderDivisionOnly`, the red-dot
  path. The eyepiece path draws the reticle with plain `RenderTypes.entityCutout`
- the stencil circle — clipping would eat the reticle's edges, and it does not

What is left: the division node and the ocular node are separate bones driven
through the same `translateAndRotateAndScale`. Compare the pose the division node
ends up with against the ocular's. `renderTempPart` walks each path itself.

### 2. Decisions — these are for a human, not a patch

Each trades against something code cannot weigh on its own.

- **A real Lua sandbox.** A gun pack is an untrusted zip a player downloads, and
  its scripts run on both client and server. `LuaSandbox` drops `io`, `os`,
  `luajava` and `debug`, but every entry point hands the script a live Java object
  through `CoerceJavaToLua.coerce(...)`, and luaj exposes the inherited
  `getClass()` — reflection reaches the whole JVM from there without needing any
  of the removed libraries. Closing it means an explicit wrapper exposing only
  whitelisted methods and never returning a bare `JavaInstance`. Bounded work;
  breaks any third-party pack that uses reflection today. **The hang half is
  already fixed** — there is an execution budget now, so the exposure is code
  execution by a pack the player chose to install, not a freeze.
- **Rate limits on the C2S packets.** None of the 15 has one; fire-select, draw
  and zoom each write NBT, broadcast a full `ItemStack` to every tracking player
  and re-run the pack's Lua modifiers, per packet. The limiter is easy, the
  numbers depend on how the server is played.
- **Narrowing the hit broadcast.** `sendToDimension` reaches every player in the
  dimension with victim, shooter, damage and headshot flag. Could be
  tracking-only. Looks free; it is still a networking change with no local test.
- **The three no-argument `endBatch()` calls** in `LeftHandRender`,
  `RightHandRender`, `TextShowRender`. They drain every buffered render type, not
  just their own. Wasteful — but which layer the arms and text land in depends on
  it. Visual change; now testable locally.
- **`SecondOrderDynamics`.** Six threads spinning for the life of the JVM, in the
  main menu too, plus a cross-thread race on plain `float` fields. Byte-identical
  to 1.21.1, so fixing it changes inherited behaviour: the fixed `t = 0.05` per
  6 ms iteration is presumably how the constants were tuned, and a correct
  fixed-step integrator will feel different.
- **The per-entity `RenderLivingEvent` allocation.** Fabric's `Event` has no
  `hasListeners()`, and every alternative copies the listener's checks into the
  mixin. Small cost, no clean fix.

### 3. Work with no decision attached

- Remove the stale `"refmap": "tacz.refmap.json"` from `tacz.mixins.json`. The
  file does not exist; annotations are remapped to intermediary at build time, so
  this is a log warning only. Deferred once to avoid changing the build right
  before handing over a jar.
- Sweep the remaining blocks onto the new blockstate format.
- Phase 3: item data storage, world migration, and a deprecated-accessor facade
  for addons. This is what 1.21.11 exists as a release for.
- Phase 4: 26.1.
- The threading audit did not reach: the `synchronized` asymmetry in
  `AnimationController`, entity retention in `RenderStateKeys` and
  `CycleTaskHelper`, and the `compat/` packages (JEI, REI, KubeJS, player
  animator). Not findings — unexamined places.

### 4. For phase 4, one piece of borrowed knowledge

[q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
forks the same `Sh1roCu/TACZ-Refabricated` 1.21.1 branch this tree does and went
straight to 26.1.2 and 26.2. GPL-3.0 like us, so borrowing is legal with
attribution.

It builds the scope out of an **offscreen mask texture**, not the stencil buffer:
`ScopeMaskTarget`, `ScopeMaskGeometry`, `ScopeMaskTextureHandle`, plus separate
etched and illuminated reticle renderers. That is the design that survives a
deferred shader pipeline, and it is the answer to the Iris incompatibility this
tree still has. Consider it before porting `StencilSupport` forward.

It also has picture-in-picture disabled, which works here — so it is ahead on the
scope and behind on the preview, not a better base. And its history is ~40
commits in two days, many of them literally "Add files via upload", so there is
nothing cherry-pickable; anything taken has to be re-derived.

---

## What was done in the last session, in one paragraph

The walk animation ran 6.7x too fast because `getWalkDist` was ported onto
`walkAnimation.position()`, which counts `min(distance * 4, 1)` per tick where the
old `Entity.walkDist` counted `distance * 0.6` — and the lua drives the walk and
run animations from that number so they stay in step with the footsteps. Then a
five-way audit (pack loading and Lua, networking, rendering, gameplay logic,
threading) whose verified findings are all fixed across four commits: the
collector path replaying the previous submission's delegates, the gun smith table
under-charging, `(int)` truncating a fractional RPM multiplier, magazine unload
losing rounds, `(ScheduledFuture) Thread.currentThread()` throwing on every shot,
a respawn NPE, handlers registered on both tick phases, a Lua execution budget,
two threads blocked on nothing, a sound-consumer leak, and the per-frame
allocations in `BedrockCube*.compile` and `BedrockPart`. One reported finding was
checked and **rejected**: Fabric marshals play payload handlers onto the client
thread itself, so the "missing `context.client().execute`" was not a race — the
ones we do have are redundant.
