## Changelog

Changes made in this fork on top of [Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated).

### Unreleased

**Security**

- Gun pack scripts are no longer able to escape the game. Attachment modifier
  functions were evaluated through `JsePlatform.standardGlobals()`, which exposes
  `luajava`, `io`, `os` and `debug` — enough for any gun pack to run arbitrary
  code on the machine that loads it. Both script entry points now share one
  sandbox with those libraries removed.
- Inventory slot indices received from the client are range-checked before use.
  A negative index used to throw out of the packet handler and crash a dedicated
  server.
- `AttachmentType.fromId` returns `NONE` for an unknown id instead of throwing,
  and the laser colour packet bounds its map to the number of attachment types.
- Extracting the built-in gun pack from the mod jar rejects entries that would
  be written outside the target directory.

**Performance**

- Attachment modifier scripts are compiled once and cached per thread, instead of
  being recompiled on every evaluation — this runs on hot paths such as recoil.
  The previous single shared script engine was also mutated from both the client
  and the server thread.
- Reading an attachment off a gun no longer walks the component registry
  backwards on every call. The serialized stack's component map was indexed with
  `DataComponents.CUSTOM_DATA.toString()`, which happens to produce the registry
  name — the key is a constant now. These methods run several times per frame
  from the HUD and the gun model.

**Build**

- There is CI. A GitHub Actions workflow builds every push and pull request.
- There is a test suite, run by `./gradlew build`. It starts with the Lua
  sandbox — those tests exist so that a change back to
  `JsePlatform.standardGlobals()` fails the build rather than quietly
  reintroducing arbitrary code execution — plus the modifier evaluator and fire
  mode parsing.
- `mavenLocal()` is out of the repository list. With it there, the result of a
  build depended on whatever happened to be sitting in the developer's `~/.m2`,
  so the same commit could produce different jars on two machines.
- The publish repository is no longer a hardcoded Windows path on the upstream
  author's machine. It comes from `-Ppublish_repo` or `TACZ_PUBLISH_REPO` and
  falls back to `build/repo`; the published coordinates are unchanged.
- The eleven Gradle deprecation warnings the build printed on every run are
  gone, including `processResources` reading `project` at execution time, which
  is what stops the configuration cache from working.
- The `yarn_mappings` property is dropped. Mappings are Mojmap plus Parchment
  and the Yarn line has been commented out for a long time.
- Sodium was declared as a compile dependency and is not referenced anywhere.
  Sodium and Iris are both runtime dependencies now, so the dev client actually
  exercises the Iris integration — previously both were compile-only and that
  code never ran during development.
- Dependencies pinned to opaque CurseForge file IDs are on readable Modrinth
  versions where one exists. Four stay on CurseForge and now say why in a
  comment: two dev-only test mods and MrCrayfish's Framework and Controllable
  have no Modrinth listing, and Carry On publishes all three loaders under one
  Modrinth version number, where the coordinate resolves to the NeoForge jar.

**Changed**

- Third-person player animation moved from PlayerAnimator to Player Animation
  Library. PlayerAnimator has no build past 1.21.7 and its author now points at
  PAL, which covers 1.21.1 through 26.2. Nothing changes for gun packs: the same
  animation files load, and the mod reads them from the same folder.

  Mostly a package rename, with one trap. Packs are exported with
  PlayerAnimator's easing names — `INOUTSINE` and friends — and PAL spells the
  same curves `easeinoutsine`, falling back to `LINEAR` for anything it does not
  recognise without logging anything. Migrating without translating them would
  have turned every keyframe in every pack linear: no crash, no warning, just
  animations that no longer look like what their author made. The three built-in
  animations alone name an easing on all 2619 of their keyframes. The names are
  rewritten on load and the tests pin it.

- SimpleBedrockModel is no longer a dependency. It had no build for any version
  this fork is heading to and was the last jar in `libs/`, and it was not the
  peripheral library it looked like: the mod implements its `IFPGeoItemRenderer`,
  so the library found the mod's renderer and drove it — the whole first-person
  view ran through the library's `FirstPersonRenderHandler`.

  The part the mod runs on was absorbed: three events, two interfaces, the
  first-person handler and its animation clock. Four mixins came with them, and
  none needed a new file except `Camera` — the library patched `Minecraft`,
  `GameRenderer` and `ItemInHandRenderer`, which this mod already patches, so two
  sets of mixins on the same vanilla methods became one.

  Left behind: the molang runtime and the particle system, reachable only through
  a first-person particle system nothing here ever fed, and 250-odd other classes.
  Also `mae`, whose only use was constant stubs for methods the interface no longer
  declares — it reached the runtime solely because the library bundled it.

  Original library is LGPL-3.0 by Sh1roCu; the absorbed files say where they came
  from.

- The Iris integration uses the stable `api.v0` where one exists. Three of its
  four calls went through Iris internals; two had an API equivalent and now use
  it, a third was the same check under a different name and had no callers. The
  remaining one — flushing Iris's batched buffer so our stencil-based scope
  rendering happens where we put it — has no API equivalent and is isolated in
  a class that only loads when Iris is present.

- The config screen keybind ships unbound. It defaulted to `T`, which is vanilla's
  chat key — on Forge the binding is Shift+T and Forge's key modifiers keep the
  two apart, but plain `KeyMapping` has no modifiers, so pressing `T` opened the
  mod's config screen and chat never got a look in. No vanilla key is free and
  any specific choice eventually collides with another mod, so the key is listed
  under Options → Controls for you to bind. The config is also reachable from
  Mod Menu.
- `/tacz reload` no longer starts the client resource reload on the server thread
  and wait for it. That pinned the integrated server for the whole reload and
  could deadlock, since the client blocks on the server during a datapack reload.
  It also reloaded data twice in singleplayer. The reported time covers the data
  reload.

- The Forge item-handler capability layer inherited by this fork is gone, replaced by
  vanilla `Container`. It was about 1100 lines — a reimplementation of `IItemHandler`,
  `LazyOptional`, nine inventory wrappers, three entity mixins and an injected
  interface on `LivingEntity` — and every caller asked for the same side-agnostic view,
  so none of the capability machinery was doing anything. Four copies of "does this
  inventory hold ammunition for this gun" collapsed into `AbstractGunItem.hasAmmoFor`.

  API note: `AbstractGunItem.findAndExtractInventoryAmmo` and its deprecated twin now
  take a `net.minecraft.world.Container` instead of the removed `IItemHandler`.

- Reimplementations of Forge events that Fabric API already provides are gone:
  player login and logout now come from `ServerPlayConnectionEvents`, and entity
  removal from `ServerEntityEvents.ENTITY_UNLOAD`. Two mixins and a mixin injection
  went with them. The trampoline classes that existed only to let a mixin fire an
  event now have the mixins fire it directly.

  API note: `cn.sh1rocu.tacz.api.event.PlayerEvent` and `EntityRemoveEvent` are
  removed. `LogicalSide` is not — it is the logical side, which `EnvType` cannot
  express, and it is part of the gun event API.

**Removed**

- The Accelerated Rendering integration. It moved gun model vertex transforms
  onto the GPU through compute shaders, but the Fabric port stopped at 1.21.1
  and is alpha throughout, and the NeoForge original it was ported from stopped
  there too — there is nothing newer to follow. It was also the most invasive
  integration in the client renderer: not a hook but a second, parallel set of
  render paths through the gun model, the attachment model and the laser, in
  exactly the code the 1.21.2 → 1.21.6 render rewrites force us to rework.
  Nothing is lost but frames — every call site was guarded and fell back to the
  vanilla path, which is now the only path.
- Dead code: the unused version checker and the deprecated gun pack JSON loader
  (neither was referenced), two mixins whose bodies were entirely commented out,
  an empty compatibility mixin, and a dead access widener entry.
- The KubeJS integration, which was commented out in full along with its
  dependencies, and the two KubeJS registration files that would have made the
  game load those classes against an API that is not present.
- The OptiFine branch of the stencil setup. OptiFine does not exist on Fabric for
  this version, so the branch was never taken.
- The vendored copy of the conventional tag data — 356 files, of which Fabric API
  already provides all but `c:gunpowders` and `c:nuggets/iron`. Those two are kept.
- `ModPainting`, an empty `init()` whose real body was commented out. Painting
  variants are a datapack registry on this version and the mod already ships
  `data/tacz/painting_variant` plus the `placeable` tag, so the painting works
  without it. `CommonLoadPack` went the same way — its body was commented out and
  the class it called does not exist in this port.
- An unused Turkish translation file. Minecraft only loads lowercase locale names,
  so `tr-TR.json` was never read; the real one is `tr_tr.json`.
- 19 MB of source `.bbmodel` files that were neither built nor shipped.
- `GameRendererAccessor`, whose three invokers lost their only consumer when upstream
  removed the hand renderer.

**Fixed**

- Shader packs work again. With Iris driving the pipeline, the world froze at the
  frame the pack was enabled on and the screen went black when scoping. Both come
  from the scope's stencil mask, and neither is repairable while a pack is loaded:

  - The mod swapped the main render target's depth texture to a packed
    `GL_DEPTH32F_STENCIL8` but left blaze3d still calling it `TextureFormat.DEPTH32`
    — the vanilla enum has no combined format to declare. Iris reads that name to
    size `depthtex1` and `depthtex2`, then fills them each frame with
    `glCopyImageSubData`, which requires the two internal formats to match. Measured
    on the same driver, same call, only the format differing: `GL_NO_ERROR` before
    the swap, `GL_INVALID_OPERATION` after. Iris does not check, so the depth
    textures silently stop updating, and every pack that reads `depthtex1` — fog,
    water, reflections, translucency in Photon and Complementary alike — stays welded
    to the last frame that copied.
  - The mask itself cannot work anyway: Iris binds its own gbuffer framebuffer for
    every draw and those carry no stencil attachment, so the stencil test always
    passes and the ocular's black mask — which is exactly what the stencil trims down
    to the tube's circle — covers the whole screen.

  So with a pack loaded the mod now stays off that path entirely and hands the depth
  texture back in vanilla format at the next frame boundary. The scope degrades: the
  body, the ring and the reticle still draw, the circular view and "no gun body
  inside the lens" do not. Getting them back under a deferred pipeline means an
  offscreen mask texture instead of the stencil buffer, which is in the roadmap.
- Firing a gun with heat data no longer divides by zero. The heat multiplier was
  applied after the rounds-per-minute value had been clamped, so enough heat drove
  it to zero and the exception came out of the server tick loop. The same call also
  dereferenced the gun without a null check.
- A gun whose fire mode tag holds an unrecognised value no longer throws. It came
  straight from item NBT into `FireMode.valueOf`, and that is read from the tooltip,
  the firing path and the tick loop; it now reads as `UNKNOWN`, which is already the
  value used when the tag is missing.
- Attachment tags that reference each other in a cycle no longer overflow the stack,
  and a malformed id in tag contents no longer throws — the search keeps track of
  where it has been and parses leniently. It also stops at the first match now
  instead of walking the rest of the tree.
- Carrying a gun through a portal refreshes its state again. Fabric splits the world
  change event in two and dispatches the entity one only for non-players, so the
  handler that exists precisely to fix stale gun data across dimensions never ran for
  a player. Firing, reloading and aiming state stayed stale after every portal.
- A modifier script that fails halfway no longer returns the previous call's
  result.
- Every bullet used to call `hurt()` twice. The split lets armour-piercing damage
  use its own damage source, but the default armour-ignore value is zero, so on
  essentially every shot the second call carried no damage — and a zero-damage
  hurt is still a full hurt: it broadcasts the damage event, plays the hurt sound,
  runs Thorns, sets the aggro target and reaches every other mod's damage hooks a
  second time. Each half now fires only if it carries damage.
- The first shot after a respawn or a dimension change is no longer swallowed.
  Both reset the shoot timestamp to the "has not fired yet" sentinel, and unlike
  the other timestamps that one is an offset rather than an absolute time, so
  subtracting it read as "fired a moment ago" and the cooldown check rejected the
  shot for up to a full firing interval. The client had already played the sound
  and the animation, so the shot simply vanished.
- A gun whose id the server cannot resolve no longer locks the player out of
  firing. The draw and melee cooldowns report `-1` for a missing gun index and the
  client compared them with `!= 0`, so the sentinel read as a cooldown that never
  expires and every trigger pull came back as "you are switching weapons".
- Lag compensation and movement inaccuracy work in singleplayer and on a LAN
  world again. The hitbox history was recorded only when the loader reported a
  dedicated-server distribution, which is not the same thing as the logical side —
  an integrated server runs inside the client distribution, so nothing was ever
  recorded there. Recording positions also no longer depends on the latency fix
  being enabled; that setting has nothing to do with movement inaccuracy, and
  turning it off used to hand every moving player standing accuracy for free.
  Editing the history depth takes effect without a restart.
- Gun pack loot injections apply to mob drops. The hook sat on an overload that
  covers container filling, block drops and fishing but not
  `LivingEntity.dropFromLootTable`, so a pack that injected into
  `minecraft:entities/zombie` dropped nothing. Injected stacks now also pass
  through vanilla's stack splitter like the rest of the table's output.
- One malformed file in a gun pack no longer fails the whole reload. Per-file
  parsing caught only the two JSON exceptions, and these deserializers throw
  `NullPointerException` on a missing field. Files that fail to parse are also no
  longer shipped to clients to fail there as well.
- Reading the pre-load config during resource pack discovery no longer risks
  throwing. It is read before the config system is ready — Forge loads that file
  by hand for exactly this reason — so there is now an accessor that reads the
  toml itself in that window and falls back to the default.
- `gradlew` is marked executable in the repository.
