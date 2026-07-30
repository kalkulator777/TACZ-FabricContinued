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

**Changed**

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
- An unused Turkish translation file. Minecraft only loads lowercase locale names,
  so `tr-TR.json` was never read; the real one is `tr_tr.json`.
- 19 MB of source `.bbmodel` files that were neither built nor shipped.

**Fixed**

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
- A modifier script that fails halfway no longer returns the previous call's
  result.
- `gradlew` is marked executable in the repository.
