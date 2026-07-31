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

**In progress.** The tree does not compile, which is expected and is why phases 0
and 1 emptied everything they could out of this one first. Progress is measured
in distinct compile errors: **4756 at the bump, 134 now**, and what is left is
almost entirely client rendering.

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
- [ ] Client: this is all that is left, and it is not a sweep. Three of the four
      pieces are redesigns rather than renames:
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
- [ ] Mixins last — they only validate in a running game. `KeyboardHandler.keyPress`
      and `MouseHandler.onPress` have already changed shape underneath them.

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

None of it can be checked without a GPU. It compiles and the algorithm is
unchanged, but **every scope needs looking at in game before this is trusted** —
and the first thing to check is that the framebuffer is still complete.
- [ ] Resources: blockstate format, recipe ingredient form. The item definition
      JSON is written for the items that have models; what is left is `ammo_box`,
      which needs two things at once — its model `overrides` become a
      `minecraft:select` over a registered item model property, and its colour
      provider becomes an `ItemTintSource`, because item tinting is part of the
      model now. Both are marked in `ClientSetupEvent`.
- [ ] Recipes on the client. `RecipeManager` is not reachable from the client
      level any more and a `RecipeHolder` is keyed by `ResourceKey<Recipe<?>>`
      rather than an `Identifier` — which the craft packet carries, so this is a
      protocol change as well as a client one. It blocks the gun smith table
      screen and both recipe viewer integrations.
- [ ] Adapt the Shoulder Surfing plugin to the 5.x `register` signature.
- [ ] Re-check the Iris buffer flush against the Iris release for the target.

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
- One Iris integration point still calls an internal symbol rather than the stable
  `api.v0`: flushing `FullyBufferedMultiBufferSource`, which has no API equivalent. It
  has to be re-checked on every Iris update.

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

### 8. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| The scope stencil mask cannot be reproduced — the new GPU abstraction has no stencil concept | **rebuilt, unverified** — see phase 2 | high; changes how every optical scope looks | it compiles and the algorithm is unchanged; needs a GPU to confirm the framebuffer is complete and the mask lands where it used to |
| Every renderer has to move to extract-and-submit | certain | high; it is most of the client port | do the block entities first — they are the smallest of the four renderers |
| `SpecialModelRenderer` does not cover what the mod needs from item rendering | low | high | prototype on a single item first |
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
