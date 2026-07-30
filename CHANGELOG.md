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

- A modifier script that fails halfway no longer returns the previous call's
  result.
- `gradlew` is marked executable in the repository.
