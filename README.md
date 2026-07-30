## Timeless and Classics Zero: Refabricated

An unofficial Fabric port of [Timeless and Classics Zero](https://github.com/MCModderAnchor/TACZ),
a gun mod for Minecraft.

This repository is a fork of [Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated),
continuing the work with a focus on stability and on reaching current Minecraft
versions. See the [roadmap](ROADMAP.md) for where it is headed and the
[changelog](CHANGELOG.md) for what has changed so far.

> This is experimental software. Expect bugs, and please report them.

### Supported versions

| Minecraft | Status |
|---|---|
| 1.21.1 | current |
| 1.21.11 | planned — see the [roadmap](ROADMAP.md) |
| 26.1 | planned |

### Requirements

- [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) — required

### Gun packs

Custom gun packs go in the `tacz` folder inside your game directory.

- Packs built for older versions need to be brought up to 1.21.1 NeoForge format
  first, which the [pack upgrader](https://www.curseforge.com/minecraft/mc-mods/tacz-pack-upgrader)
  does for you. The upgraded pack then loads here.
- If in-game text turns into empty squares after adding a pack, unzip the pack and
  load the extracted folder instead.

No changes to gun packs are required for any planned version — see
[§2 of the roadmap](ROADMAP.md#2-what-this-means-for-gun-packs-worlds-and-addons).

### Known issues

- Keybinds can conflict with other mods. [Keybind Fix Plus](https://modrinth.com/mod/keybind-fix-plus)
  or a similar mod resolves it.
- Third-person player animations depend on PlayerAnimator, which has no build past
  1.21.7. That integration will become optional.

### Building

Requires JDK 21.

```
./gradlew build
```

This compiles, runs the unit tests and remaps the jar, which lands in
`build/libs/`. The same command runs in CI on every push.

To publish to a local Maven repository, pass a target — otherwise it goes to
`build/repo`:

```
./gradlew publish -Ppublish_repo=/path/to/repository
```

### Credits

- **Timeless and Classics Zero** — [MCModderAnchor](https://github.com/MCModderAnchor/TACZ),
  the original mod.
- **TACZ-Refabricated** — [Sh1roCu](https://github.com/Sh1roCu/TACZ-Refabricated),
  the Fabric port this fork builds on.

### License

Code is licensed under the [GNU GPL v3](LICENSE). Assets carry the upstream
CC BY-NC-ND 4.0 terms.
