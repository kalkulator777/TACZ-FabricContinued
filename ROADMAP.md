## Roadmap

Where this fork is going and how it plans to get there.

The short version: the mod is on 1.21.1 today. The goal is Minecraft 26.1, with
**1.21.11 as a shipped release along the way**, not as a detour.

Everything here is a plan, not a promise. Dates are deliberately absent.

Current work is phase 2, the version bump itself — see [§6](#6-phases) for what is
done and what is next, and the [changelog](CHANGELOG.md) for what has already landed.
The tree does not compile while that is under way.

---

### Contents

1. [Version targets](#1-version-targets)
2. [What this means for gun packs, worlds and addons](#2-what-this-means-for-gun-packs-worlds-and-addons)
3. [Dependency status](#3-dependency-status)
4. [SimpleBedrockModel](#4-simplebedrockmodel)
5. [Item data storage](#5-item-data-storage)
6. [Phases](#6-phases)
7. [Known work items](#7-known-work-items)
8. [Risks](#8-risks)
9. [Links](#9-links)

---

### 1. Version targets

```
1.21.1  ──►  1.21.11  ──►  26.1
(now)        (release)     (goal)
```

**Why not go straight to 26.1.** Every painful change between 1.21.1 and 26.1
already happened before 1.21.11:

| Drop | What broke |
|---|---|
| 1.21.2 | entity render states, `Ingredient` became a `HolderSet`, the `Recipe` interface was rewritten, minecarts reworked, explosions split, damage handling split |
| 1.21.4 | item model rework — `BuiltinItemRendererRegistry` and `BlockEntityWithoutLevelRenderer` removed |
| 1.21.5 | `RenderPipeline` replaced `ShaderInstance`, stencil helpers removed, `CompoundTag` moved to `Optional` getters |
| 1.21.6 | `GlStateManager` and `LayeredDraw` gone, `GuiGraphics.pose()` became a 2D `Matrix3x2fStack`, `ValueInput`/`ValueOutput` serialization |
| 1.21.9 | blockstate JSON format, a large renaming pass in the official mappings, input moved into records |

Reaching 1.21.11 absorbs all of it. What remains for 26.1 is mostly the switch to
an unobfuscated toolchain plus one drop's worth of API churn. So 1.21.11 is not
extra work — it is the same work with a point where a release can ship.

The renaming pass deserves its own line, because it is the single largest edit in
the port and it was not on this list before the work started. `ResourceLocation`
is `Identifier` now — same package, same API, 1430 occurrences here — along with
`FastColor` to `ARGB`, `MetadataSectionSerializer` to `MetadataSectionType`,
`ToastComponent` to `ToastManager` and six package moves. None of it is hard;
all of it is everywhere.

The second reason is testing. A gun mod is rendering, animation, netcode and gun
packs of several thousand files. Only real players find those bugs. Shipping
1.21.11 first means bug reports arrive in volume and with a clean signal, instead
of every report being ambiguous between "the port broke it" and "26.1 is still
settling".

**Why 26.1 and not 26.2+.** 26.1 is the first fully unobfuscated Java release and
is where the modded ecosystem is currently consolidating. Later drops can wait
until they have an audience.

**Mappings.** The project already builds against Mojang mappings with Parchment,
so the move to unobfuscated 26.x costs almost nothing on that front. Parchment
itself has no release for 1.21.9, 1.21.11 or 26.1 — real class, method and field
names will be there, parameter names and javadoc will not. Inconvenient, not
blocking.

---

### 2. What this means for gun packs, worlds and addons

The part most people actually care about.

**Gun packs — no changes needed from pack authors.** Bedrock geometry, animations
and the pack's own JSON are a Bedrock-format specification and do not depend on
the Minecraft Java version. Of the 3322 files in the built-in pack, five mention
item NBT at all, and those are recipe results handled by the mod's own
deserializer, which will keep accepting the existing form. Nobody has to re-export
models or re-author packs for any target on this roadmap.

**Existing worlds — will be migrated automatically.** When item data storage
changes (see §5) it ships with a datafixer that reads both the old and the new
form. Guns already in chests and inventories keep their attachments, ammo and
settings.

**Java addon mods — will break once, with warning.** The public API in
`com.tacz.guns.api` is stable for now. When item storage changes, the old accessor
methods stay in place as `@Deprecated` delegates for at least one release before
anything is removed, so addons get a compile warning rather than a
`NoSuchMethodError`.

**Optional integrations may lag.** Every integration is already gated on the other
mod being installed and has a fallback, so none of them can block a release. Both that
needed a decision have had one: third-person animation moved from PlayerAnimator, which
stopped at 1.21.7, to Player Animation Library, and Accelerated Rendering was dropped
outright — see §3.

---

### 3. Dependency status

Verified against the published Fabric artifacts.

| Dependency | 1.21.11 | 26.1.2 |
|---|:---:|:---:|
| Fabric API | `0.141.6+1.21.11` | `0.155.2+26.1.2` |
| Cloth Config | yes | yes |
| Sodium | `0.8.13` | `0.9.1` |
| Iris | `1.10.7` | `1.11.2` |
| ImmediatelyFast | yes | — |
| Zoomify | yes | — |
| Cardinal Components API | yes | yes |
| Forge Config API Port | yes | yes |
| Architectury | yes | yes |
| JEI | yes | yes |
| REI | yes | yes |
| Mod Menu | yes | yes |
| Shoulder Surfing Reloaded | `5.0.7` — API break, see below | yes |
| Player Animation Library | `1.1.9+mc.1.21.11` | `1.2.5+mc.26.1` |
| Accelerated Rendering | no — dropped, see below | no |
| SimpleBedrockModel-Fabric | no — absorbed, see §4 | no — absorbed |
| Parchment | no release | no release |

The picture is identical for both targets: there is nothing available on 26.1 that
is missing on 1.21.11. Dependencies are not what makes this port hard.

**Sodium and Iris are the right choice and are already in use.** Upstream TACZ is a
Forge mod and integrates with Oculus and Embeddium, which are the Forge ports of Iris
and Sodium; on Fabric the originals are what exist, and both are current on every
target. Only Iris is integrated in code — four calls, now down to one on an internal
symbol.

**Accelerated Rendering was dropped.** It moved gun model vertex transforms onto the
GPU through compute shaders. The Fabric port stopped at 1.21.1 and is alpha
throughout, and the NeoForge original it was ported from stopped there too, so there
is nothing to wait for. It was also the most invasive integration in the client
renderer — a second, parallel set of render paths through the gun model, the
attachment model and the laser — sitting in exactly the code the 1.21.2 → 1.21.6
render rewrites force us to rework. Every call site was guarded and fell back to the
vanilla path, which is now the only path. If the mod is ported forward, the
integration comes back out of the history in one piece.

**Shoulder Surfing 5.x changes `IShoulderSurfingPlugin.register`** from taking a
registrar to taking an event bus, so the version bump has to adapt our plugin. We
build against 4.14.1 until then.

Development and compatibility dependencies are now pinned by readable version where a
Modrinth coordinate exists. Four remain on CurseForge file IDs, each with a comment
saying why: two dev-only test mods and MrCrayfish's Framework and Controllable have no
Modrinth listing, and Carry On publishes all three loaders under one Modrinth version
number, where the maven coordinate resolves to the NeoForge jar.

---

### 4. SimpleBedrockModel

**Done.** The library had no build for either target and was the last jar in `libs/`.
Rather than port a Bedrock model, molang and particle library the mod does not use, the
part it runs on was absorbed and the rest dropped — 8 new files against a 347-class jar.
See the [changelog](CHANGELOG.md) for what came across and what did not.

The reason it was not the peripheral dependency the earlier plan assumed:
`AnimateGeoItemRenderer` implements the library's `IFPGeoItemRenderer`, so the library
found our renderer and drove it. `FirstPersonRenderHandler` is what draws the player's
own hands.

Two things worth remembering from it:

- **`mae` was only ever present because the library bundled it.** It is declared as an
  ordinary dependency here with no `include`, so removing the library without removing
  the last use of `mae` would have been a `NoClassDefFoundError` on any real install.
  Worth checking the same for anything else declared without `include`.
- **The off-hand render pass is suspicious.** The handler cancels vanilla rendering for
  the off hand and then renders the gun anyway, so with something in the off hand the
  gun is drawn a second time against the off-hand pose. The mod's own first-person entry
  point, dead since the upstream NeoForge sync, returned at that point instead. Upstream
  behaviour is preserved for now; it needs one look in game.

---

### 5. Item data storage

There are currently no custom `DataComponentType`s in the mod. All gun, ammo and
attachment state lives in `minecraft:custom_data` as NBT, and installed attachments
are serialized `ItemStack` NBT nested inside the gun's custom data. This works, but
it means no client-side predicates, no tooltips or recipes driven by components, no
component-based commands, and no structural validation.

Migration to real component types is planned. It is **written after the 1.21.11 port
compiles and runs**, so that a bug is never ambiguous between the version change and
the storage change, but **shipped in the same release**, so players get one
disruptive update rather than two.

A world migration is needed either way, independently of the component work.
Installed attachments are stored as serialized `ItemStack` NBT nested inside the
gun's `custom_data`, and vanilla datafixers do not descend into `custom_data` — it
is opaque NBT by design. So nothing but this mod can upgrade those nested stacks
when a world moves from 1.21.1. Since that pass has to be written regardless,
the component migration rides along with it.

One exception is being fixed early: attachment tags are looked up by
`DataComponents.CUSTOM_DATA.toString()`, which relies on what `toString()` happens
to return rather than on any contract. That is broken today and independent of the
migration.

---

### 6. Phases

The guiding rule: **anything that can be done before the version bump is done
before it.** After a bump nothing compiles for a while, and it stops being possible
to tell a porting bug from a bug that was always there.

#### Phase 0 — cleanup and fixes on 1.21.1

This work lives on the `1.21.11` branch, which is this fork's default. It was branched
from `58a1c51` on the parent repository's `1.21.1` branch — the parent's default is
`1.20.1`, so a clone of *that* repository without `--branch 1.21.1` lands on the wrong
tree. The branch is named for where it is going, not for the version it currently
builds against: everything up to and including phase 1 still targets 1.21.1, and the
bump happens in phase 2.

- [x] **Security.** Sandbox gun pack scripts, range-check network slot indices,
      guard jar extraction. See the [changelog](CHANGELOG.md).
- [x] **Deletion.** Done in three passes: dead code and the vendored conventional tag
      data, the Forge item-handler layer, and the events Fabric API already provides.
      About 1500 lines of the compatibility shim and 185 000 lines of resources went;
      every one of them is a line that does not have to be ported later.
- [x] **Correctness.** Version-independent fixes — see the
      [changelog](CHANGELOG.md) for the list. Three crashes, the dimension change,
      the double `hurt()` per bullet, the physical-versus-logical side confusion, the
      `-1` sentinel, the swallowed first shot after a respawn, the pre-load config,
      loot injection into mob drops, the config key sitting on vanilla chat, the
      component key, per-file isolation during a reload and `/tacz reload` driving
      the client from the server thread. What is left in §7 is either robustness
      work that needs a running game to judge, or performance work that belongs
      with the version bump.
- [x] **Build hygiene.** `mavenLocal()` is gone, the publish repository comes from a
      property, the eleven Gradle deprecation warnings are fixed, and the vestigial
      Yarn mapping property is dropped.
- [x] **Infrastructure.** A GitHub Actions workflow builds every push and pull
      request, and there is a JUnit suite that `./gradlew build` runs. It starts
      with the Lua sandbox — those tests exist so that going back to
      `JsePlatform.standardGlobals()` fails the build rather than quietly
      reintroducing arbitrary code execution — plus the modifier evaluator and fire
      mode parsing. Still worth covering: the resource scanner, the JSON data
      managers, tag tree search, and the pack converter's rewrite table.

#### Phase 1 — clear the external blockers, still on 1.21.1

**Done.** Both items removed a dependency that does not exist on the target, while the
tree still compiled and a mistake was still distinguishable from a porting mistake.
Nothing on the dependency list now blocks the version bump.

- [x] Absorb SimpleBedrockModel — see §4.
- [x] Move third-person animation from PlayerAnimator to Player Animation Library.
      Done — see the [changelog](CHANGELOG.md). PAL covers 1.21.1 through 26.2, so this
      clears the way to both targets at once. Mostly a package rename; the loader got
      shorter because PAL returns the animations already keyed by name.

      The one thing that would have gone wrong silently: gun packs write
      PlayerAnimator's easing names (`INOUTSINE`), PAL spells the same curve
      `easeinoutsine`, and its lookup answers `LINEAR` for anything unrecognised without
      logging. Every keyframe in every pack would have gone linear with no error
      anywhere. `EasingNames` translates them, with the table derived from
      `EasingType.values()`, and the tests pin it. Worth asking PAL upstream for the
      aliases so nobody else has to find this the hard way.

#### Phase 2 — port to 1.21.11 and release

**The tree compiles and the jar builds** — `remapJar`, the access widener check and
all of it. That took **4756 distinct compile errors** down to zero, which is why
phases 0 and 1 emptied everything they could out of this one first.

Compiling is not running. Nothing here has been in front of a GPU, and the mixin
remapper has already named the places where it will not: see below.

- [x] **Toolchain.** Loom 1.17 — 1.17 split the plugin, and `fabric-loom-remap` is
      the one that keeps remapping to intermediary — Gradle 9.5.1, loader 0.19.3,
      Fabric API 0.141.6, and every dependency on its 1.21.11 build. Parchment is
      dropped for having no release here. The dev-only convenience mods are parked
      for the duration: nothing references them, and while the tree is red they only
      make "what did I break" harder to answer.
- [x] **The renaming pass.** See §1. Derived from a diff of the two remapped jars
      rather than written by hand, so the moves are what the jar says they are.
- [x] **NBT.** Every getter returns an `Optional` and `contains` lost its type
      argument. The one place that keeps the old semantics by hand is
      `LuaNbtAccessor.contains(key, type)`, because gun pack scripts call it.
- [x] **Input and keybinds.** Keybinds take a registered `KeyMapping.Category`;
      the label follows the id, so the language files moved from `key.category.tacz`
      to `key.category.tacz.guns`. Keyboard and mouse input are records now, and the
      event shim carries them.
- [x] **Forge Config API Port** — `neoforge.v4` became `v5`, same methods.
- [x] **Block entities** on `ValueInput`/`ValueOutput`.
- [x] **Blocks and block entities.** `updateShape` reordered and takes a
      `ScheduledTickAccess`, `onRemove` became `affectNeighborsAfterRemoval`,
      `RenderShape.ENTITYBLOCK_ANIMATED` is `INVISIBLE`, and `BlockEntityType`'s
      constructor is closed, so the types go through Fabric's builder.
- [x] **The explosion split.** `Explosion` is an interface and `ServerExplosion`
      does the work behind one `explode()`, so `ProjectileExplosion` overrides that
      whole method. What it exists for is intact: damage still falls off by
      ray-tracing fifteen points on the lag-compensated hitbox.
- [x] **Minecarts** — and the `IMinecart` shim and its two mixins are gone with
      them, because `isRideable()` is an overridable method now.
- [x] **Recipes, ingredients, items, reload listeners, Cardinal Components.**
- [x] **Client rendering.** Done, in the sense that it compiles. Three of the four
      pieces were redesigns rather than renames:
      - **Rendering is submit-based now.** Renderers no longer draw; they fill a
        render state and hand geometry to a `SubmitNodeCollector`. The block
        entities and the two entity renderers are done, and `BedrockModel` — what
        actually draws every gun, attachment and block in this mod — has the
        collector-based path next to the immediate one the rest of the tree still
        uses. What is left is the item renderers, which are tangled with the item
        model rework below.

        Two rules came out of that work and apply to everything still to convert:
        the entity or block entity may only be read during extraction, and the
        model instance is shared, so posing it has to happen inside the deferred
        geometry callback — posing before submitting means the second gun in view
        overwrites the first one's animation and both draw the same.
      - **GPU state moved into `RenderPipeline`.** `GlStateManager` is gone and
        `RenderSystem` no longer has `enableBlend`, `depthMask`, `stencilFunc` or
        any of it. Colour and depth masking, the depth test and blending are now
        properties of a pipeline, reapplied on every `setPipeline`, so they cannot
        be flipped around a draw — a state change means a second `RenderType`.
        `RenderStateShard` and `CompositeState` are gone the same way; a
        `RenderType` is a pipeline plus a texture and a handful of leftovers
        (`RenderSetup`).

        The scope stencil mask — §8's first risk — is done and described below.
        So are the laser beam, the bullet hole particle, the tooltips and the HUD.
      - **Item rendering** without `BuiltinItemRendererRegistry` or
        `BlockEntityWithoutLevelRenderer`. Done. An item's appearance comes from
        `assets/<ns>/items/<id>.json` now, and custom rendering is declared as
        `minecraft:special` naming a registered `SpecialModelRenderer`. There is
        one such type, `tacz:dynamic`, which draws nothing and forwards to
        whatever renderer the item names — the same indirection as the old
        registry, moved into the model JSON. The registry's other job, looking a
        renderer up from an item, never needed a registry and now goes through
        `IDynamicItemRenderer.of(item)`.

        `BedrockGunModel` and `BedrockAttachmentModel` each grew a collector
        overload for the non-first-person case. The stencil work stays immediate:
        it needs its draws in order, which a collector cannot promise, and outside
        first person there is nothing in the stencil buffer to test against anyway.
      - **The GUI** is two-dimensional now — `GuiGraphics.pose()` is a
        `Matrix3x2fStack`. Anything that wanted a 3D transform has to become a
        picture-in-picture element: content drawn to an offscreen texture, then
        blitted. The gun smith table's rotating preview is done that way, via
        Fabric's `SpecialGuiElementRegistry`.

        Colour is no longer ambient state either: `setShaderColor` and `setColor`
        are gone, and tints are an argument to `blit`.
      - 2D `GuiGraphics`, HUD on `HudElementRegistry`, and the widgets — the only
        genuinely mechanical part of the four.
- [ ] **Mixins.** The keyboard and mouse handlers are retargeted onto the record
      forms. What is left is the set the remapper reports on every build — a free,
      exact inventory, because a target it cannot find is a target that moved:

      | Mixin target | What happened |
      |---|---|
      | `Minecraft.timer` | renamed |
      | `ItemInHandLayer.render(…, LivingEntity, …)` | `submit(…, S, …)`, takes a render state |
      | `ItemInHandLayer.renderArmWithItem(…)` ×2 | `submitArmWithItem(…)` |
      | `HumanoidModel.setupAnim(LivingEntity, …)` | `setupAnim(S)` |
      | `PlayerModel.setupAnim(LivingEntity, …)` | `setupAnim(S)` |
      | `LivingEntityRenderer.render(LivingEntity, …)` | render state |
      | `AbstractButton.onClick(DD)` | `onClick(MouseButtonEvent, boolean)` |
      | `RecipeManager.apply(Map, ResourceManager, ProfilerFiller)` | reload contract changed |

      Five of those are the same problem: the renderer, the layer and the model all
      work from a render state now and cannot see the entity. The reads behind them
      — is the main hand holding a gun, what is in the offhand and the hotbar — have
      to move into extraction and ride along in the state. That is the last piece of
      the extract-and-submit conversion, and the only one that could not be done by
      following compile errors.

**The scope stencil mask.** Nothing in vanilla touches the stencil buffer any
more — not `RenderSystem`, not `RenderPipeline` — so it had to be rebuilt from
three separate pieces rather than ported call for call.

- *The buffer.* `RenderTarget`'s depth attachment is a `GpuTexture` in `DEPTH32`,
  with no stencil bits, and the FBO is assembled inside the GL backend where the
  old mixin used to reach. `RenderTargetMixin` marks the depth texture when a
  scope asks for stencil, `GlDeviceMixin` allocates it as `DEPTH32F_STENCIL8`
  instead — same depth precision, eight bits more — and `GlTextureMixin` hangs it
  on `GL_STENCIL_ATTACHMENT` when the FBO is built.
- *The state.* Because vanilla never touches stencil, raw GL survives untouched
  between passes. `StencilSupport` wraps it. The clear is the exception: between
  render passes the bound draw framebuffer is the window's, so a `glClear` there
  wipes the wrong thing. It runs inside an empty render pass on the main target,
  which is the cheapest way to get the right FBO bound.
- *The masking.* Since colour and depth writes are pipeline properties, the
  stencil-only pass and the reticle pass each get their own `RenderType` with the
  state baked in. That is also why `BedrockAttachmentModel.render` now takes the
  texture: a `RenderType` cannot be asked what it draws with, so the variants have
  to be keyed on the identifier from the call site.

It has now been measured in game (llvmpipe, 854×480) rather than guessed at from
a screenshot, and it works: aiming an ACOG shows the world through the lens with
the reticle over it.

Getting there took three wrong answers, all of the same kind — reading a cause
off a picture. Looking at a scope produced a black rectangle with a round hole in
it, which looks exactly like a mask that landed in the wrong place. It was not
that. `-Dtacz.scopeDebug=true` said:

    fbo=3 complete=true stencilAttachment=0 stencilBits=0 stencilTest=true

No stencil attachment at all, so every test passed and every write was dropped
and the ocular mask drew unclipped over the screen. The "hole" was never cut by
anything — at rest the same trace reports `radius=0.0`, so that pass draws
nothing. It was the transparent lens area of the ocular's own texture.

The texture was created and it was attached; it landed on a framebuffer nobody
was drawing to. Two more wrong guesses followed — that `_glBindFramebuffer` was
skipping a bind it thought redundant, and that the encoder binds lazily so the
probe was reading someone else's framebuffer. Neither moved the reading.

The actual cause: **`GlTexture` and `GlTextureView` each carry their own
`getFbo` / `createFbo` / `fboCache`, and the render pass uses the view's.**
`GlCommandEncoder.createRenderPass` goes through `GlTextureView.getFbo`;
`GlTexture.getFbo` only serves blits and copies. The hook was on `GlTexture`, so
the stencil was being attached to a framebuffer that never got drawn to, every
single time.

There is a second trap next to it, which is why the hook is on `getFbo` and not
`createFbo`: the cache is keyed by the depth texture's *GL id*, and GL recycles
ids. When the depth texture is rebuilt as `DEPTH32F_STENCIL8` the new texture
usually gets the same id, so `getFbo` returns the framebuffer it already had and
`createFbo` never runs again. Vanilla does not care — a framebuffer attachment
refers to the texture id, so it silently points at the new texture — but the
stencil face would never be attached. Hooking the lookup instead of the creation
covers both, and `StencilSupport` remembers which framebuffers it has already
patched so it is one set lookup per pass.

**What running the client actually found.** The remapper's inventory above only
covers targets whose *name* moved. Six more mixins compiled and remapped clean
and still failed, because what moved was the injection point or the callback
type, and Mixin only finds that out when it applies — which for a screen class is
whenever that screen first opens, not at startup. Loading every `@Mixin` target
in one go turns that into a single run instead of one launch per screen, and it
is worth doing again after the 26.1 bump.

| Mixin | What moved |
|---|---|
| `Minecraft.pickBlock` | rewritten around `MultiPlayerGameMode.handlePickItemFrom*`; the `Abilities.instabuild` read the injection hung on is gone |
| `Minecraft.startUseItem` | `InteractionResult.shouldSwing()` → `Success.swingSource() == CLIENT` |
| `SoundEngine.play` | returns `PlayResult`, so the callback is a `CallbackInfoReturnable` |
| `ItemInHandRenderer.renderHandsWithItems` / `renderArmWithItem` | take a `SubmitNodeCollector`, not a `MultiBufferSource` |
| `GameRenderer.getFov` | returns `float`, not `double` |
| `CreateWorldScreen.openFresh` | only forwards now; the `PackRepository` is built in `openCreateWorldScreen` |

That last one was worth more than it looks: `SelectWorldScreen` links against
`CreateWorldScreen`, so a mixin that fails there takes the world list down with
it — the singleplayer screen renders as an empty panel and `--quickPlaySingleplayer`
hangs on "Loading Minecraft" with nothing in the log.

Verified in game on 1.21.11: the title screen and world list, loading a world,
the first-person gun with the player's arm, the third-person gun on the player
model, the HUD including the ammo counter, the creative inventory and REI
alongside it, item tooltips, chat and commands, iron-sight aiming, firing (the
counter decrements and the target dies), the gun smith table block with its full
model, and that table's screen — recipe list and tooltips.

The table's rotating preview is the one place where "it renders, it is just small
and dark" turned out to be wrong on a second look. Cropped out of the screenshot
and brightened, the gun is upside down and pushed most of the way below the
panel. `PictureInPictureRenderer.prepare` hands over `scale(s, s, -s)` — Y is not
flipped — while the offscreen target's projection is Y-down like the GUI and the
model is Y-up; 1.21.1 wrote `scale(1, -1, 1)` on the modelview for exactly this.
That flip is back. Confirming it on screen is still outstanding.

The muzzle flash is not broken either, and the way that was established is worth
recording because the same trap is waiting for every short-lived effect in this
mod. It never appears in game here, and the shot plainly registers, which reads
like a defect. The trace says the chain is whole — `GunFireEvent` arrives on the
client, `onShoot` runs, the renderer is reached with `isSelf` true — and then
prints the thing that actually matters:

    muzzle flash: 86 ms since shot, window is 50 ms

The flash lasts 50 ms. Under llvmpipe a frame takes about 80 ms, so the first
frame after a shot is already past the window and no frame ever lands inside it.
Widening the window with `-Dtacz.muzzleFlashMs=1500` (only honoured when the
diagnostic is on) puts the flame on screen at the muzzle. At 60 fps two or three
frames fall inside 50 ms and it would be visible normally.

So "effect not seen under software rendering" is not evidence of anything. Shell
casings are on the same short timer and should be treated the same way.

Bullet holes turned out not to be broken either, for a duller reason: the run
that reported a clean wall was firing a gun with an empty magazine, so nothing
was ever shot. With a loaded one the decal is there — one particle created, a
valid sprite off the block atlas, extracted every frame for its whole life, and
a small dark square on the stone where the round landed. The quad offset that
lifts it off the surface is `0.01 * quadSize`, the same as 1.21.1; it was worth
checking, because at 0.0005 blocks it is close enough to the face to be a
plausible z-fighting suspect, but it matches upstream and it draws.

- [ ] Resources: blockstate format, recipe ingredient form. The item definition
      JSON is written for the items that have models; what is left is `ammo_box`,
      which needs two things at once — its model `overrides` become a
      `minecraft:select` over a registered item model property, and its colour
      provider becomes an `ItemTintSource`, because item tinting is part of the
      model now. Both are marked in `ClientSetupEvent`.
- [x] **Recipes on the client.** Since 1.21.2 the client is not sent recipe data
      at all — `RecipeAccess` carries only what the recipe book needs. Fabric's
      `RecipeSynchronization` sends them back for one serializer, and
      `ClientRecipes` wraps the lookup. A `RecipeHolder` is keyed by
      `ResourceKey<Recipe<?>>` now; the craft packet still carries a plain
      `Identifier` and the server rebuilds the key, so the protocol is unchanged.
- [x] **The integrations.** JEI's subtype and category interfaces, REI's
      `Display.getSerializer`, Shoulder Surfing's 5.x event bus and Framework's
      renamed events were all real API moves and are ported.

      Two were deleted instead, and for the same reason: they existed to work around
      a mod doing its own draw batching, and both mods stopped, because vanilla's
      submit pipeline does that now. ImmediatelyFast dropped `hud_batching` and its
      whole public api package. Iris dropped `batchedentityrendering` — which takes
      §8's standing liability with it, the one integration point with no `api.v0`
      equivalent that had to be re-checked on every Iris release.

Three behaviour changes went in rather than being deferred, because vanilla made
them and there was no way to keep the old shape:

- The target block and the target minecart no longer resolve their owner's
  profile themselves. Skulls stopped doing that too — the block entity holds an
  unresolved `ResolvableProfile` and the client's skin cache resolves it at render
  time. `TargetRenderer` still asks the skin manager directly and needs the same
  treatment when the client renderers are ported, or the target will show a
  default skin.
- `Item.verifyComponentsAfterLoad` is gone with no replacement, and two things
  hung on it: per-ammo-type stack sizes, which come from the gun pack and so can
  only be set per stack, and the 1.20.1 attachment-id rewrite. Both sit on
  `inventoryTick` behind a guard for now. They belong in phase 3's datafixer.
- Cardinal Components followed vanilla onto `ValueInput`/`ValueOutput`, which only
  speak codecs, while the synced entity data is written by per-type serializers
  that hand back raw tags. The list is still built the same way but goes in
  wrapped, so the shape in the save file gains one level.

#### Phase 3 — item data storage, shipped with the 1.21.11 release

- [ ] See §5. Component types, world migration, deprecated accessor facade for addons.
      Written after phase 2 runs, released together with it.

#### Phase 4 — port to 26.1

- [ ] Unobfuscated toolchain, no Parchment.
- [ ] Whatever API churn the 26.1 drop brings.
- [ ] Prototype the riskiest rendering work early rather than at the end.

##### Another port of the same ancestor already exists on 26.1.2 / 26.2

[q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
forks the same `Sh1roCu/TACZ-Refabricated` 1.21.1 Fabric branch this tree does,
and went straight to 26.1.2 and 26.2 without the intermediate step. Beta 1, GPL-3.0
like us, so borrowing from it is legal with attribution. Read from the GitHub web
pages only — it cannot be cloned into this session, so what follows is from its
README, file listing and one file, not from building it.

Worth knowing before phase 4 starts:

- **Its scope is an offscreen mask texture, not the stencil buffer.** A whole
  `client/render/scope` package: `ScopeMaskTarget`, `ScopeMaskGeometry`,
  `ScopeMaskTextureHandle`, plus separate etched and illuminated reticle renderers.
  The ocular geometry is drawn to a dedicated `TextureTarget` as a black-and-white
  mask, and the mask is sampled where we test the stencil. That sidesteps the whole
  problem §7 records about scopes under Iris — a deferred shader pipeline owns the
  framebuffer, and a stencil attachment we hang off vanilla's depth texture is not
  guaranteed to survive it. It is the better design for 26.x and should be
  considered before porting `StencilSupport` forward.
- **It gave up on picture-in-picture**: "secondary world rendering (PIP) remains
  disabled due to deep 26.2 rendering/RenderTarget coupling". That is the gun smith
  table preview, which works in this tree. So this is not a strictly better port to
  switch to — it is ahead on the scope and behind on the preview.
- **Its history is not usable as a source of patches.** Roughly 40 commits in two
  days, a large share of them literally "Add files via upload" — whole files pushed
  through the web UI, with build logs and gun pack archives committed and then
  deleted again. There are no reviewable diffs to cherry-pick; anything taken from
  it has to be read and re-derived.

The honest read: keep this tree, and treat that repo as a second opinion on the
26.x rendering questions rather than as a base or an upstream.

---

### 7. Known work items

Issues found in a full review of the codebase that are worth fixing regardless of
the target version. Most predate this fork.

Some of these are parity regressions — behaviour the Fabric port lost relative to the
Forge original — found by comparing the two line by line rather than by reading this
tree in isolation.

What follows is what is still open. Everything already fixed is in the
[changelog](CHANGELOG.md).

**Lost relative to the Forge original**

- The handshake no longer checks a channel version. A client running a different
  version of the mod connects and desynchronises mid-game instead of being refused.
- The client asset cache is cleared on disconnect rather than on join, and the handler
  returns early on a memory connection — so moving from singleplayer to a dedicated
  server clears nothing.
- All eleven keybinds lost `KeyConflictContext.IN_GAME`. In practice every handler
  checks `isInGame()` itself, so the behaviour is right; what is lost is that the
  controls screen still counts them as conflicting with any vanilla key on the same
  code.

**Incorrect behaviour**

- Worlds coming from 1.20.1 lose every installed attachment. They were written as
  `{id, Count, tag}` and are now read with `ItemStack.CODEC`, which does not fail on
  the old shape — it just yields an empty stack. Guns and ammunition survive; scopes,
  silencers and grips vanish. The existing `AttachmentIdFix` only renames ids.
- The headshot marker sets a shader colour that three early returns skip resetting, so
  the HUD stays tinted red for up to 300 ms.
- The legacy gun pack hint never appears in multiplayer. It is a client-side message
  hooked to a server-side join, so it only fires on an integrated server.
- Lua script writes to item NBT are silently dropped despite the API documenting
  them as persistent.
- Glass breaking, ignition and bell ringing bypass region protection and the
  `mobGriefing` rule.
- On the off-hand render pass the first-person handler cancels vanilla rendering and
  then draws the gun anyway, so with something in the off hand the gun is drawn a second
  time against the off-hand pose. Upstream behaviour, preserved on absorbing the handler
  rather than changed blind — see §4.
- The velocity stored in the hitbox history is the movement of two ticks, not one.
  `HitboxHelper.onPlayerTick` records the velocity before trimming the position
  history, so the sample it takes spans three positions. The hitbox offsets around it
  are described upstream as experimentally derived, and they were derived against
  this — correcting it changes hit registration, so it wants a playtest rather than a
  patch. The value read directly by the movement inaccuracy check is over one tick
  and is correct.

**Robustness**

- The shared assets holder is swapped from a background thread before the reload
  succeeds, and is not `volatile`.
- One data manager is only half synchronized — the fast path reads an unguarded
  `HashMap`.
- Client firing logic reads item components off the main thread.
- No spectator check on the server firing path.
- A static handoff field leaks the reloadable server resources permanently.
- The loot table id cache is a static strong-reference map that is never cleared, and
  it re-scans the registry on every roll for a table it failed to resolve.

**The gun pack sync packet**

Every pack's JSON goes to the client as one payload, with no chunking and no
compression. On 1.20.1 that hit the 1 MiB custom payload limit outright. On 1.21.1 a
registered typed payload goes through its own `StreamCodec` and skips that check, so
the ceiling moves to the frame decoder — roughly 2 MiB in, 8 MiB out — and the failure
now surfaces as a connection frame error rather than a clear size exception. Either
way a server with a few large community packs breaks on every player join. The fix is
a begin/part/end protocol over batches plus gzip, which pack JSON compresses very well.

**Performance and polish**

- Every accessor read deep-copies the gun's entire NBT, including its serialized
  attachments. This runs several times per frame from the HUD, the gun model and the
  animation state machine.
- O(N²) JSON parsing when a player joins.
- Mod container lookup and string formatting every frame in the HUD overlay, and an
  NBT write from the render thread.
- 18 `printStackTrace()` calls instead of logging, four of them in animation
  interpolators where they spam per frame.
- An interpolator selectable from animation JSON has three empty method bodies.
- Off-by-one in backup retention, and temporary files left behind on one path.

---

#### From the first play test on real hardware

Two reports from someone running the built jar at a normal frame rate. Both are
in the class this environment cannot see — llvmpipe gives about 12 fps here. One
is found and fixed; the other is still open, and both of the leads written down
for it turned out to be wrong.

- **The first-person animation ran too fast when walking. Found, fixed.**
  Not a frame-rate problem at all, and not in `tickAnimation` — the `tick(float)`
  it calls has an empty body. Animation progress runs on `System.nanoTime`
  throughout and never depended on frame rate.

  The walk and run animations do not play at their own speed: the lua sets their
  progress from distance travelled, `getWalkDist() % 2.0 / 2.0`, so that the gun
  bobs in step with the footsteps. `GunAnimationStateContext.getWalkDist` was
  ported onto `walkAnimation.position()`, which is not the same quantity as the
  `Entity.walkDist` it replaced. Both were read out of the bytecode:

  | | per tick |
  |---|---|
  | 1.21.1 `Entity.walkDist` | `horizontalDistance * 0.6` |
  | 1.21.11 `WalkAnimationState.position` | `min(horizontalDistance * 4, 1)` |

  6.7x too fast at walking speed, and clamped — so it also drifts out of step
  with the footstep sounds when sprinting. Now on `Entity.moveDist`, which off a
  climbable block adds exactly `horizontalDistance * 0.6` and is the same counter
  vanilla triggers step sounds from. There is no `moveDistO`, so the previous
  tick's value is reconstructed from `xo`/`zo` to keep the sub-tick interpolation
  the animation needs at high frame rates.

- **The scope reticle thins out while the breathing animation plays. Open.**
  Both leads recorded here earlier are now disproven, by comparing the ported
  pipelines against vanilla's in the bytecode rather than reasoning about them:

  - Vanilla `ENTITY_CUTOUT` is `ENTITY_SNIPPET` + `ALPHA_CUTOUT 0.1` + `Sampler1`.
    `ScopeRenderTypes.NO_DEPTH_TEST_PIPELINE` is the same three plus the depth
    state, so the cutout threshold is not a divergence. The extra `depthWrite`
    it turns off is a real difference from 1.21.1, but it can only affect what
    is drawn *after* the reticle, not the reticle's own coverage.
  - Mipmapping is not a divergence either: nothing in the mod registers textures
    itself, so pack textures load through vanilla exactly as they did before.
  - And that pipeline is only used by `renderDivisionOnly`, the red-dot path. A
    scope with an eyepiece goes through `renderOcularAndDivision`, which draws
    the reticle with the caller's plain `RenderTypes.entityCutout` — the same
    call 1.21.1 made.

  Two answers since, from the player: the scope is a Mark 5, so it is the
  eyepiece path — `renderScope` → `renderOcularAndDivision`, which draws the
  reticle with the caller's plain `entityCutout`, the same call 1.21.1 made. And
  the reticle "floated as a whole", not thinned at the rim.

  That last one moves the suspicion off the stencil circle. A mask clipping the
  reticle would eat its edges; a reticle that drifts bodily is the reticle's own
  geometry moving against the eyepiece it is supposed to sit in. The division node
  and the ocular node are separate bones, and the breathing animation drives them
  through `translateAndRotateAndScale` — so the question is whether both are still
  reached by the same transform chain, or whether one of them now picks up a pose
  the other does not. `renderTempPart` walks the path itself and flushes per part;
  compare the pose the division node ends up with against the ocular's.

#### Diagnostics and dev affordances that exist now

- `-Dtacz.renderDebug=true` — `RenderDebug`, off and free otherwise. Framebuffer
  and stencil attachment state, scope circle geometry, one-shot notes.
- `-Dtacz.muzzleFlashMs=<ms>` — widens the 50 ms muzzle flash window, honoured
  only with the diagnostic on. Needed because a frame here is longer than the
  window; see the muzzle flash note above.
- `-PquickPlay=<save>` on `runClient` — straight into a save, no menu.
- `-PclientProps=a=1,b=2` — passes system properties to the client JVM.

#### Findings from the five-way audit

Five parallel auditors over pack loading and Lua, networking and the trust
boundary, client rendering, gameplay logic, and threading. Everything below marked
**verified** was re-read in this tree by hand afterwards; everything marked
*reported* is the auditor's, plausible, and still needs that second read. One
finding was checked and **rejected** — see the end.

**Security**

- **verified — a gun pack can execute arbitrary code.** The port already replaced
  `JsePlatform.standardGlobals()` with `LuaSandbox` (ae06a32), which drops `io`,
  `os`, `luajava` and `debug`. It is not enough. Every entry point hands the script
  a live Java object through `CoerceJavaToLua.coerce(...)` — about twenty sites in
  `LuaAnimationState`, `LuaStateMachineFactory`, `ModernKineticGunItem`. In the
  bundled luaj (`FiguraMC 3.0.8-figura`), `JavaClass` builds its method map from
  `Class.getMethods()` filtered only on `Modifier.isPublic`, so the inherited
  `getClass()` is exposed; `CoerceJavaToLua` maps a `Class` to a `JavaClass`, whose
  members are then those of `java.lang.Class`. Reflection reaches the whole JVM
  from there, and the removed libraries are not on that path. Packs run on the
  client and on the server. The fix is to stop passing raw coerced objects: an
  explicit wrapper exposing only whitelisted methods, never returning a bare
  `JavaInstance`. That can break third-party packs that use reflection today.
- **verified — no execution budget on pack scripts.** No `DebugLib` means no hook
  mechanism, so `while true do end` in a pack hangs the client or the server tick
  with no recovery. Cheap fix: install `DebugLib`, immediately clear the `debug`
  global, keep `setHook`, abort after N instructions.
- **verified — no rate limit on any of the 15 C2S packets.** Three do real work
  ungated. `ClientMessagePlayerFireSelect` is a `StreamCodec.unit` — about 20 bytes
  — and each one writes item NBT, broadcasts a full serialised `ItemStack` to every
  player tracking the shooter, and re-evaluates the pack's Lua attachment
  modifiers. Draw and zoom are the same shape.
- **verified — every hit and kill goes to the whole dimension.**
  `EntityKineticBullet` sends through `sendToDimension` (`PlayerLookup.world`), and
  the payload carries victim id, shooter id, damage and the headshot flag. Clients
  out of tracking range cannot use it; a hostile one gets a live feed of every
  engagement. One packet per player per pellet.
- *reported* — the legacy pack converter streams entries with no cap on
  decompressed size. Admin-initiated on a local file, so low.

What the auditors checked and found sound, which is worth recording: hits are
simulated server-side and the client supplies no positions, damage or results; lag
compensation uses the server-measured ping, clamped by config; slot indices are
range-checked; melee checks distance, cone and line of sight; commands need
permission level 2.

**Correctness**

- **verified, port regression — the collector path replays the previous
  submission's delegates.** `BedrockModel.render(..., collector, ...)` snapshots
  `delegateRenderers` and swaps in a fresh list *before* `submitCustomGeometry`,
  but the list is filled from inside the deferred callback (`part.render` →
  `FunctionalBedrockPart` → `AttachmentRender.delegateRender`). So the loop draws
  what the last submission produced, and nothing at all on the first. Worse, one
  `BedrockGunModel` is shared per gun id, so with two players holding the same gun
  each replays the other's delegates and their captured matrices — third-person
  attachments at the wrong entity. The immediate path is correct; only the
  collector path, which is new for 1.21.9, is wrong.
- **verified — the gun smith table under-charges.** `GunSmithTableMenu.doCraft`
  declares `recordCount` outside the per-ingredient loop and keys it by slot index,
  so a second ingredient matching the same stack overwrites the first reservation
  instead of adding to it. Ten iron plus one by tag deducts one. No shipped recipe
  triggers it; packs can.
- **verified — `(ScheduledFuture<?>) Thread.currentThread()`** at
  `LocalPlayerShoot.java:272` and `:279`. `Thread` implements no such interface, so
  every shot throws `ClassCastException`. It behaves correctly only because
  `ScheduledThreadPoolExecutor` drops a periodic task that throws — which is what
  the unreachable `cancel(false)` was for.
- **verified — `AbstractGunItem.getRPM`** does `rpm *= (int) iGun.lerpRPM(gun)`.
  The cast binds to `lerpRPM`, which returns a fractional multiplier, so any pack
  setting it below 1.0 gets zero. The sibling in `GunData.getShootInterval` was
  already fixed; this one was missed. Public API, used by addons.
- **verified — unloading a magazine loses rounds.** `AbstractGunItem` computes
  `roundCount = ammoCount / (stackSize + 1)` and loops `i <= roundCount`, where the
  answer is `ceil(n / stackSize)`, then zeroes the magazine regardless. With a
  64-round stack a 193-round drum returns 192. It can only lose, never duplicate.
- **verified — handlers registered on both tick phases with no discriminator.**
  `ServerTickEvent::onServerTick` on start and end, and
  `TickAnimationEvent::tickAnimation(Minecraft)` likewise, so the animation state
  machine gets its movement input twice per tick. The sibling overload does check
  the phase, which is what makes this an oversight — it is the Forge
  `TickEvent.phase` idiom lost in the port.
- **verified, and inherited, not ours** — `SecondOrderDynamics` is byte-identical
  to 1.21.1. Each instance submits an endless `while (!stop)` loop with a 6 ms sleep
  to a static 15-thread non-daemon pool; `stop()` has no callers. Six instances
  exist, so six threads spin for the life of the JVM, in the main menu too. `py`,
  `pyd`, `px`, `target` and `stop` are plain fields written by both that worker and
  the render thread. The loop also advances a fixed `t = 0.05` every ~6 ms, i.e.
  about 8x real time — presumably how the constants were tuned, but it means the
  smoothing is not in seconds. Note this is *not* the cause of the play-test reports
  above: 1.21.1 behaves the same.
- **verified on a second read, all fixed** — respawn NPE with
  `AutoReloadWhenRespawn` on and a gun whose pack is gone; `hide_tooltip_part`
  writing one key and reading another, with mask 1 blanking the whole tooltip;
  `heat.max` unvalidated, zero bricking the gun after one shot; the heat bar never
  showing until the first shot because `GunItemBuilder.build` dropped
  `setHeatData`; `CycleTaskHelper` delays expiring quadratically early and then
  burning the whole cycle budget at once; the ammo box reporting success on an
  insert that moved nothing, and lockable to an id from an unloaded pack.
- **verified on a second read, all fixed** — threading: `ScriptManager.scriptMap`
  was a plain `HashMap` cleared on the main thread during a reload while the
  asset-preload thread read it; `SoundConsumerStorage` leaked a consumer whenever a
  channel was released before its play task ran; the handshake handler parked a
  netty loop on a `CountDownLatch` waiting for an acknowledgement the server never
  waits for (configuration handlers, unlike play handlers, do run on netty);
  `join()` on the render thread against a single-threaded asset executor waited on
  everything queued ahead of the task rather than on the task.

**Performance** — all verified by reading the code, none measured in a profiler.

- `BedrockCubeBox.compile` allocates a `Vector3f` per polygon and a `Vector4f` per
  vertex, and recomputes `ARGB.colorFromFloat` per vertex for a value constant
  across the call. Vanilla's equivalent allocates one `Vector3f` per cube and
  transforms into it. The stock m4a1 is ~1000 cubes, so this is tens of thousands
  of objects per gun per frame.
- `BedrockPart.translateAndRotateAndScale` allocates three `Quaternionf` per bone
  per frame where vanilla uses one `rotationZYX`, and applies `mulPose` and `scale`
  unconditionally — a full rotate and scale against identity for every static bone.
- `StencilSupport.clear()` runs at the end of every first-person gun render even
  with no optic fitted: an extra render pass and a full-target clear per frame.
- `endBatch()` with no argument in `LeftHandRender`, `RightHandRender` and
  `TextShowRender` drains every buffered render type, not just theirs.
- *reported* — `HumanoidOffhandRender` runs a complete extra gun render per
  back-slung gun per visible player per frame; the animation interpolators allocate
  a `float[]` per channel per frame; `PapiManager` evaluates every placeholder
  whether the string uses it or not.

**What is still open, and why each one needs a decision rather than a patch**

Everything else from this audit is fixed — see the changelog and the two commits
that followed it. What is left is left on purpose:

- **A real Lua sandbox.** Closing the reflective path means handing scripts an
  explicit wrapper that exposes only whitelisted methods and never returns a bare
  `JavaInstance`. The set of classes is bounded and the work is not large, but it
  will break any third-party pack that reaches through a coerced object today, and
  there is no way to tell from here how many do. The execution budget is already in,
  so the remaining exposure is code execution by a pack the player chose to install,
  not a hang.
- **Rate limiting the C2S packets.** Fire-select, draw and zoom each do real work
  per packet with no cooldown. A limiter is easy; picking the numbers is not, because
  too tight a limit eats legitimate rapid input and the right value depends on how
  the server is played.
- **Narrowing the hit broadcast.** `sendToDimension` to everyone could become
  tracking-players-only. The handler already ignores what it cannot resolve, so the
  change looks free — but "looks free" is what this session has been wrong about
  before, and it is a networking change with no way to test it here.
- **The three no-argument `endBatch()` calls.** Draining every buffer is wasteful,
  but which layer the arms and the text land in depends on it. That is a visual
  change and this environment cannot judge visual changes.
- **`SecondOrderDynamics`.** Six threads spinning for the life of the JVM and a
  cross-thread race on plain floats. It is byte-identical to 1.21.1, so fixing it is
  a behaviour change to inherited code — the fixed `t = 0.05` per 6 ms iteration is
  presumably how the constants were tuned, and a correct fixed-step integrator would
  feel different.
- **Gating the per-entity `RenderLivingEvent` allocation.** Fabric's `Event` has no
  `hasListeners()`, and every alternative copies the listener's own checks into the
  mixin. Small cost, no clean fix.

**Rejected**

`ServerMessageSwapItem.handle` was reported as racing the client thread for want of
`context.client().execute(...)`. It does not. Fabric's own javadoc for
`ClientPlayNetworking.PlayPayloadHandler.receive` says it "is called on the render
thread, and can safely call client methods" — the API marshals before invoking. The
`execute(...)` in our other play handlers is redundant. Configuration handlers are
the exception and genuinely do run on netty, which is why the handshake item above
stands.

#### A caution that cost most of a day

Four rendering "defects" were called from screenshots this session. One was real.
The scope mask was misread three times in a row from the same picture before a
three-point state dump found the actual cause; the muzzle flash and the bullet
holes were not broken at all; the table preview was called blank, then called
fine, and was in fact upside down — which only became visible after cropping the
panel out and raising the brightness. At 854x480 under software rendering, the
absence of an effect is close to no evidence, and small detail is unreadable by
eye. Dump state, or crop and magnify. Do not name a cause from a screenshot.

### 8. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| The scope stencil mask cannot be reproduced — the new GPU abstraction has no stencil concept | **resolved** — rebuilt and confirmed in game | high; changes how every optical scope looks | aiming an ACOG now shows the world through the lens with the reticle over it, and the trace reports `stencilAttachment=1 stencilBits=8` on the framebuffer being drawn to |
| Every renderer has to move to extract-and-submit | **done except the mixins** | high; it is most of the client port | the five render-state mixins are the tail — see phase 2 |
| `SpecialModelRenderer` does not cover what the mod needs from item rendering | **resolved** — one `tacz:dynamic` type forwards to the mod's own renderers | high | — |
| No replacement for the current camera/FOV discriminator | medium | medium; scope zoom and gun model FOV depend on it | find a new discriminator during the client port |
| No Parchment | high | low | accept |
| Addons break at the storage migration | high | medium | deprecated delegating facade for a release |
| Two live branches to maintain | certain | medium | keep the chain linear; fixes land before the fork point |

---

### 9. Links

- Upstream mod — [MCModderAnchor/TACZ](https://github.com/MCModderAnchor/TACZ)
- NeoForge 1.21.1 port — [MUKSC/TACZ-1.21.1](https://github.com/MUKSC/TACZ-1.21.1)
- This fork's parent — [Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated)
- SimpleBedrockModel — [TartaricAcid/SimpleBedrockModel](https://github.com/TartaricAcid/SimpleBedrockModel)
- SimpleBedrockModel Fabric shim — [Sh1roCu/SimpleBedrockModel-Fabric](https://github.com/Sh1roCu/SimpleBedrockModel-Fabric)
