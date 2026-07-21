# Deltasound

Client-side Fabric mod for Hypixel SkyBlock. Plays custom (or vanilla) sounds when chat matches configured patterns — starting with RNG drop lines.

## Authors

- [Cale08](https://github.com/Cale08) (cfrost)
- [ukpan25](https://github.com/ukpan25)

A passion project we’re building together.

## Requirements

- Minecraft **26.1.2**
- Java **25**
- Fabric Loader **0.19.3+**
- Fabric API

## Features

- Chat trigger engine (substring / regex → sound)
- In-game config GUI (`/deltasound` or `/ds`)
- Import `.ogg` / `.mp3`, pick built-in Minecraft sounds, per-trigger volume
- Config saved to `config/deltasound/triggers.json`

## Download

The latest jar is always on the [Releases](https://github.com/Cale08/Deltasound/releases/latest) page (updated automatically on every push to `main`).

## In-game config

Commands (client-side):

- `/deltasound`
- `/ds`

The main screen is a trigger list:

- **Add trigger** / **Edit** — name, chat activator, browse files in-game or pick a Minecraft sound, set volume
- **Test** — posts a client-only chat line `Deltasound Test>> <text>` and runs the trigger pipeline
- **Delete** — removes a trigger

File picking uses an in-game browser (no Windows native dialog), so it works on Windows 10 and 11.

## Development

Minecraft 26.1 needs **JDK 25**. On Windows, Microsoft Build of OpenJDK 25 works well (`winget install Microsoft.OpenJDK.25`). Point `JAVA_HOME` at it, or let Gradle’s toolchain auto-provision/detect it.

```bash
git clone https://github.com/Cale08/Deltasound.git
cd Deltasound
./gradlew runClient
./gradlew build
```

Built jar: `build/libs/deltasound-<version>.jar`

Collaborators can push to `main` (CI rebuilds the Latest release) or open pull requests if you prefer review first.

## Adding a custom sound

1. Put an `.ogg` at `src/main/resources/assets/deltasound/sounds/rng_drop.ogg`
2. Keep / edit the entry in `assets/deltasound/sounds.json`
3. In `config/deltasound/triggers.json`, point a trigger `"sound": "deltasound:rng_drop"` — or use **Browse files** in `/ds`

## Config example

```json
{
  "triggers": [
    {
      "id": "rng_drop",
      "name": "RNG Drop",
      "match": "RNG Drop!",
      "mode": "CONTAINS",
      "sound": "minecraft:entity.player.levelup",
      "volume": 1.0,
      "enabled": true,
      "ignore_overlay": true,
      "cooldown_ms": 1500,
      "require_local_player_name": false
    }
  ]
}
```

With **Contains** mode, the activator text is matched as a substring. **Regex** mode supports capture groups (group 1 = player name when `require_local_player_name` is true).

## Staying current across Minecraft updates

See [docs/PORTING.md](docs/PORTING.md). Minecraft touch-points are isolated in `client/bridge` so ports stay small.

## Roadmap

1. Chat triggers (current)
2. Per-drop sound maps
3. Dungeon room-enter sounds
4. Optional vanilla/Hypixel SFX replacement
