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

**Fixed**

- A modifier script that fails halfway no longer returns the previous call's
  result.
- `gradlew` is marked executable in the repository.
