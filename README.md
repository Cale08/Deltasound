# Deltasound

Client-side Fabric mod for Hypixel SkyBlock. Plays custom (or vanilla) sounds when chat matches configured patterns — starting with RNG drop lines.

## Requirements

- Minecraft **26.1.2**
- Java **25**
- Fabric Loader **0.19.3+**
- Fabric API

## Features (v0.1)

- Chat trigger engine (regex → sound id)
- Config file written on first launch: `config/deltasound/triggers.json`
- Default trigger for `RNG Drop! <player> unlocked <drop>!` using a vanilla sound so it works before you add `.ogg` files

## Download

The latest jar is always on the [Releases](https://github.com/Cale08/Deltasound/releases/latest) page (updated automatically on every push to `main`).

## In-game config

Commands (client-side):

- `/deltasound`
- `/ds`

The main screen is a trigger list:

- **Add trigger** — name, chat activator text, then upload `.ogg`/`.mp3` or pick a Minecraft sound
- **Test** — posts a client-only chat line `Deltasound Test>> <text>` and runs the trigger pipeline
- **Delete** — removes a trigger

## Development

Minecraft 26.1 needs **JDK 25**. On Windows, Microsoft Build of OpenJDK 25 works well (`winget install Microsoft.OpenJDK.25`). Point `JAVA_HOME` at it, or let Gradle’s toolchain auto-provision/detect it.

```bash
./gradlew runClient
./gradlew build
```

Built jar: `build/libs/deltasound-<version>.jar`

## Adding a custom sound

1. Put an `.ogg` at `src/main/resources/assets/deltasound/sounds/rng_drop.ogg`
2. Keep / edit the entry in `assets/deltasound/sounds.json`
3. In `config/deltasound/triggers.json`, enable the `rng_drop_custom` trigger (or set a trigger `"sound": "deltasound:rng_drop"`)

## Config example

```json
{
  "triggers": [
    {
      "id": "rng_drop",
      "pattern": "RNG Drop! (.+) unlocked (.+)!",
      "sound": "minecraft:entity.player.levelup",
      "enabled": true,
      "ignore_overlay": true,
      "cooldown_ms": 1500,
      "require_local_player_name": false
    }
  ]
}
```

Capture group 1 is treated as the player name (used when `require_local_player_name` is true). Group 2 is available for future per-drop sound routing.

## Staying current across Minecraft updates

See [docs/PORTING.md](docs/PORTING.md). Minecraft touch-points are isolated in `client/bridge` so ports stay small.

## Roadmap

1. Chat triggers (current)
2. Per-drop sound maps
3. Dungeon room-enter sounds
4. Optional vanilla/Hypixel SFX replacement
