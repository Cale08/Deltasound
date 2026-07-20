# Porting Deltasound to a new Minecraft version

Goal: keep version churn inside a few adapter files so chat/sound logic rarely changes.

## Architecture

| Layer | Package | Minecraft dependency | When it changes |
|-------|---------|----------------------|-----------------|
| Core | `dev.deltasound.core` | None | Almost never |
| Config model | `dev.deltasound.client.config` | Fabric Loader paths + Gson only | Rarely |
| Bridges | `dev.deltasound.client.bridge` | Fabric API + vanilla | **Every port** |
| Entry | `dev.deltasound.client.DeltasoundClient` | Fabric entrypoint only | Rarely |

Rules:

1. Prefer Fabric API events over mixins.
2. Do not put `net.minecraft.*` types in `core`.
3. Trigger definitions live in JSON (`config/deltasound/triggers.json`), not hardcoded Java.
4. Avoid mixins until sound *replacement* requires intercepting the sound engine.

## Checklist (every Minecraft bump)

1. Update `gradle.properties`: `minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`.
2. Refresh Gradle wrapper if Fabric docs require a newer Gradle (`./gradlew wrapper --gradle-version <ver>`).
3. Confirm Loom plugin id is still `net.fabricmc.fabric-loom` (post-26.1 / unobfuscated).
4. Compile and fix only `client.bridge` (and entrypoint if signatures moved).
5. Re-verify:
   - `ClientReceiveMessageEvents.GAME`
   - `Component#getString()`
   - `Identifier.parse` / `fromNamespaceAndPath`
   - `SoundEvent.createVariableRangeEvent`
   - `SimpleSoundInstance.forUI`
   - `GameProfile` player name accessor
6. Smoke test in `runClient`: send a fake chat line or join Hypixel and trigger an RNG message.
7. Bump `mod_version` and note the Minecraft target in the changelog / commit message.

## Adding features without hurting ports

- **New chat patterns**: edit JSON only.
- **Custom `.ogg` files**: drop under `assets/deltasound/sounds/`, declare in `sounds.json`, point a trigger `sound` at `deltasound:<name>`.
- **Dungeon rooms (later)**: add `core` room-id matching + a new `DungeonBridge` that reports “entered room X”. Do not fold room scanning into chat code.
- **Sound replacement (later)**: isolate in `bridge/SoundReplaceMixin` (or Fabric event if one exists). Keep replace tables data-driven.

## Useful links

- [Fabric develop versions](https://fabricmc.net/develop/)
- [Fabric porting docs](https://docs.fabricmc.net/develop/porting/)
- [Fabric example mod `26.1` branch](https://github.com/FabricMC/fabric-example-mod/tree/26.1)
