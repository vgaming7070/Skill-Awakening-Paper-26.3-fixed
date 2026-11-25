# Skill Awakening Paper 26.3 — Vgaming70

Source rebuild for a PaperMC 26.3 server. Bedrock players may join the same Paper server through GeyserMC; no separate Bedrock addon is needed for that setup. Standard Bukkit inventory menus are used, but Bedrock/Geyser interactions still need live compatibility testing.

## Build

Requires Java 25 and Gradle 9.1.0.

```sh
gradle clean build
```

The JAR will be in `build/libs/`. GitHub Actions builds it automatically when pushed to `main` or when manually started from Actions.

## Implemented in this source rebuild

- 26 named skills in five ranks: Common (8), Rare (6), Epic (5), Legendary (4), Mythical (3).
- Five skill equipment slots, menu book, equipment GUI, archive/withdraw GUI, cast selection GUI.
- Physical skill books learn skills; withdrawal unequips and returns a book.
- Mythical claims persisted; roll excludes claimed Mythicals; OP admin menu releases claim; broadcasts Mythical roll with player name.
- Starter menu token and Ancient Tablet; Awakening animation; sound and particles.
- PvP skill-book drop; chest-click tablet reward; Skill Awakening items protected from death drops.
- Individual learned/equipped progress persisted in config.
- Required cooldowns: World Breaker and Spatial Crack 210 seconds; Reality Shift 330 seconds.
- Requested effects coded: World Breaker direct health subtraction (intended armor-bypassing damage), Spatial Crack ray safe teleport up to 30 blocks + Regeneration II for 5 seconds, Reality Shift Resistance X and Fire Resistance, Frost Touch Slowness II.
- Ancient Tablet recipe is unchanged from the uploaded 8.9.0 JAR: `LLL / EDE / BBB`, where L=Lapis Lazuli, E=Experience Bottle, D=Diamond Block, B=Book.

## Known limitations / test-required

This is a source rebuild, not a verified release. It has not yet been compiled against the exact Paper 26.3 API or run on a server. Some skill mechanics are simplified implementations, menu title/interaction behavior must be tested on both Java and Geyser Bedrock clients, structure chest reward detection is a chest interaction approximation (not verified loot-table integration), and the death-protection behavior protects tagged Skill Awakening items rather than a full invulnerability feature. Skill rolling/ownership, GUI edge cases, recipe, persistence, and each skill effect require server testing. Do not deploy to a production world without backups and testing.
