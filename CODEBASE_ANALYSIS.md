# CompactSolars-Legacy — Codebase Review and IC2 Solar Parity Analysis

**Reviewed tree:** `feature-CodebaseRefactor`, HEAD `20d3aa5` ("Clean up code")
**Target:** Minecraft 1.7.10, Forge 10.13.4.1614, IndustrialCraft² 2.2.828-experimental
**IC2 reference:** `ic2/core/block/generator/tileentity/TileEntitySolarGenerator.java` and
`ic2/core/block/generator/tileentity/TileEntityBaseGenerator.java`, decompiled from the
`industrialcraft-2:2.2.828-experimental:dev` jar used by this build.

> **Status note (batches 1-6):** Batch 1 implemented config/metadata crash guards, server-only tile
> updates, on-load sun refresh, charge-slot validation and shift-click fixes, recipe guards, and
> OreDictionary recipe inputs. Batch 2 implemented IC2's sun predicate and linear sky-light scaling,
> IC2's solar balance config, 128-tick refresh, two-panel storage, IC2's power-output tooltip format,
> and the metal block step sound. Batch 3 added solar-hat tooltips, a single infinite-durability hat
> material, consistent hat build-up decay, and localized GUI title/inventory labels. Batch 4 unified
> the three tile entities into one tier-NBT tile entity with legacy ID aliases, split recipe/hat
> registration out of the tier enum, removed the duplicate GUI enum and unused container state, and
> removed dead tier fields. Batch 5 replaced the `BasicSource` prefab with a direct `IEnergySource`
> for exact 16/128/1024 EU storage and added the blocks/hats to IC2's creative tab. Batch 6 added
> IC2-style wrench behavior (90% tier preservation, Basic Machine Casing fallback on non-wrench breaks/failed rolls)
> with a config switch. This document describes the state at the time of the review; check the
> current source and git history for what has landed.

Line references point at the current source files. The code is a faithful cleanup of the original
2014 CompactSolars source (the GPLv3 cpw implementation), so many findings are inherited rather than
introduced by the cleanup. This is a static review plus a formula-level simulation of the 1.7.10
sun/light math; no in-game profiling or runtime test was run.

---

## 0. Executive summary

The good news first: the project is small (~900 lines of Java), uses only the IC2 API
(`BasicSource`, `IElectricItem`, `IC2Items`, `IWrenchable`), and the tier/data model is easy to
follow. The bad news is that it is **not** currently an "exact array of IC2 solar panels" in any
tier, and there are several real bugs.

Highest-impact findings:

1. **The arrays do not match IC2 solar behavior over a full day.** At full noon they match
   (8/64/512 EU/t), but `isDaytime()` is `skylightSubtracted < 4` — effectively "sky light ≥ 12" —
   while IC2 generates from sky light > 4 and scales linearly with `skyLight / 15`. The arrays are
   completely off while IC2 still generates at dawn/dusk, and they ignore IC2's
   `balance/energy/generator/solar` config. A per-tick simulation of the 1.7.10 formulas gives
   **~92.4% of the equivalent IC2 panels per clear day** (a ~7.6% net *nerf*, not a buff), even
   though the arrays over-produce at sky light 12–14.
2. **`scaleFactor = 0` (or any negative value) crashes the server** with
   `IllegalArgumentException` from `Random.nextInt(0)` whenever a solar is in sunlight. There is no
   validation or config minimum.
3. **Metadata/damage values outside 0–2 cause crashes**: `/setblock ... 3`, `/give ... 3`, a
   corrupted item, or any wildcard-damage query hits `values()[damage]` in
   `CompactSolarType.makeEntity` / `ItemCompactSolar.getUnlocalizedName`.
4. **The charge slot and shift-click logic are broken.** `ContainerCompactSolar` never routes
   shift-clicked items into the machine slot, and the hotbar→main range is off by one (slot 27 is
   never a target). The machine slot is a plain `Slot`, so players can place any item in it; the
   inventory `isItemValidForSlot` check only applies to hoppers and does not check tier or fullness.
5. **The client tile entity overwrites the server-synced sun state with a stale value.** In 1.7.10
   `skylightSubtracted` is advanced in `WorldServer.tick`; the client world only calculates it at
   construction, so `world.isDaytime()` on the client is unreliable. The CS tile entity still runs
   its sun calculation on the client and writes `theSunIsVisible`, which is the field the GUI reads.
   The sun icon can be wrong or flicker even though the server sent the correct value.
6. **Hats diverge substantially from IC2's solar helmet** (all armor vs chest only, tier-limited vs
   `Integer.MAX_VALUE`, transfer-limited vs ignore-transfer, confusion effect, 11 durability vs
   infinite), and their `buildUp`/`lastTick` logic looks inverted/incorrect.
7. **The docs and licensing are wrong.** `README.md` is the README for a completely different mod
   (IC2Tweaks). Upstream CompactSolars is GPLv3 (the imported source headers said so and upstream's
   README says so), but this repository ships an MIT `LICENSE` with the headers removed. Resolve
   before distributing.
8. **Storage does not match an equivalent panel array.** IC2 panels store 2 EU each; an 8/64/512
   array should store 16/128/1024. CS stores 2 *packets* = 64/256/1024 (4×/2×/1×).
9. **Several smaller contract bugs**: `createNewTileEntity` returns `null`, recipes can crash on a
   null `IC2Items` result, the hat recipe uses a size-0 `ItemStack`, the charge slot does not mark
   the tile dirty, and the GUI is not localized.
10. **The code is easy to improve.** Most fixes are local; the tier enum should be split into data,
    registration, recipes, and rendering, and the sun logic should be shared by blocks and hats.

Severity legend used below: **S1** crash/data loss/legal, **S2** gameplay/parity, **S3** UI/UX,
**S4** robustness/maintainability, **S5** cosmetic/hygiene.

---

## 1. Codebase map

| File | Responsibility | Notes |
| --- | --- | --- |
| `CompactSolars.java` | `@Mod` entry point: config, block/item registration, recipes, server-stop cleanup | 83 lines; config is one static `int`; block is registered in `init`, items in `preInit` |
| `CompactSolarType.java` | Tier enum + recipes + TE factory + hat factory + textures/names | God enum: data, registration, recipes, reflection, and rendering keys |
| `BlockCompactSolar.java` | Block, icons, GUI opening, inventory drop | Hand-rolled drop code; `createNewTileEntity` returns null |
| `ItemCompactSolar.java` | `ItemBlockWithMetadata` subtype names and metadata clamping | No tooltip; unlocalized name is unbounded |
| `TileEntityCompactSolar.java` | Energy source, inventory, sun state, wrench behavior | MV/HV subclasses only pass a type; client/server logic mixed |
| `TileEntityCompactSolarMV.java` / `HV.java` | 8-line type carriers | Could be eliminated |
| `ContainerCompactSolar.java` | Slot layout, sun progress-bar sync, shift-click transfer | Generic slot; transfer ranges wrong |
| `client/GUISolar.java` | GUI + duplicate `GUI` enum | Hardcoded English; uses IC2's GUI texture |
| `client/ClientProxy.java` | GUI factory + empty render hooks | Empty overrides are dead code |
| `CommonProxy.java` | Server GUI factory + empty render hooks | Same |
| `ItemSolarHat.java` | Solar helmet, armor charging, confusion buildup, per-player state | Adds three armor materials; odd state machine |
| `resources/.../lang/*.lang` | Block/hat names | GUI titles/labels are not in lang files |
| `resources/mcmod.info` | Mod metadata | Raw newlines inside JSON strings; no logo |

---

## 2. Bugs

### 2.1 S1 — Crashes and legal issues

#### B1. `scaleFactor = 0` / negative crashes the server

`CompactSolars.java:45-47` reads the property with no validation; `TileEntityCompactSolar.java:56-57`
and `ItemSolarHat.java:47` then call `random.nextInt(CompactSolars.productionRate)`. Java's
`Random.nextInt(0)` and `nextInt(negative)` throw `IllegalArgumentException`. Because the call is
inside the server tick, a single config edit takes the server down as soon as a solar is in
sunlight.

**Fix:** clamp to `>= 1` at load, log a warning, and add a config minimum. Also guard against
someone setting the public static field to 0 from another mod.

```java
int raw = scale.getInt(1);
if (raw < 1) {
    LOG.warn("scaleFactor must be >= 1; clamping {} to 1", raw);
    raw = 1;
}
CompactSolars.productionRate = raw;
```

#### B2. Invalid metadata/damage causes `ArrayIndexOutOfBoundsException`

- `CompactSolarType.makeEntity(int metadata)` → `values()[solartype]` (`CompactSolarType.java:63-69`).
  `/setblock x y z compactsolars:CompactSolarBlock 3` will call `createTileEntity(world, 3)` and
  crash the server tick.
- `ItemCompactSolar.getUnlocalizedName(ItemStack)` → `values()[itemstack.getItemDamage()]`
  (`ItemCompactSolar.java:17-19`). A damage-3 item (`/give`, NBT editing, a wildcard query) crashes
  item tooltip rendering.
- `BlockCompactSolar.getIcon(int, int)` returns `null` for metadata > 2
  (`BlockCompactSolar.java:45-47`); a stale meta-3 block renders as a missing icon and can NPE in
  renderers that do not expect null.

**Fix:** a single `SolarTier.byMetadata(int)` that clamps/logs, precomputed unlocalized names, and a
non-null icon fallback.

#### B3. The README and license are inconsistent with the project and its upstream

- `README.md` is the README for **IC2Tweaks**, a different mod (global energy-net overload,
  `com.mrfuzzihead.ic2tweaks`, different config, different feature list). It does not describe
  CompactSolars at all.
- Upstream `cpw/compactsolars` states "licensed as GPL v3"; the imported 2014 sources carried
  GPLv3 headers. This repository ships an **MIT** `LICENSE` and the headers were removed in the
  cleanup. Unless the author has explicit permission to relicense, the derivative should remain
  GPLv3. This is a distribution/legal risk, not a code bug, but it belongs at S1.

**Fix:** rewrite the README for CompactSolars (features, tiers, config, recipes, parity notes),
restore the upstream GPLv3 license/headers or document the permission, and align the badge.

### 2.2 S2 — Gameplay and IC2-parity bugs

#### B4. Sun/light behavior is not IC2's (the main parity bug)

Current code (`TileEntityCompactSolar.java:68-72`):

```java
boolean isRaining = this.canRain && (this.worldObj.isRaining() || this.worldObj.isThundering());
this.theSunIsVisible = this.worldObj.isDaytime() && !isRaining
    && this.worldObj.canBlockSeeTheSky(this.xCoord, this.yCoord + 1, this.zCoord);
```

In 1.7.10 the vanilla overworld `WorldProvider.isDaytime()` is
`worldObj.skylightSubtracted < 4`. Since the top-of-world sky light is `15 - skylightSubtracted`,
that is effectively **sky light ≥ 12**. IC2's solar uses:

```java
return getskylight(world, x, z) > 4                    // sky light > 4
    && !world.provider.hasNoSky
    && world.canBlockSeeTheSky(x, y, z)
    && (biome instanceof BiomeGenDesert
        || (!world.isRaining() && !world.isThundering()));
```

and then scales output by `solarbasevalue * skylight / 15`. So the arrays:

- turn on much later at dawn and off much earlier at dusk than IC2 panels;
- produce a binary 0 or full output instead of a smooth 33%–100% ramp;
- ignore IC2's `balance/energy/generator/solar` config entirely;
- treat any 0-rainfall biome as "desert" for the rain exception (`getIntRainfall() > 0`), whereas IC2
  only exempts `BiomeGenDesert`. In vanilla that means savanna/savanna plateau/mesa/mesa plateau keep
  generating during global rain while IC2 panels stop;
- in a desert during global thunder, `isDaytime()` can become false (thunder dims the sky) while IC2's
  desert exception plus `skyLight > 4` still generates at reduced output; during global rain the
  desert sky light is 12/15, so IC2 produces 0.8× while CS (which ignores the skylight term) produces
  full output.

See §3 for the quantitative comparison and the exact formula to copy. `ItemSolarHat` duplicates this
sun/rain logic, so it inherits the same behavior; §3.7 covers the hat separately.

#### B5. Storage is 2 packets, not 2× the equivalent panels

`CompactSolarType.java:43` sets `maxStorage = outputPacketSize << 1`:

| Tier | Current CS storage | Equivalent IC2 panels | IC2 array storage | Ratio |
| --- | ---: | ---: | ---: | ---: |
| LV (8) | 64 | 8 | 16 | 4× |
| MV (64) | 256 | 64 | 128 | 2× |
| HV (512) | 1024 | 512 | 1024 | 1× |

`BasicSource` also enforces `capacity >= power`, so an exact 16-EU LV buffer cannot be represented
with `BasicSource` (it would become 32). This matters under backpressure (no sink, partially full
sink, or no cable): CS can power its nominal output for 8/4/2 ticks after the sun goes away, while
an IC2 array would have only 2 ticks of buffer at the same draw rate.

**Fix:** decide whether "array parity" (2×panels) or "packet smoothing" (2×packet) is the design
goal. For exact parity, either accept the LV `BasicSource` minimum of 32 or implement `IEnergySource`
directly; document the choice.

A related naming issue: `outputPacketSize` (32/128/512) is a **ceiling**, not the actual packet size.
`BasicSource.getOfferedEnergy()` returns `min(storage, power)`, and in steady state the array offers
its generation rate (8/64/512), so only HV actually fills a full tier packet each tick. If the intent
is "one full packet per tick", buffer 4 ticks for LV and 2 ticks for MV before offering. The config
comment's "whole packet (8, 64, 512 EU)" conflates output with packet size; 8 and 64 are not packet
sizes (32 and 128 are).

#### B6. Initial sun state is not updated on load, and the update interval differs

- `TileEntityCompactSolar` starts `theSunIsVisible = false` and only updates it when the private
  `tick` counter reaches 0; `tick` starts at `random.nextInt(64)` (`TileEntityCompactSolar.java:35,
  50-53`). On every chunk load, the array can be blind for up to 64 ticks (~3.2 s).
- IC2's `TileEntitySolarGenerator.onLoaded()` calls `updateSunVisibility()` immediately and then
  refreshes every `tickRate() = 128` ticks.
- CS refreshes every 64 ticks (twice as often). That is more responsive to weather but doubles the
  per-array sun-check cost and is not IC2 parity.

**Fix:** call `updateSunState()` on `validate()`/first tick; use 128 ticks unless deliberately
faster; keep the random offset to stagger arrays.

#### B7. The charge slot accepts anything and is not tier-aware

`ContainerCompactSolar.java:20` uses a plain `new Slot(inventory, 0, 80, 26)`. Vanilla `Slot`
defaults to `isItemValid(...) == true`, so a player can manually place dirt/stone/anything in the
charge slot. `TileEntityCompactSolar.isItemValidForSlot` (`:226-228`) only checks
`instanceof IElectricItem`, is not consulted by the generic slot, and does not check the array tier
or whether the item is already full. Hoppers therefore also insert full or wrong-tier electric
items, clogging the slot.

IC2's `TileEntityBaseGenerator` uses an `InvSlotCharge` with
`ElectricItem.manager.charge(stack, ∞, tier, true, true) > 0` (simulate, ignore transfer limit),
and exposes it as a top-side `ISidedInventory` slot.

**Fix:** a `SlotCharge` that delegates to the same simulate check with the array tier; implement
`ISidedInventory` if side parity matters; keep `isItemValidForSlot` consistent.

#### B8. Shift-click never inserts into the charge slot; hotbar→main misses slot 27

`ContainerCompactSolar.java:66-86`:

```java
if (i == 0) {
    mergeItemStack(itemstack1, 1, 37, true);      // machine -> player
} else if (i >= 1 && i < 28) {
    mergeItemStack(itemstack1, 28, 37, false);    // main -> hotbar only
} else if (i >= 28 && i < 37) {
    mergeItemStack(itemstack1, 1, 27, false);     // hotbar -> main, but 27 is excluded
}
```

- From any player slot, the machine slot is never a target. Shift-clicking a battery does **not**
  put it in the charge slot.
- Hotbar→main uses end-exclusive `27`, so main-inventory slot 27 is never used. If it is the only
  free slot, the transfer fails.
- The source ranges exclude the source region, so there is no duplication bug — just the missing
  machine target and the off-by-one.

**Fix:** use constants and try the machine slot first for player-origin stacks:

```java
private static final int MACHINE = 0, INV_START = 1, INV_END = 28, HOTBAR_START = 28, HOTBAR_END = 37;

if (i == MACHINE) {
    if (!mergeItemStack(stack, INV_START, HOTBAR_END, true)) return null;
} else if (i >= INV_START && i < INV_END) {
    if (!mergeItemStack(stack, MACHINE, MACHINE + 1, false)
        && !mergeItemStack(stack, HOTBAR_START, HOTBAR_END, false)) return null;
} else if (i >= HOTBAR_START && i < HOTBAR_END) {
    if (!mergeItemStack(stack, MACHINE, MACHINE + 1, false)
        && !mergeItemStack(stack, INV_START, INV_END, false)) return null;
}
```

`mergeItemStack` with the custom `SlotCharge` will reject invalid items and fall through to the
player ranges.

#### B9. Missing `markDirty()` after charging the slot

`TileEntityCompactSolar.java:62-64` calls `energySource.charge(inventory[0])` and ignores the
boolean result. `BasicSource.charge` modifies the item's NBT and `energyStored` but does not mark
the tile/chunk dirty. IC2's `TileEntityBaseGenerator` sets `needsInvUpdate` and calls
`func_70296_d()` (markDirty) when it charges. CS can therefore lag or miss persistence of the
charged item on saves/unloads (the chunk still saves periodically, so this is a promptness/correctness
issue rather than guaranteed data loss).

**Fix:**

```java
if (inventory[0] != null && inventory[0].getItem() instanceof IElectricItem
        && energySource.charge(inventory[0])) {
    markDirty();
}
```

#### B10. Hat behavior is not the solar helmet's, and the state machine is suspect

`ItemSolarHat.java:36` calls `EnumHelper.addArmorMaterial("COMPACTSOLARHAT", 1, {1,1,1,1}, 1)` **once
per item instance** (three times total). Each call creates a new enum constant via reflection; the
three hats use duplicate material names/values. It also means the hat's durability is material
durability 1 → `ItemArmor` sets 11, while IC2's solar helmet calls `setMaxDamage(0)` (infinite).
`ISpecialArmor.getArmorDisplay` returns 0 (matching IC2's zero protection), so the `{1,1,1,1}`
reduction values are misleading.

Behavior differences from IC2's `ItemArmorSolarHelmet`:

| Behavior | IC2 solar helmet | CS hat |
| --- | --- | --- |
| Charged slot | Chestplate only (`armorInventory[2]`) | All armor except the hat |
| Rate | 1 EU/t | Array output (8/64/512 EU/t), no buffer |
| Tier | `Integer.MAX_VALUE` (any tier) | 1/2/3 (hat tier) |
| Transfer limit | Ignored (`ignoreTransferLimit = true`) | Honored (`false`) |
| Sun check | IC2's exact `isSunVisible` | CS `isDaytime()` (same parity bug) |
| Confusion | None | Build-up when `available <= 0` |
| Durability | 0/infinite | 11 (material durability) |
| Recipe | Iron helmet + 1 solar panel + cables | Iron helmet + one whole array |
| Creative tab | IC2 tab (via IC2 item) | Combat (ItemArmor default) |

The `buildUp` logic at `ItemSolarHat.java:66-88` increments when all available energy was absorbed
by armor, and decrements when some energy was unused (full/no armor). That is the opposite of a
"storage overflow" mechanic; if the intent was "confusion when the hat cannot dump its energy", it is
inverted. In addition, `state.lastTick` is only updated inside the sun-visible branch, so time spent
underground/night is either ignored or included inconsistently depending on whether armor had room.
The dose scale is uneven: `IntMath.pow(10, ordinal) * 5` gives 5/50/500, while the per-tick increment
is 1/2/4, so the time to trigger is ~5/25/125 ticks and the confusion duration (`dose >> 2`) is
1/12/125 ticks. LV is almost invisible; HV is a 6.25 s effect. The entire method is also behind the
`productionRate` gate, so the `% 20 == 0` rain-cache update can be skipped for many ticks at high
rates and the hat can generate in rain until a gated tick happens to land on a multiple of 20.
`EnumHelper.addArmorMaterial` is reflective enum extension; on modern Java (17+) it can require
additional JVM module access, so it is a compatibility risk as well as a startup cost.

**Fix:** use one static `ArmorMaterial`; `setMaxDamage(0)`; decide the mechanic (overflow vs
"charging causes sickness") and document it; update `lastTick` every tick (or decay before the branch);
update the rain cache outside the production gate; make the effects configurable; test the three tiers.

#### B11. Recipe registration can crash on a missing IC2 item

`CompactSolarType.java:46-54` calls `IC2Items.getItem("solarPanel")` and
`IC2Items.getItem(transformerName)` and passes them straight to `GameRegistry.addRecipe`. The API
returns `null` and logs on failure, so a missing/renamed IC2 item causes an NPE during post-init
(world load). Add null checks and skip/log the affected recipe instead of crashing.

#### B12. The hat recipe uses a size-0 `ItemStack`

`CompactSolarType.java:96-98`:

```java
ItemStack solarBlock = new ItemStack(block, 0, typ.ordinal());
GameRegistry.addShapelessRecipe(new ItemStack(typ.item), solarBlock, ironHat);
```

Vanilla `ShapelessRecipes.matches` only compares item/damage, so the recipe does work, but a
size-0 stack is invalid input for NEI/recipe viewers and for anything that inspects the recipe list.
Use size 1.

#### B13. `createNewTileEntity` returns `null`

`BlockCompactSolar.java:37-39` returns `null`; `BlockCompactSolar` instead overrides
`createTileEntity` (`:41-43`). Vanilla's `Block.createTileEntity` falls back to
`ITileEntityProvider.createNewTileEntity`, but because `createTileEntity` is overridden this
particular path is not hit today. It is still the wrong contract: any mod/tool that calls
`createNewTileEntity` directly (block placement helpers, world editors, future MC/Forge code) gets
`null`, and a later refactor that removes the `createTileEntity` override will break tile creation.

**Fix:** implement `createNewTileEntity(World, int)` and have `createTileEntity` delegate to it (or
remove the latter and register only the provider method).

### 2.3 S3 — UI/UX and client/server

#### B14. Client TE overwrites the server-synced sun state with a stale value

`TileEntityCompactSolar.updateEntity` runs on both sides. It calls `updateSunState()` (which uses
`world.isDaytime()`) and writes the public `theSunIsVisible`. The GUI reads that same field on the
client (`GUISolar.java:37`). The server sends the correct value via
`ContainerCompactSolar.detectAndSendChanges`/`updateProgressBar`, but the client's own update
overwrites it every 64 ticks.

Why the client value is stale: in 1.7.10, `skylightSubtracted` is updated in `WorldServer.tick`;
`WorldClient` only calls `calculateInitialSkylight()` at construction. `WorldProvider.isDaytime()`
therefore reflects the client's world-load time, not the current time. The client sun calculation is
wrong for most of the day/night cycle, and the GUI sun icon can be stuck or flicker.

**Fix:** make the tile entity server-only for generation and sun state:
`if (worldObj == null || worldObj.isRemote) return;` at the top of `updateEntity` (and do not call
`BasicSource.onUpdateEntity` on the client). Let the container progress bar be the only client
source of `theSunIsVisible`. Alternatively, separate `serverSunVisible` from a synced client field.

#### B15. GUI is hardcoded English and lacks useful information

`GUISolar.java:27-28`:

```java
drawString(this.type.mainType.friendlyName, 8, 6, 4210752);
drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);
```

IC2 uses `StatCollector.translateToLocal("ic2.SolarGenerator.gui.name")`. CS's `friendlyName`
(`CompactSolarType.java:21`) is a hardcoded English string, and "Inventory" is not localized at all.
There is also no tier/EU/tooltip information; IC2's solar GUI is equally minimal (sun icon only), so
the *lack* of an energy gauge matches IC2, but the title/label localization does not.

**Fix:** `StatCollector.translateToLocal(tier.translationKey())` and
`StatCollector.translateToLocal("container.inventory")`; add lang keys for all tiers.

#### B16. No item tooltip while IC2 shows one

IC2's `ItemGenerator` adds a tooltip like "Power output: 1 EU/t max". CS's `ItemCompactSolar` has no
`addInformation`, so users cannot see the tier, output, storage, or panel count without consulting
the wiki/recipe. Add a tooltip (EU/t at full sun, tier/packet, buffer, equivalent panels, and a note
that output scales with sky light once parity is fixed).

#### B17. Hats appear in the Combat tab, blocks in Redstone; no mod/IC2 tab

`BlockCompactSolar` uses `CreativeTabs.tabRedstone` (`:28`), while `ItemArmor`'s constructor puts the
hats in `CreativeTabs.tabCombat`. IC2's generator block and solar helmet are in IC2's tab. This is
cosmetic but inconsistent and makes the mod harder to find.

**Fix:** a `CreativeTabs` for CompactSolars (with a tier icon) or use IC2's tab if the API allows it.

### 2.4 S4 — Robustness, NBT, and smaller functional issues

- **`BlockCompactSolar.getIcon` can return null** for out-of-range meta and can NPE if `textures` is
  null (server-side call or before icon registration). Add bounds/fallback.
- **`breakBlock` casts blindly** to `TileEntityCompactSolar` (`BlockCompactSolar.java:77-84`). A
  stale/different TE at the position causes `ClassCastException`; use `instanceof`.
- **`dropContent(int newSize, ...)` has a dead `newSize` parameter** and duplicates the standard
  inventory-drop loop (1.7.10 has no `InventoryHelper.dropInventoryItems`; that class is 1.8+). The
  hand-rolled code is ~40 lines and has to copy NBT manually; extract a small `InventoryDrops`
  helper or use a tested utility instead.
- **`getContents()`** returns the internal `ItemStack[]` and is unused; remove it or return a copy.
- **`TileEntityCompactSolar` uses deprecated `BasicSource` wrappers** (`onUpdateEntity`, `onInvalidate`,
  `onReadFromNbt`, `onWriteToNbt`). They work in 2.2.828 but are compatibility shims; consider the
  non-deprecated lifecycle methods or a direct `IEnergySource` implementation for forward compatibility.
- **`getInventoryName()` returns the enum name** (`LV`/`MV`/`HV`) and `hasCustomInventoryName()` is
  false; integrations that display the inventory name get an unlocalized short string. Use the
  tier's localized name or a translation key.
- **`getStackInSlotOnClosing` does not `markDirty()`**; minor.
- **`isItemValidForSlot` does not check tier/fullness** (see B7).
- **`CompactSolarType.makeEntity` uses reflection (`clazz.newInstance()`)** per placement and wraps
  failures in `Throwables.propagate`; a switch/factory is faster and clearer, and `newInstance()` is
  deprecated on modern Java.
- **`CompactSolarType` exposes mutable public fields** (`clazz`, `friendlyName`, `transformerName`);
  make them final/private. Adding a tier in the middle of the enum would shift `ordinal()` used as
  metadata, texture row, GUI id, and electrical tier; use explicit metadata/tier fields.
- **`TileEntityCompactSolar` uses `type.ordinal() + 1` as the electrical tier**; if the enum order
  ever changes, the voltage changes with it. Same for `outputPacketSize`.
- **`productionRate` is a public mutable static** used by both blocks and hats; no config reload, no
  validation, no way to disable the random gate without splitting it from IC2's solar base.
- **`CompactSolars` saves the config in `finally` even when load failed**, potentially writing an
  empty/default config over a malformed one. Prefer only saving on success, and fall back to
  defaults with a clear log instead of throwing during pre-init.
- **`FMLLog` is deprecated**; use a Log4j logger (`LogManager.getLogger("CompactSolars")`).
- **`mcmod.info` contains literal newlines inside JSON strings.** Forge 1.7.10's lenient Gson parser
  accepts it today, but strict JSON tools (and potentially future parsers) reject it. Use `\n`.
- **No tests** despite `dependencies.gradle` pulling JUnit 5 and `build.gradle.kts` enabling the JUnit
  platform. Add unit tests for the pure logic (tier/meta mapping, sun/light math, config clamp) or
  remove the unused test dependencies.

### 2.5 S5 — Cosmetic / hygiene

- `.github/scripts/update_version` still refers to `build.gradle` (this project uses
  `build.gradle.kts` and git-derived versioning); it appears unused. Remove or update.
- `jitpack.yml` runs `./gradlew setupCIWorkspace`; verify that task exists in the current GTNH
  convention (it is an old GTNH task name).
- `gradle.properties` sets `autoUpdateBuildScript = true`; this can change the build script
  unexpectedly between builds. Consider pinning for reproducibility.
- Unused assets: `textures/block_textures.png` and `textures/item_textures.png` are source sheets,
  not loaded by the mod. Remove from the shipped jar or keep them outside `resources`.
- `modId = CompactSolars` is mixed case; harmless in 1.7.10, but lower-case mod ids are the modern
  convention.
- Tile entity ids contain dots (`CompactSolarType.LV`); valid but unconventional. Prefix with the
  mod id/lower-case names for clarity.
- `BlockCompactSolar` does not set a metal step sound; IC2's generator does. Add
  `setStepSound(Block.soundTypeMetal)` for parity.
- `getTextureRow()` is unused; `GUISolar.GUI` duplicates `CompactSolarType`; `ContainerCompactSolar`
  has unused `myPlayer`/`getPlayer()` and an unused `type` parameter in `layoutContainer`.
- `CommonProxy`/`ClientProxy` empty render hooks and overrides are dead code.
- Missing `@Override` on most interface methods, inconsistent `this.`, decompiler-style `i/j/k`,
  `par1`, `var1`, `f1` names; see §6.

---

## 3. IC2 solar parity analysis

### 3.1 Reference behavior (IC2 2.2.828)

IC2's `TileEntitySolarGenerator`:

```java
public TileEntitySolarGenerator() {
    super(1, 1, 2);              // production=1, tier=1, maxStorage=2
    this.ticker = IC2.random.nextInt(tickRate()); // 128
    this.solarbasevalue = ConfigUtil.getDouble(MainConfig.get(),
        "balance/energy/generator/solar"); // default 1.0
}

public void onLoaded() {
    super.onLoaded();
    updateSunVisibility();       // immediate on load
}

public boolean gainEnergy() {
    if (ticker++ % tickRate() == 0) updateSunVisibility();
    if (sunIsVisible) {
        storage += solarbasevalue * (getskylight(world, x, z) / 15.0F);
        return true;
    }
    return false;
}

private static int getskylight(World world, int x, int z) {
    return world.getBlockLightValue(x, 255, z); // top-of-world sky light
}

public static boolean isSunVisible(World world, int x, int y, int z) {
    return getskylight(world, x, z) > 4
        && !world.provider.hasNoSky
        && world.canBlockSeeTheSky(x, y, z)
        && (world.getWorldChunkManager().getBiomeGenAt(x, z) instanceof BiomeGenDesert
            || (!world.isRaining() && !world.isThundering()));
}

public int tickRate() { return 128; }
```

Key consequences:

- Output per panel = `solarbasevalue * skyLight / 15`, a **double**, only when `skyLight > 4` and the
  other conditions hold.
- Sky light is taken at y=255, so the skylight term is essentially time-of-day + weather; roof/leaf
  occlusion is handled by `canBlockSeeTheSky`.
- `onLoaded` updates sun immediately; the periodic refresh is 128 ticks.
- Storage is 2 EU (about two ticks of generation), not a packet buffer.
- Tier is 1 and packet power is 32 for every IC2 solar panel.
- The charge slot is tier 1, accepts only items that can accept charge at tier 1 (simulated with
  infinite amount/ignore transfer limit), and is a top-side `ISidedInventory` slot.
- The GUI is localized ("Solar Panel") and draws a sun icon; it has no energy gauge.
- `getWrenchDropRate()` is 0.9; breaking without a wrench drops a basic Generator item (block
  metadata 0) via `damageDropped`, not the solar panel (unless `balance/ignoreWrenchRequirement` is on).
- Block material iron, hardness 3.0, metal step sound, IC2 creative tab; item tooltip shows
  "Power output: 1 EU/t max".

### 3.2 Current CS behavior

```java
// TileEntityCompactSolar.updateEntity
if (tick-- == 0) { updateSunState(); tick = 64; }

int energyProduction = 0;
if (theSunIsVisible
    && (productionRate == 1 || random.nextInt(productionRate) == 0)) {
    energyProduction = type.getOutput(); // 8/64/512
}
energySource.addEnergy(energyProduction);
if (inventory[0] != null && inventory[0].getItem() instanceof IElectricItem) {
    energySource.charge(inventory[0]);
}

// updateSunState
boolean isRaining = canRain && (world.isRaining() || world.isThundering());
theSunIsVisible = world.isDaytime() && !isRaining
    && world.canBlockSeeTheSky(x, y + 1, z);
```

Differences from IC2 in one sentence: **the arrays use a binary "is it day?" test at a much higher
light threshold, have no skylight scaling and no IC2 config, update on a different cadence, have a
larger buffer in LV/MV, and differ in slot/UI/wrench/block conventions.** The tier/packet differences for MV
and HV are intentional; everything else should be uniform but is not.

### 3.3 Data/behavior comparison

`skyLight` is the top-of-world `getBlockLightValue(x, 255, z)` (0–15). "IC2 equivalent" means N
separate IC2 solar panels, each tier 1/packet 32, with the default `solarbasevalue = 1.0`.

| Metric | LV: 8 IC2 panels | CS LV | MV: 64 IC2 panels | CS MV | HV: 512 IC2 panels | CS HV |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Panels represented | 8 | 8 | 64 | 64 | 512 | 512 |
| Full-sun output (EU/t) | 8 | 8 | 64 | 64 | 512 | 512 |
| Output at sky 12 (EU/t) | 6.4 | **8** | 51.2 | **64** | 409.6 | **512** |
| Output at sky 11 (EU/t) | 5.87 | **0** | 46.9 | **0** | 375.5 | **0** |
| Output at sky 5 (EU/t) | 2.67 | **0** | 21.3 | **0** | 170.7 | **0** |
| Output at sky 4 (EU/t) | 0 | 0 | 0 | 0 | 0 | 0 |
| Tier | 1 (each) | 1 | 1 (each) | 2 | 1 (each) | 3 |
| Packet power | 32 (each) | 32 | 32 (each) | 128 | 32 (each) | 512 |
| Storage (EU) | 16 | 64 | 128 | 256 | 1024 | 1024 |
| Storage vs IC2 array | 1× | 4× | 1× | 2× | 1× | 1× |
| Sun predicate | `sky>4 && …` | `isDaytime` (sky≥12) + rain | same as LV | same as LV | same as LV | same as LV |
| Sky-light scaling | linear | none | linear | none | linear | none |
| IC2 solar config | honored | ignored | honored | ignored | honored | ignored |
| Sun refresh | on load + 128 t | 64 t, no on-load | same | same | same | same |
| Charge slot tier | 1 | 1 | 1 | 2 | 1 | 3 |
| Charge slot filter | chargeable @ tier 1 | none (manual) / `IElectricItem` (hopper) | same | none | same | none |
| GUI title | localized | hardcoded English | hardcoded | hardcoded | hardcoded | hardcoded |
| Wrench drop | 0.9, wrench required | 1.0, any tool | same | same | same | same |
| Block step/tab | metal / IC2 | stone / Redstone | same | same | same | same |
| Item tooltip | yes | no | no | no | no | no |
| Client sun logic | server + network GUI | client TE runs stale logic | same | same | same | same |

### 3.4 Why the daily total is lower, not higher

`WorldProvider.isDaytime()` is `skylightSubtracted < 4`, which means sky light 12–15. IC2 generates
from sky light 5–15 and scales linearly. Therefore:

- **sky 12–15:** CS is on at full output; IC2 is at 80%–100%. CS over-produces by up to 25%.
- **sky 5–11:** CS is off; IC2 produces 33%–73%. CS loses this entire window.
- **sky ≤ 4:** both off.

For a vanilla overworld with clear weather, no obstruction, and all generated energy exported (no
charge-slot item), simulating the 1.7.10
`calculateCelestialAngle` / `getSunBrightnessFactor` formulas over 24,000 ticks gives the table
below. The per-tick model used `angle = provider.calculateCelestialAngle(t, 1.0F)`,
`factor = getSunBrightnessFactor(angle)` (clear weather), `subtract = (int)((1 - factor) * 11)`, and
`sky = 15 - subtract`; CS was on when `subtract < 4`, IC2 when `sky > 4` and scaled by `sky / 15`.

| Equivalent array | IC2 EU/day | CS EU/day | CS / IC2 | Difference |
| --- | ---: | ---: | ---: | ---: |
| 1 panel | 14,153.8 | 13,081.0 | 92.4% | −7.6% |
| LV (8) | 113,230.4 | 104,648 | 92.4% | −8,582 EU/day |
| MV (64) | 905,843.2 | 837,184 | 92.4% | −68,659 EU/day |
| HV (512) | 7,246,745.6 | 6,697,472 | 92.4% | −549,274 EU/day |

The exact crossover times are approximately:

- CS on: ~0–12,539 and ~23,459–23,999.
- IC2 on: ~0–13,668 and ~22,330–23,999.
- CS is off for roughly 1,129 ticks at each end while IC2 still generates.

In clear weather, CS's on-window is a **strict subset** of IC2's: it never generates when IC2 is off,
but it is off for roughly 2,258 ticks per day while IC2 is still producing at a partial rate.

This is an ideal per-tick model; CS's 64-tick refresh (and IC2's 128-tick refresh) add small timing
noise, but the threshold and the missing linear scaling dominate. **Under default settings, a CS
array is a net nerf versus the same number of IC2 panels, not a buff** — despite matching at noon.
Changing IC2's solar config makes the mismatch arbitrarily large in either direction.

### 3.5 Per-tier verdict

#### LV — 8 panels, tier 1, packet 32

- **Matches at full sun:** 8 EU/t, and it is the same electrical tier/packet as an IC2 panel.
- **Closest to a true array:** the only intentional difference should be aggregation into one block.
- **Current divergences:** sun threshold/scaling, IC2 config, 4× storage, generic/unrestricted slot,
  64 vs 128 tick refresh and no on-load refresh, 1.0 vs 0.9 wrench rate and no wrench requirement,
  no tooltip, stone step sound, Redstone tab, client stale sun state.
- **Verdict:** *not* an exact 8× IC2 panel today; with the sun/formula/storage/slot/UI fixes it would
  be.

#### MV — 64 panels, tier 2, packet 128

- **Matches at full sun:** 64 EU/t = 64 panels.
- **Intentional differences:** tier 2/128 packet (higher voltage), charge slot tier 2.
- **Current divergences:** all LV sun/config issues, 2× storage, unrestricted slot (so a tier-1 or
  full item can occupy it), hardcoded GUI, no tooltip, wrench/block conventions, client state.
- **Verdict:** the generation/tier design is coherent, but the behavior is not uniform with IC2.

#### HV — 512 panels, tier 3, packet 512

- **Matches at full sun:** 512 EU/t = 512 panels; generation exactly fills one HV packet per tick.
- **Intentional differences:** tier 3/512 packet, charge slot tier 3.
- **Current divergences:** sun/config, unrestricted slot, GUI/tooltip/wrench/block, client state.
- **Storage is exactly 2×512 = 1024**, so HV is the only tier whose buffer matches an IC2 array.
- **Verdict:** the closest to an "N panels, but higher voltage" block at noon; still not uniform in
  sun/weather/config/UI/slot behavior.

### 3.6 Exact-parity checklist (what "the same, but higher voltage/generation" requires)

1. **Use IC2's sun predicate**, not `isDaytime`:

   ```java
   int sky = world.getBlockLightValue(x, 255, z);
   boolean sun = sky > 4
       && !world.provider.hasNoSky
       && world.canBlockSeeTheSky(x, y + 1, z)
       && (world.getWorldChunkManager().getBiomeGenAt(x, z) instanceof BiomeGenDesert
           || (!world.isRaining() && !world.isThundering()));
   ```

2. **Scale with sky light and IC2's config**, in `double`:

   ```java
   double euPerTick = sun ? panels * ic2SolarBase * (sky / 15.0) : 0.0;
   ```

   `ic2SolarBase` should come from IC2's `balance/energy/generator/solar` (default 1.0) or from a
   documented mirror config. `BasicSource.addEnergy(double)` accepts fractional EU, so no integer
   accumulator is needed.

3. **Match the refresh contract:** update on load and every 128 ticks (or document a deliberate 64).
4. **Match storage semantics:** 2×panels (16/128/1024) for literal array parity, or 2×packet if the
   goal is packet smoothing. Document the choice; `BasicSource` cannot represent LV's 16 exactly
   (minimum 32), so exact LV storage needs a direct `IEnergySource`.
5. **Match the charge slot contract:** one `SlotCharge` with
   `ElectricItem.manager.charge(stack, ∞, tier, true, true) > 0`; optionally top-side
   `ISidedInventory`; mark dirty after a successful charge.
6. **Match the GUI contract:** localized title; sun icon from the server; optionally add tier/EU
   tooltip. IC2's solar GUI has no energy gauge, so omitting one is parity, but adding one is a
   useful feature.
7. **Match machine conventions:** wrench rate/requirement, metal step sound, tooltip, creative tab,
   `damageDropped`/wrench drop, active state. These are small but visible.
8. **Keep generation server-only**; never let the client TE overwrite `theSunIsVisible`.
9. **Keep the intentional differences:** `panels` = 8/64/512, tier = 1/2/3, packet = 32/128/512,
   charge-slot tier = array tier. Everything else should be identical.
10. **Add a parity test** that computes IC2's per-panel daily total and CS's array total over 24,000
    ticks and asserts the expected ratio (1.0 once fixed, with a small tolerance for refresh timing).

### 3.7 Hats vs IC2's solar helmet

IC2's `ItemArmorSolarHelmet` is a small, focused item: if the chestplate is present and
`TileEntitySolarGenerator.isSunVisible(player.world, floor(x), floor(y)+1, floor(z))`, it charges the
chestplate with `ElectricItem.manager.charge(chest, 1.0, Integer.MAX_VALUE, true, false)` and calls
`player.openContainer.detectAndSendChanges()`. It has no confusion mechanic, no skylight scaling,
and infinite durability.

CS's hat is a different feature: an array-rate charger for all armor pieces, with tier/transfer-limit
gating and a confusion penalty. That can be a legitimate design, but it should be documented as a
divergence, and the state machine should be fixed. If parity is the goal, add a config such as
`hatBehavior = ARRAY | IC2` where `IC2` charges only the chest at 1 EU/t with tier
`Integer.MAX_VALUE` and ignores transfer limits.

---

## 4. Optimizations

| Optimization | Where | Why |
| --- | --- | --- |
| Cache `CompactSolarType[] VALUES = values()` | `BlockCompactSolar.getIcon`, `ItemCompactSolar.getMetadata/getUnlocalizedName`, `CompactSolarType` loops | `values()` clones the array on every call; `getIcon`/`getUnlocalizedName` are hot render paths |
| Precompute unlocalized names/translation keys | `ItemCompactSolar`, `CompactSolarType` | Avoid `String` concatenation per item render/tooltip |
| Skip all work on the client | `TileEntityCompactSolar.updateEntity` | Generation/sun logic is server-only; also fixes B14 and saves CPU for large solar farms |
| Refresh sun every 128 ticks and on load | `TileEntityCompactSolar` | Halves the per-array sun-check cost and matches IC2; keep the random offset |
| Use `world.rand` instead of a new `Random` | `BlockCompactSolar` | Avoids an extra RNG object; no statistical need for a dedicated stream |
| Replace reflection in `makeEntity` | `CompactSolarType` | A switch on explicit metadata is faster and clearer; supports bounds handling |
| Extract a local `InventoryDrops` helper | `BlockCompactSolar.breakBlock` | Removes ~40 lines of hand-rolled drop code and handles stacks/NBT consistently (1.7.10 has no vanilla inventory helper) |
| Precompute hat constants | `ItemSolarHat` | `IntMath.pow(2, ordinal)`/`IntMath.pow(10, ordinal)*5` per tick → static per-tier values |
| Cache hat sun state | `ItemSolarHat` | `canBlockSeeTheSky` per player per tick can be reduced to every 20–64 ticks with a small state object |
| Use a single `ArmorMaterial` | `ItemSolarHat` | Three reflective enum additions → one; less startup work and less risk |
| Consider direct `IEnergySource` | `TileEntityCompactSolar` | Removes the extra `BasicSource` TileEntity, the min-capacity clamp, and gives exact storage control; slightly more code |
| Avoid `addEnergy(0)` | `TileEntityCompactSolar` | Trivial, but avoids a call/client-side check on every dark tick |
| Use `Locale.ROOT` for `toLowerCase` | `BlockCompactSolar.registerBlockIcons` | Locale-independent texture names (not currently broken for LV/MV/HV, but correct) |
| Cache the GUI title string | `GUISolar` | `drawString` runs every frame; cache the localized string in the constructor |

Profile note: with thousands of arrays, the dominant cost is the per-tick energy-net/`BasicSource`
update and the per-tick charge-slot call, not the sun check. Skipping client updates is the largest
easy win; batching sun updates globally is possible but adds complexity and is not necessary for
normal bases.

---

## 5. Features worth adding

**Parity / balance**

- `matchIc2Solar` (default true): use IC2's sky-light formula and `balance/energy/generator/solar`.
- `solarBaseValue` mirror config (default 1.0) for packs that do not want to read IC2 internals.
- `generationInterval` / renamed `scaleFactor` with a minimum of 1; separate from output multiplier.
- `storageMode = PANEL_ARRAY | PACKET_BUFFER` (2×panels vs 2×packet).
- `wrenchRequired` and `wrenchDropRate` (IC2: 0.9/required) config.
- `chargeSlotTier = ARRAY | ONE` (array tier vs IC2's tier 1).
- `hatBehavior = ARRAY | IC2`; `hatCausesConfusion` toggle; `hatChargesAllArmor` toggle.

**UI / integration**

- Tooltips for arrays and hats: EU/t at full sun, tier/packet, buffer, equivalent panels, "scales
  with sky light", hat charge behavior.
- Localized GUI title/inventory label; optional energy/tier line.
- WAILA (or another 1.7.10 tooltip mod) integration: sun visible, current output, buffer, tier.
- NEI recipe handlers for arrays/hats (standard shaped/shapeless recipes are picked up, but a
  custom handler can show the panel-equivalent count).
- Comparator output based on buffer fill (useful for automation; not IC2 parity).
- A mod creative tab with a tier icon.

**Gameplay / compatibility**

- Config to disable/enable recipes, or use a cheaper hat recipe (iron helmet + solar panel) for
  parity with IC2's solar helmet.
- Optional buffer for the hat so high-tier hats do not waste energy when armor transfer limits are
  low.
- `ISidedInventory` for the charge slot (top-only) to match IC2 machine automation.
- Optional active/visual state (texture or light) when generating; IC2's solar doesn't change
  texture, so keep it optional.
- Data migration/`FMLMissingMappingsEvent` handling if upgrading from old block IDs.

---

## 6. Readability and rewrite plan

### 6.1 Problems with the current arrangement

- `CompactSolarType` is a god enum: tier data, block metadata, TE class, texture keys, item factory,
  recipes, and hat registration all live in one 101-line file. A change to recipes touches the same
  file as the voltage data.
- `TileEntityCompactSolar` mixes four concerns: energy net, inventory, sun/weather, and wrench
  behavior. It also carries client logic.
- `GUISolar.GUI` duplicates `CompactSolarType`; adding a tier requires editing two enums and the
  block/item/TE registration.
- `TileEntityCompactSolarMV`/`HV` are 8-line boilerplate that only pass a constructor argument.
- Magic numbers are everywhere: 64-tick sun interval, 80/26 slot position, 84/142 inventory Y,
  4210752 text color, 176,0 sun icon, 0.8/0.1 drop offsets, `dose >> 2`, `10^ordinal * 5`.
- Decompiler-style names (`i`, `j`, `k`, `par1`, `var1`, `f1`) and missing `@Override` make the code
  look older than it is.
- Dead code (`getContents`, `getPlayer`, `getTextureRow`, `newSize`, empty proxy hooks) adds noise.
- No tests; no clear separation between pure logic (sun math, tier data, config) and Minecraft
  plumbing.

### 6.2 Target package tree

```
com.mrfuzzihead.compactsolars
├── CompactSolars.java                 // @Mod: lifecycle only
├── CompactSolarsConfig.java           // load/validate; typed getters
├── registry/
│   ├── ModBlocks.java                 // block registration
│   ├── ModItems.java                  // item/hat registration
│   ├── ModTileEntities.java           // one TE registration (or three)
│   └── ModRecipes.java                // array + hat recipes; null checks
├── solar/
│   ├── SolarTier.java                 // immutable data: meta, tier, panels, output, packet, storage, keys
│   ├── SolarArrayBlock.java           // block behavior, icons, drops
│   ├── SolarArrayItem.java            // ItemBlock, names, tooltip
│   ├── SolarArrayTileEntity.java      // energy + inventory + sun state
│   ├── SolarArrayContainer.java       // slots, transfer, sync
│   ├── SlotCharge.java                // slot validity
│   └── SolarSun.java                  // shared IC2-parity sun/skylight math
├── hat/
│   ├── SolarHatItem.java
│   └── SolarHatState.java
├── client/
│   ├── ClientProxy.java
│   └── SolarArrayGui.java
└── util/
    └── InventoryDrops.java            // optional helper
```

If you want to keep the three TE classes for registry simplicity, keep them, but move all behavior
into the base class and make the subclasses trivial. If you want to eliminate them, register one
`SolarArrayTileEntity` and persist the tier in NBT (the no-arg constructor is used on load, so the
tier cannot come from the constructor alone). A single TE + tier NBT is cleaner but requires a small
data migration; three subclasses + one base is the lower-risk incremental step.

### 6.3 Class-by-class recommendations

**`SolarTier` (replaces `CompactSolarType`)**

```java
public enum SolarTier {
    LV(0, 1, 8, 32, 8, "lv"),
    MV(1, 2, 64, 128, 64, "mv"),
    HV(2, 3, 512, 512, 512, "hv");

    private static final SolarTier[] VALUES = values();

    public final int metadata;
    public final int tier;
    public final int panels;
    public final int packetSize;
    public final int fullSunOutput;
    public final String textureKey;

    SolarTier(int metadata, int tier, int panels, int packetSize, int fullSunOutput, String textureKey) {
        this.metadata = metadata;
        this.tier = tier;
        this.panels = panels;
        this.packetSize = packetSize;
        this.fullSunOutput = fullSunOutput;
        this.textureKey = textureKey;
    }

    public static SolarTier byMetadata(int meta) {
        for (SolarTier t : VALUES) if (t.metadata == meta) return t;
        return LV; // or throw with a clear log
    }

    public String translationKey() { return "tile.compactsolars:" + name() + "_block.name"; }
    public int panelArrayStorage() { return panels * 2; }
    public int packetBufferStorage() { return packetSize * 2; }
}
```

Benefits: explicit metadata/tier (no ordinal fragility), no registration/recipe/TE concerns, easy
unit tests.

**`SolarSun` (shared by block and hat)**

```java
public final class SolarSun {
    public static final int MIN_SKY_LIGHT = 5;

    public static int skyLight(World world, int x, int z) {
        return world.getBlockLightValue(x, 255, z);
    }

    public static boolean isSunVisible(World world, int x, int y, int z) {
        return skyLight(world, x, z) > 4
            && !world.provider.hasNoSky
            && world.canBlockSeeTheSky(x, y + 1, z)
            && (world.getWorldChunkManager().getBiomeGenAt(x, z) instanceof BiomeGenDesert
                || (!world.isRaining() && !world.isThundering()));
    }

    public static double perPanelOutput(int skyLight, double ic2SolarBase) {
        return skyLight > 4 ? ic2SolarBase * skyLight / 15.0 : 0.0;
    }
}
```

Testing this class is straightforward: `perPanelOutput(4, 1) == 0`, `(5,1) == 1/3`,
`(15,1) == 1`, etc. It also makes the parity formula obvious to reviewers.

**`SolarArrayTileEntity`**

- Add `if (worldObj == null || worldObj.isRemote) return;` at the top of `updateEntity`.
- Use `double energyProduction` and `BasicSource.addEnergy(double)`.
- Refresh sun on `validate()`/first tick and every 128 ticks.
- Keep `canRain`/`noSunlight` cached, but compute `canRain` from the same predicate as IC2 if parity
  is desired (`instanceof BiomeGenDesert`; note that `canRain` is no longer a simple rainfall check).
- Use a `double` `energyStored`; if you keep `BasicSource`, do not try to force exact 16 EU for LV.
- `if (energySource.charge(inventory[0])) markDirty();`
- Consider implementing `IEnergySource` directly if exact storage/control is required; otherwise
  `BasicSource` is fine and is the official prefab.

**`SolarArrayContainer` / `SlotCharge`**

- Use `SlotCharge` with the simulate check.
- Use named slot constants and a correct transfer algorithm (see B8).
- Consider `ISidedInventory` for top-only charge access.
- Keep the sun progress bar sync; it is simple and works once the client TE stops overwriting it.

**`SolarArrayGui`**

- Delete the `GUI` enum; pass `SolarTier` directly.
- Localize the title and "Inventory".
- Cache the title string.
- Optionally draw tier/EU/tooltip.
- Use the IC2 GUI texture (fine, IC2 is required) or ship a copy for stability.

**`SolarHatItem`**

- One static `ArmorMaterial`, unique name, `setMaxDamage(0)`.
- Static `PlayerState`, private fields, `long now`/`delta` handled consistently.
- Share `SolarSun.isSunVisible` and `SolarSun.perPanelOutput`.
- Use `double available` and do not truncate charge returns.
- If keeping the array-rate behavior, cap/waste explicitly and document it; add a buffer if desired.
- If parity is desired, add the `IC2` hat mode (chest only, 1 EU/t, any tier, ignore transfer).
- Make the confusion mechanic configurable and fix the inversion/lastTick behavior.

**`CompactSolars` / `CompactSolarsConfig`**

- Move config parsing/validation into `CompactSolarsConfig` with a `LogManager` logger.
- Clamp `generationInterval` to `>= 1`; expose typed getters.
- Register blocks/items in pre-init (block currently in init) and recipes in post-init.
- Keep `@Mod` dependencies in sync with the compiled IC2 version (`[2.2.828,)` or tighter).
- Remove empty proxy render hooks or implement them.

**`SolarArrayBlock`**

- Implement `createNewTileEntity` and delegate from `createTileEntity` (or remove the latter).
- Guard `breakBlock` with `instanceof`.
- Extract a local `InventoryDrops` helper (1.7.10 has no `InventoryHelper`); delete `newSize`.
- Use `world.rand`.
- Add `setStepSound(Block.soundTypeMetal)`.
- Add bounds/fallback in `getIcon`.
- Add a lang key for the block name if `getLocalizedName()` is used by integrations.

**`SolarArrayItem`**

- `getMetadata`/`getUnlocalizedName` use `SolarTier.byMetadata` and precomputed names.
- Add `addInformation` tooltip with output/tier/buffer/panels.
- Pass the block for both `ItemBlockWithMetadata` arguments explicitly (`super(b, b)`) rather than
  referencing the static `CompactSolars.compactSolarBlock`; the second argument is the block used for
  `getIconFromDamage`, and the static field is not guaranteed to be assigned in every construction path.

### 6.4 Incremental migration plan

1. **Mechanical cleanup (no behavior change):** rename fields/methods, add `@Override`, make fields
   `final`, extract constants, cache `VALUES`, precompute names, delete dead code. Compile/test.
2. **Extract data/logic:** add `SolarTier` (wrapping the existing enum values), `SolarSun`,
   `CompactSolarsConfig`; keep behavior identical. Add unit tests for tier/meta and sun math.
3. **Fix parity behind a config flag:** implement IC2's sun predicate and double sky-light output
   with `matchIc2Solar` default true; keep the old fixed-output path for compatibility. Add the daily
   parity test.
4. **Fix container/slot/UI:** `SlotCharge`, correct transfer ranges, server-only tile update,
   localized GUI, tooltips.
5. **Fix recipes/hats:** null checks, size-1 hat stack, static armor material, state machine, optional
   IC2 hat mode.
6. **Structural cleanup:** remove `GUISolar.GUI`, optionally collapse MV/HV TE subclasses, split
   registration/recipes, rename the package/classes if desired.
7. **Docs/build:** rewrite README, resolve GPLv3, add tests/CI, remove stale scripts/assets.

---

## 7. Testing plan

**Unit-testable pure logic**

- `SolarTier.byMetadata`: 0/1/2 map to LV/MV/HV; 3/-1 clamp or throw with a log; names/tier/panels.
- `SolarSun.perPanelOutput`: sky 0–4 → 0; 5 → 1/3; 12 → 0.8; 15 → 1.0; non-default `solarBase`
  (0.5/2.0).
- `SolarSun.isSunVisible`: matrix of `hasNoSky`, `canSeeSky`, `skyLight`, `raining`, `thundering`,
  desert/non-desert. Extract a pure predicate if mocking `World` is painful.
- Config validation: `generationInterval` 0/-1 → 1; large values preserved; comment/log.
- Daily parity: compute IC2 and CS totals over 24,000 ticks after the fix; assert ratio ≈ 1.0.
- Transfer ranges: with a fake inventory/container, assert slot 27 is reachable, machine slot is
  tried, invalid items fall through.

**Manual/integration matrix**

- LV/MV/HV at noon: 8/64/512 EU/t into the appropriate cable/sink.
- Dawn/dusk: compare against an adjacent IC2 solar; after the fix the ramp should match.
- Rain/thunder: non-desert both stop; desert IC2 behavior (including global thunder) matches; savanna
  and mesa match IC2's (arguably surprising) behavior.
- Under glass/water/leaves; at y=255; in the Nether/End.
- GUI: sun icon follows the server; title/localization; tooltip.
- Shift-click battery into the slot; hotbar→main including slot 27; hopper insertion of a full item
  and a wrong-tier item (must reject).
- Wrench: drop rate/requirement; pickaxe break; contents drop with NBT.
- Chunk reload/world reload: sun state on load, inventory/energy persistence, no lost charge.
- Hat: LV/MV/HV charging, full/empty armor, tier mismatch, confusion timing, durability, death/respawn.

---

## 8. Prioritized roadmap

**P0 — fix before release**

- Clamp `productionRate` (B1).
- Bound metadata/damage (B2).
- Fix README and license (B3).
- Fix container transfer + `SlotCharge` (B7, B8).
- Make the tile entity server-only (B14).
- Implement `createNewTileEntity` correctly (B13).

**P1 — parity and correctness**

- IC2 sun predicate + sky-light scaling + IC2 config (B4).
- Storage decision + documentation (B5).
- On-load/128-tick sun refresh (B6).
- `markDirty` after charge (B9).
- Recipe null checks + size-1 hat stack (B11, B12).
- Hat material/state/durability (B10).
- GUI localization + tooltips (B15, B16).

**P2 — polish and structure**

- Storage parity/direct `IEnergySource` if exact 16 EU for LV is required.
- Collapse GUI enum/subclasses; split registration/recipes.
- Optimizations (client guard already P0, cache values, etc.).
- Comparator/WAILA/NEI, creative tab, sound, hat config.
- Tests, build hygiene, unused assets.

---

## Appendix A — File-by-file notes

**`CompactSolars.java`**
- Config is one unchecked static int; move to `CompactSolarsConfig`.
- Block constructed in pre-init but registered in init; items/hats registered in pre-init. Register
  block/items together in pre-init.
- `FMLLog`/`Level` deprecated; use a logger.
- `resetMap` name is unclear; use `clearSolarHatState`.
- `@Mod` IC2 dependency `[2.1,)` is too permissive for the compiled 2.2.828 API; tighten.

**`CompactSolarType.java`**
- God enum; split.
- `values()` used repeatedly; cache.
- `makeEntity` unbounded + reflective.
- Recipes lack null checks; hat recipe size 0.
- Mutable public fields; use final/private.
- `getTextureRow` unused; `addRecipe` wrapper pointless.
- `ordinal()` drives metadata/tier/GUI/texture row; use explicit fields.

**`BlockCompactSolar.java`**
- `createNewTileEntity` returns null.
- `getIcon` unbounded/nullable; `values()` clone per render.
- `breakBlock` blind cast; `dropContent` hand-rolled/dead parameter.
- Redstone tab; no metal step sound; no tooltip.
- Good: simple icon mapping, metadata drop, GUI opening.

**`ItemCompactSolar.java`**
- `getUnlocalizedName` unbounded; cache names.
- `getMetadata` clamps only upper bound.
- `super(b, CompactSolars.compactSolarBlock)` unnecessarily reaches for the static field; use
  `super(b, b)` so the icon block is always the same instance being wrapped.
- No tooltip.

**`TileEntityCompactSolar.java`**
- Mixed concerns; client logic; stale `isDaytime`.
- `productionRate` crash; no validation.
- Sun refresh 64/no on-load; no sky-light scaling.
- Charge slot unvalidated; no `markDirty` after charge.
- `getContents` leaky; `getStackInSlotOnClosing` no markDirty.
- Good: NBT items/energy, wrench interface, BasicSource integration, distance check.

**`ContainerCompactSolar.java`**
- Transfer bug (no machine target, off-by-one).
- Generic Slot; no tier/fullness validation.
- Unused `myPlayer`, `getPlayer`, `layoutContainer` type parameter.
- Sun progress bar sync is fine.

**`GUISolar.java`**
- Hardcoded English; duplicate `GUI` enum; no tooltip.
- Sun icon positions match IC2; no energy gauge (also IC2's solar GUI has none).
- Cache title; use `StatCollector`.

**`ItemSolarHat.java`**
- `EnumHelper` per instance; duplicate material name; durability 11 vs infinite.
- All-armor/tier-limited/transfer-limited vs IC2's chest-only/any-tier/ignore-limits.
- `buildUp`/`lastTick` logic suspect; dose scaling uneven; rain cache gated.
- `PlayerState` non-static; `clearRaining` misnamed.
- Good: server-only, zero armor display (matches IC2), per-tier textures.

**`CommonProxy` / `ClientProxy`**
- Empty render hooks are dead; GUI factories are fine.
- Client GUI factory could pass the tier directly.

**Resources/build**
- Lang files have all six expected block/hat keys; GUI/label keys missing.
- `mcmod.info` raw newlines; no logo.
- Unused sprite sheets.
- JUnit dependencies but no tests.
- Stale `update_version`; verify `jitpack.yml`; `autoUpdateBuildScript`.

---

## Appendix B — Quick-win checklist

- [ ] Clamp `productionRate` to `>= 1`.
- [ ] `SolarTier.byMetadata` bounds + precomputed unlocalized names.
- [ ] Guard `getIcon` and `createNewTileEntity`.
- [ ] `SlotCharge` + correct `transferStackInSlot` ranges.
- [ ] `if (worldObj.isRemote) return;` in the tile update.
- [ ] IC2 sun predicate + sky-light scaling (double) + IC2 solar config.
- [ ] On-load sun update; 128-tick refresh.
- [ ] `markDirty()` on successful charge.
- [ ] Null-check recipe ingredients; hat stack size 1.
- [ ] One static hat `ArmorMaterial`; `setMaxDamage(0)`.
- [ ] Localize GUI title/label; add item tooltip.
- [ ] Decide storage semantics and document.
- [ ] Rewrite README; resolve GPLv3/MIT.
- [ ] Add tests for `SolarTier`, `SolarSun`, config validation.
- [ ] Remove dead code/unused assets; cache `values()`; use `Locale.ROOT`.
