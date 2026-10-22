# CompactSolars-Legacy

Compact Solar Arrays for **Minecraft 1.7.10 (Forge)**. This is an add-on for
[IndustrialCraft²](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) that condenses many
IC2 solar panels into a single block.

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.7.10 |
| Minecraft Forge | 10.13.4.1614 (or compatible 1.7.10 Forge) |
| IndustrialCraft² | 2.2.828-experimental or newer |

## Solar arrays

| Tier | Equivalent IC2 panels | Output at full sun | IC2 tier | Packet power | Buffer |
| --- | ---: | ---: | ---: | ---: | ---: |
| LV | 8 | 8 EU/t | 1 | 32 EU | 16 EU |
| MV | 64 | 64 EU/t | 2 | 128 EU | 128 EU |
| HV | 512 | 512 EU/t | 3 | 512 EU | 1024 EU |
| EV | 2048 | 2048 EU/t | 4 | 2048 EU | 4096 EU |

The buffer is exactly two panels' worth of storage (16/128/1024/4096 EU).

Output now follows IC2's solar rules: it scales linearly with `skyLight / 15` and uses IC2's
`balance/energy/generator/solar` value by default. At full sun the outputs are 8/64/512/2048 EU/t.
LV/MV/HV combine eight of the previous tier; EV is a 4× voltage step, so it combines four HV arrays
(2048 panels) and outputs 2048 EU/t. Keeping EV to one 2048 EU/t packet also keeps it compatible
with IC2Tweaks' packet-model source cap and avoids over-tier explosions at EV sinks.

The charge slot matches the array's output tier: LV charges LV, MV charges up to MV, HV charges
up to HV, and EV charges up to EV, like IC2's BatBox/CESU/MFE/MFSU storage blocks. IC2's solar
panel's own slot happens to be tier 1 because its output is tier 1; CompactSolars intentionally
exposes the higher array tier.

## Solar hats

Each tier also has a solar hat that charges electric armor while the player is in sunlight:

| Hat | Charge rate | Tier |
| --- | ---: | ---: |
| LV Solar Hat | 8 EU/t | 1 |
| MV Solar Hat | 64 EU/t | 2 |
| HV Solar Hat | 512 EU/t | 3 |
| EV Solar Hat | 2048 EU/t | 4 |

Each hat charges **only the chestplate**, accepts **any tier** (`Integer.MAX_VALUE`), ignores the
armor's transfer limit, and has **no confusion mechanic**. It keeps the array-rate output
(8/64/512/2048 EU/t) and syncs the player's container after a successful charge.

IC2's own solar helmet uses the same chest-only/any-tier/ignore-transfer/no-confusion model, but
charges at **1 EU/t** regardless of the panel count and has no useful tooltip. CompactSolars keeps
the array-rate output, the tooltip, and the per-tier textures.

Hats use a single shared, infinite-durability armor material. Their tooltip shows the charge rate
(`Power Output: X EU/t max`), the power tier, and a short description.

## Presentation

- **Array tooltip:** matches IC2's solar panel format, e.g. `Power Output: 8 EU/t max`, using
  IC2's own localization keys.
- **Hat tooltip:** shows the charge rate, `Power Tier: n`, and a short description.
- **GUI:** the array title and inventory label are localized.
- **Creative menu:** arrays and solar hats are listed in IC2's creative tab.
- **Sound:** uses IC2's metal block step/place/break sound. IC2's solar panel has no ambient
  generator loop, so there is no continuous hum.

## Configuration

`config/CompactSolars.cfg`:

| Option | Default | Description |
| --- | ---: | --- |
| `general.scaleFactor` | `1` | Generation interval. `1` generates every tick; `2` generates on average every other tick, etc. Values below `1` are clamped to `1`. This does not change the per-generation EU amount. |
| `general.solarBaseValue` | IC2's `balance/energy/generator/solar` (usually `1.0`) | EU/t per panel at full sky light (15). Set to `0` to disable generation. Negative or NaN values fall back to `1.0`. |
| `general.wrenchParity` | `true` | IC2-style wrench behavior: breaking without a wrench drops a Basic Machine Casing, and a wrench has a 90% chance to preserve the tier. Set `false` for the old always-drop behavior. |

## Wrench behavior

With `wrenchParity=true` (the default):

- Breaking an array without a wrench drops a **Basic Machine Casing**, even for MV/HV/EV. This is
  the IC2-style machine drop for a machine broken without a wrench.
- A wrench roll has a **90%** chance to preserve the original tier. On failure, the normal Basic
  Machine Casing drop is used.
- IC2's electric wrench lossless mode can force a successful wrench operation, matching IC2's
  behavior.
- Set `wrenchParity=false` to restore the old behavior: every break drops the exact tier.

## Recipes

Array recipes are shaped and use eight of the previous tier (four for EV, which is a 4× voltage
step):

- **LV Solar Array:** 8 IC2 solar panels + 1 LV transformer
- **MV Solar Array:** 8 LV Solar Arrays + 1 MV transformer
- **HV Solar Array:** 8 MV Solar Arrays + 1 HV transformer
- **EV Solar Array:** 4 HV Solar Arrays + 1 EV transformer

Hat recipes are shapeless:

- **Solar Hat:** 1 matching Solar Array + 1 iron helmet

### Recipe inputs

Array recipes use IC2's solar panel and transformer items directly. The solar hat recipe uses
Forge's OreDictionary for the iron helmet, so packs and other mods can substitute alternatives:

| Ore name | Registered default |
| --- | --- |
| `helmetIron` | vanilla iron helmet |

If an IC2 input is missing, recipe registration logs an error instead of crashing the game. The
`helmetIron` entry is registered by CompactSolars and can be extended by other mods or pack scripts.

## IC2 parity status

The array generation rules now match IC2's solar generator:

- sun predicate: `skyLight > 4`, no-sky check, `canBlockSeeTheSky`, desert exception, rain/thunder
  check;
- linear `skyLight / 15` output scaling;
- IC2 `balance/energy/generator/solar` value by default;
- 128-tick sun refresh and an immediate refresh on load.

Remaining known differences:

- Hats use the array-rate output (8/64/512/2048 EU/t) rather than IC2's flat 1 EU/t; the
  chest-only, any-tier, ignore-transfer, and no-confusion behavior now matches IC2.

See [CODEBASE_ANALYSIS.md](CODEBASE_ANALYSIS.md) for the full review and the remaining planned work.

## World compatibility

All four tiers now share one tile entity class. The tier is persisted in NBT, and the old per-tier
tile entity IDs (`CompactSolarType.LV`, `CompactSolarType.MV`, `CompactSolarType.HV`,
`CompactSolarType.EV`) are registered as aliases of the unified tile entity, so existing worlds
should load their arrays at the correct tier. Back up a world before testing an upgrade from an
older build.

## Building

This project uses the GTNewHorizons 1.7.10 Gradle toolchain. The correct JDK is provisioned by
Gradle.

```bash
./gradlew build        # Windows: gradlew.bat build
./gradlew test         # unit tests
./gradlew runClient    # development client
```

Built jars are placed in `build/libs/`.

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for the
full text. CompactSolars-Legacy is derived from cpw's CompactSolars, which was also GPLv3.

## Credits

- Original CompactSolars by cpw, based on an idea by Zippinus
- 1.7.2 update by ProgWML6
- 1.7.10 maintenance and port by MrFuzzihead
