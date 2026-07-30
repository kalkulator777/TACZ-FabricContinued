## Roadmap

Where this fork is going and how it plans to get there.

The short version: the mod is on 1.21.1 today. The goal is Minecraft 26.1, with
**1.21.11 as a shipped release along the way**, not as a detour.

Everything here is a plan, not a promise. Dates are deliberately absent.

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
| 1.21.9 | blockstate JSON format |

Reaching 1.21.11 absorbs all of it. What remains for 26.1 is mostly the switch to
an unobfuscated toolchain plus one drop's worth of API churn. So 1.21.11 is not
extra work — it is the same work with a point where a release can ship.

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

**Optional integrations may lag.** PlayerAnimator has no build past 1.21.7, so
third-person player animations become an optional module that is disabled when the
library is absent. The mod itself does not depend on it.

---

### 3. Dependency status

Verified against the published Fabric artifacts.

| Dependency | 1.21.11 | 26.1.2 |
|---|:---:|:---:|
| Fabric API | `0.141.6+1.21.11` | `0.155.2+26.1.2` |
| Cloth Config | yes | yes |
| Cardinal Components API | yes | yes |
| Forge Config API Port | yes | yes |
| Architectury | yes | yes |
| JEI | yes | yes |
| REI | yes | yes |
| Mod Menu | yes | yes |
| Shoulder Surfing Reloaded | yes | yes |
| PlayerAnimator | no — 1.21.7 is the last build | no |
| SimpleBedrockModel-Fabric | no | no — see §4 |
| Parchment | no release | no release |

The picture is identical for both targets: there is nothing available on 26.1 that
is missing on 1.21.11. Dependencies are not what makes this port hard.

A few development-time and compatibility dependencies pinned to opaque CurseForge
file IDs have gone stale and will be dropped or repointed at Modrinth coordinates.

---

### 4. SimpleBedrockModel

The one library with no build for either target. It is a Bedrock model, animation,
molang and particle library, and the mod uses almost none of it — it has its own
Bedrock model system in `client/model/bedrock/`.

The actual consumption surface is **10 files and 6 types**:

| Type | Origin | Used by |
|---|---|---|
| `ViewportEvent` (and `ComputeCameraAngles`, `ComputeFov`) | the library's Fabric shim | 5 imports |
| `RenderTickEvent` | the library's Fabric shim | 4 imports |
| `RenderHandEvent` | the library's Fabric shim | 1 import |
| `IFPGeoItemRenderer`, `IFPAnimationInstance` | the library proper | `client/renderer/item/AnimateGeoItemRenderer.java` |
| `FirstPersonRenderHandler` | the library proper | `client/resource/ClientIndexManager.java` |
| `Pose`, `DummyPose` | `com.maydaymemory:mae`, a separate artifact | `AnimateGeoItemRenderer.java` |

The molang runtime (33 classes), the particle system (25+ classes) and the Bedrock
model loader are not referenced at all. The library's Fabric half is five client
mixins targeting the same vanilla classes the mod already patches, so today two
mods compete for the same methods.

**Plan: absorb it.** Move three event classes, three interfaces and five mixins
into the mod, depend on `mae` directly from Maven, drop the rest. That removes the
`libs/` flat directory, removes the jar-in-jar, removes the mixin overlap, and
leaves one codebase to port instead of two.

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

- [x] **Security.** Sandbox gun pack scripts, range-check network slot indices,
      guard jar extraction. See the [changelog](CHANGELOG.md).
- [ ] **Deletion.** Roughly 1300 of the 2970 lines of the Forge compatibility shim
      have native Fabric equivalents, plus dead code and a vendored copy of the
      conventional tag data. Every line removed here is a line not ported later.
- [ ] **Correctness.** Version-independent fixes — see §7.
- [ ] **Build hygiene.** Drop `mavenLocal()`, unhardcode the publish repository,
      remove Gradle 9 incompatibilities, remove the vestigial Yarn mapping property.
- [ ] **Infrastructure.** There is no CI and no test suite. Several components are
      testable without a Minecraft harness: the resource scanner, the JSON data
      managers, tag tree search, the modifier evaluator, the pack converter's
      rewrite table.

#### Phase 1 — absorb SimpleBedrockModel, still on 1.21.1

- [ ] See §4.

#### Phase 2 — port to 1.21.11 and release

- [ ] Toolchain: Loom, Gradle, loader, Fabric API.
- [ ] Common code: item and block properties, `Optional` NBT getters, the rewritten
      recipe system, `ValueInput`/`ValueOutput`, minecarts, explosions, entity spawn
      data, reload listeners.
- [ ] Client: item rendering without `BuiltinItemRendererRegistry`, HUD on
      `HudElementRegistry`, 2D `GuiGraphics`, entity render states, the scope stencil
      mask, camera and FOV handling, `RenderPipeline` for the laser beam.
- [ ] Mixins last — they only validate in a running game.
- [ ] Resources: item definition JSON, blockstate format, recipe ingredient form.
- [ ] Make PlayerAnimator support an optional module.

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

**Crashes and hangs**

- Heat multiplier is applied after the RPM clamp, so a division by zero can reach
  the server tick loop.
- `FireMode.valueOf` runs on raw NBT and throws from the tooltip, the firing path
  and the tick loop.
- Tag tree search has no visited set or depth limit, so a cyclic tag overflows the
  stack.
- A duplicate resource ID, or a single malformed gun pack, aborts the entire reload
  instead of being isolated.
- `/tacz reload` blocks the server thread in singleplayer.

**Incorrect behaviour**

- Attachment tags are keyed on `DataComponents.CUSTOM_DATA.toString()` rather than
  a real key.
- Each bullet calls `hurt()` twice, the second usually for zero damage — this
  doubles Thorns, hit sounds, aggro and damage events seen by other mods.
- Physical side (`EnvType`) is used where logical side is meant, so movement
  inaccuracy and lag compensation do not work in singleplayer or on a LAN world.
- A `-1` sentinel from a missing gun index is tested against `0`, which can leave a
  player permanently unable to fire with no diagnostic.
- The first shot after each respawn or dimension change is silently swallowed.
- The legacy gun pack hint never appears in multiplayer. It is a client-side message
  hooked to a server-side join, so it only fires on an integrated server.
- Lua script writes to item NBT are silently dropped despite the API documenting
  them as persistent.
- Glass breaking, ignition and bell ringing bypass region protection and the
  `mobGriefing` rule.

**Robustness**

- The shared assets holder is swapped from a background thread before the reload
  succeeds, and is not `volatile`.
- One data manager is only half synchronized — the fast path reads an unguarded
  `HashMap`.
- Client firing logic reads item components off the main thread.
- No spectator check on the server firing path.
- A static handoff field leaks the reloadable server resources permanently.
- The gun pack sync packet is sent as a single payload and needs measuring against
  the 1 MiB limit.

**Performance and polish**

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
| The scope stencil mask cannot be reproduced — the new GPU abstraction has no stencil concept | medium | high; changes how every optical scope looks | prototype it before committing to the render port |
| `SpecialModelRenderer` does not cover what the mod needs from item rendering | low | high | prototype on a single item first |
| No replacement for the current camera/FOV discriminator | medium | medium; scope zoom and gun model FOV depend on it | find a new discriminator during the client port |
| PlayerAnimator never appears for the target | high | low; optional module | ship without it, fork later if worth it |
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
