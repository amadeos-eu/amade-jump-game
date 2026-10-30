# amade-jump-game

**amade-jump-game** is the platformer of the [amadeOS](https://github.com/amadeos-eu/amadeos) desktop: run and jump through three levels larger than the screen, a meadow, caves and a climb into the clouds, take the coins, jump on beetles and birds and reach the red flag. The levels are maps of the [Tiled](https://www.mapeditor.org/) editor; best times and coins are saved. Built on [amade-game-engine](https://github.com/amadeos-eu/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- **Space**, **Enter** or button **A** on a gamepad leads from the title to the choice of a level. **Left** and **right** choose, **Space** or **Enter** plays. A level opens when the one before it is finished.
- **Arrow keys** or **A** and **D** run, **Space**, **W** or **Up** jump. Holding the key jumps higher; letting go early makes a short hop. A jump pressed a moment before landing, or a moment after running off an edge, still counts.
- **Down** drops through a wooden ledge.
- Coins are taken by touching them. A beetle or a bird is beaten by landing on it from above, and the player bounces off; touching one in any other way, spikes, water or a fall into a pit sends the player back to the last **green flag** reached. There are no lives: the clock goes on and the falls are counted.
- A **spring** throws whoever lands on it high up.
- At the **red flag** the time, the coins and the falls are shown, with a new best time or all coins marked.
- **P** or **Esc** pauses, and so does leaving the window; **Esc** in the pause leaves the level.
- Gamepads: d-pad or left stick to run, **A** or **B** to jump, **Start** to pause.
- **F11** switches to full screen and back. *Game → Graphics* chooses how the level reaches the screen: **Direct** (at the screen's full size) or **Pixels** (the level at 640 × 400, enlarged by whole factors). **F12** shows how long each frame takes.
- Sound effects and music can be switched off under *Game → Sound*.

The keys can be changed in `~/.config/amadeos/jump/jump.properties`, with the names Java gives them, several separated by spaces:

```properties
keys.left=LEFT A
keys.right=RIGHT D
keys.down=DOWN S
keys.jump=SPACE UP W
keys.pause=P ESCAPE
```

Texts in English, Deutsch, Français, Español and Italiano.

## Gamepads

Plugged in before or while the game runs; any of them plays. *Help → About* shows how many are connected.

- **Linux**: the game reads `/dev/input/js0` to `js3`, which needs read access to them (on most desktops the user at the screen has it). The buttons follow the Xbox layout of the `xpad` driver.
- **Windows**: XInput, so Xbox and compatible controllers.
- Elsewhere the game runs without gamepads.

## Files

| What | Where |
|---|---|
| Settings (graphics, sound, keys) | `~/.config/amadeos/jump/jump.properties` |
| Progress (open levels, best times, most coins) | `~/.local/share/amadeos/games/jump/progress.save` |

The progress is written to a new file that then replaces the old one, with the old one kept as `progress.save.bak` and a checksum in the last line, so a crash while saving or a damaged file loses nothing.

## Making levels

The levels are `src/main/resources/eu/amadeos/games/jump/levels/level1.tmx` to `level3.tmx`, made for Tiled 1.10 or later, with the tileset `tiles.tsx` and its picture `tiles.png`. Open them in Tiled, change them, save; the game reads them as they are.

- A map is orthogonal, not infinite, with tiles of 16 × 16 pixels. Its properties `name` and `theme` (`meadow`, `cave` or `sky`, the background) are optional.
- The tile layer **ground** is what the player stands on; the tileset gives each tile a class: `solid`, `platform` (stand on it from above, jump through from below), `spikes`, `water` or `coin`. Tiles without a class are decoration. Any other tile layer is only drawn: one below **ground** in Tiled's list, such as **back**, behind the springs and flags, one above it in front of them. Animations made in Tiled's tileset editor run in the game.
- The object layer **things** holds rectangles of the classes `start` and `goal` (one each), `checkpoint`, `spring`, `beetle` (properties `careful`: turns at edges, `direction`: `left` or `right`) and `bird` (properties `axis`: `x` or `y`, `range` in tiles, `period` in seconds).
- Infinite maps, other orientations than orthogonal and object templates cannot be read; such a map is rejected with its file and line. Flipped or rotated tiles are drawn as they are in the tileset.

`LevelTest` checks that the goal and every coin of each level can be reached with the player's jumps, so a level that cannot be finished fails the build.

## Building

```bash
./gradlew test          # the window test needs a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-jump-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

Gamepads on Windows go through Java's foreign function interface, so the game starts with `--enable-native-access=ALL-UNNAMED` (set in `build.gradle` for `run`, the tests and the packages).

The pixel art is drawn by `tools/MakeSprites.java` (`sprites.png` and `levels/tiles.png`), the icon by `tools/MakeIcon.java`. `./gradlew screenshots -Pshots=<folder>` draws the handbook's screenshots.

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface or time of their own. `Game` (a level being played: running, jumping, the tiles touched, enemies stomped or touched, springs, checkpoints, falls and the goal), `Level` (reads a Tiled map: its ground as tiles, its things by their class), `Player` (a body that runs and a jump), `Enemy` (beetles walk with the player's physics, birds fly back and forth), `Tile`, `Input`, `Event`, `Field` (sizes, speeds, gravity, jump heights), `LevelResult`.
- `repository`: `JumpSettings` (graphics, sound, keys), `Progress` (open levels, best times, most coins, in a save file).
- `ui.game`: the running game on the loop thread. `JumpGame` (loop, controls, scenes: title, choice of a level, play, pause, goal; the camera), `JumpPainter` (the level as the camera sees it: background, the map's tile layers, things, enemies, player), `Backdrop` (the backgrounds of the themes, in layers), `Hud` (coins, clock, falls, the cards and texts), `Effects` (events become particles, sounds and music), `SoundBank`, `Music` (the tunes, in MML), `Arena` (what the presenter may ask of the game).
- `ui.main`: Model-View-Presenter for everything around the level: `JumpPresenter`, `JumpView`, `JumpWindow`.

From the engine: `TiledMap` (maps of the Tiled editor), `Camera` and `Parallax`, `Body`, `TileCollider` and `Jump`, `SaveFile`, `TileMap`, `Gamepads`, `GameLoop`, `GameCanvas`, `Sprite` and `SpriteSheet`, `Particles`, `Sound`, `Tune` and `Mixer`, `Keyboard` and `Controls`, `SceneStack`, `FullScreen`.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://docs.amadeos.eu/amade-game-engine/) quotes; chapter 27 is about this game.

## Licence

[EUPL-1.2](LICENSE)
