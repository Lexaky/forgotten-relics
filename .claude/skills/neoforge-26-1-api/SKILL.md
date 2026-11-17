---
name: neoforge-26-1-api
description: >-
  Practical reference for writing NeoForge mods on Minecraft 26.1.x (26.1.2, NeoForge 26.1.2.x, Java 25,
  ModDevGradle) and for translating old Forge 1.7.10/1.12/1.20 code to it: renamed classes (ResourceLocation →
  Identifier), data components instead of NBT, Item.Properties tool helpers, ValueInput/ValueOutput block entities,
  data-driven enchantments and villager trades, item model definitions, the new GUI extractor, GameTest registration,
  and build/dependency setup. Use it whenever code targets Minecraft 26.1 / NeoForge 26.x, when a compile error names a
  missing vanilla method, when porting any pre-1.21 mod logic, or when setting up a 26.1 mod project — even if the user
  only says "update the mod" or "it doesn't compile on 26.1".
---

# NeoForge 26.1 API for porters

Everything here was compiled and run on Minecraft 26.1.2 / NeoForge 26.1.2.122 / ModDevGradle 2.0.148 / JDK 25.
APIs move between snapshots; when in doubt, grep the extracted sources (section "Reading the sources").

- `references/api-cheatsheet.md` — old idiom → 26.1 idiom tables (registration, items, blocks, entities, events,
  components, GUI, enchantments, trades, models). Read it when writing or porting code.
- `references/build-setup.md` — build.gradle/settings/mods.toml templates, CurseMaven/Modrinth/Curios dependencies,
  run configs (client, server, gameTestServer, a screenshot client), and how to extract sources.

## The ten changes that cause most errors

1. `ResourceLocation` is now `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath`, `Identifier.parse`);
   `ResourceKey#location()` is `ResourceKey#identifier()`.
2. NBT on items is gone: per-stack state is a **data component** (`DeferredRegister.DataComponents`,
   `registerComponentType(name, b -> b.persistent(codec).networkSynchronized(streamCodec))`). Use a stream codec too, or
   creative-mode copies drop the data.
3. Tools are configured on `Item.Properties`: `.sword(material, dmg, speed)`, `.pickaxe(...)`, `.shovel(...)`; axes and
   shovels with custom behaviour still subclass `AxeItem`/`ShovelItem(material, dmg, speed, props)`. `ToolMaterial` is a
   record: `new ToolMaterial(incorrectBlocksTag, durability, speed, attackBonus, enchantability, repairTag)`.
4. `Item#use(Level, Player, InteractionHand)` returns `InteractionResult` (no holder). Damage with
   `stack.hurtAndBreak(n, entity, EquipmentSlot)`. Hurting entities: `entity.hurtServer(ServerLevel, source, amount)`.
5. Block entities persist through `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` (`putInt`,
   `storeNullable(name, codec, value)`, `getIntOr`, `read(name, codec)`); drop contents in `preRemoveSideEffects`.
6. Entity tags: `type.builtInRegistryHolder().is(tag)` (there is no `EntityType#is(TagKey)`).
7. Enchantments are datapack JSON (`data/<ns>/enchantment/*.json`); code reads levels with
   `EnchantmentHelper.getItemEnchantmentLevel(holder, stack)` after looking the key up in `Registries.ENCHANTMENT`.
   Not in `#minecraft:in_enchanting_table` ⇒ never offered by the table.
8. Villager trades are data: `data/<ns>/villager_trade/**.json`, grouped by a tag, referenced by
   `data/<ns>/trade_set/<profession>/level_N.json`; the `VillagerProfession` record maps levels to `ResourceKey<TradeSet>`.
   Costs accept number providers (`{"type":"minecraft:uniform","min":1,"max":2}`), so old random prices port exactly.
9. Item rendering needs `assets/<ns>/items/<id>.json` (item model definition) in addition to `models/item/*.json`.
   Per-stack tint without client code: `"tints":[{"type":"minecraft:constant","value":-1},
   {"type":"minecraft:custom_model_data","index":0,"default":<rgb>}]` and set `DataComponents.CUSTOM_MODEL_DATA` colors.
   Switch models on a component with `"type":"minecraft:condition","property":"minecraft:has_component"`.
10. GUI code draws in `Screen#extractRenderState(GuiGraphicsExtractor g, ...)` (`g.fill`, `g.text(font, str, x, y,
    argb, shadow)`, `g.item(stack, x, y)`, `g.pose()` is a `Matrix3x2fStack`). Colours need an alpha byte (`0xFFFFFFFF`).

## Reading the sources (do this instead of guessing)

After the first `./gradlew compileJava`, MDG leaves `build/moddev/artifacts/minecraft-patched-<ver>-sources.jar`.
NeoForge's own sources are `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/<ver>/*/neoforge-<ver>-sources.jar`.
Unzip both into the scratchpad once and grep (`grep -n "public .*(" path/Item.java`). Vanilla data templates
(oak tree feature, leaves loot, sharpness enchantment, trade sets, blockstates) are inside
`build/moddev/artifacts/minecraft-patched-<ver>.jar` under `data/minecraft/...` and `assets/minecraft/...`:
`unzip -p <jar> data/minecraft/worldgen/configured_feature/oak.json`.

A target library's own source is the best example of current idioms (registration, materials, tree blocks, GameTest
wiring); read its registry classes before writing yours.

## Event map for classic Forge hooks

| Old hook | 26.1 event / API |
|---|---|
| `LivingDropsEvent` | same name: `getDrops()` is `Collection<ItemEntity>`, `isRecentlyHit()` |
| XP from kills (`EntityXPOrb` spawning) | `LivingExperienceDropEvent#setDroppedExperience` |
| `LivingHurtEvent` (cancel/modify) | `LivingIncomingDamageEvent` (cancelable, `getSource()`, `getAmount()`) |
| `LivingHurtEvent` after armour | `LivingDamageEvent.Post#getInflictedDamage()` |
| `HarvestDropsEvent` | `BlockDropsEvent` (`getDrops()` list of `ItemEntity`, `getTool()`, `getBreaker()`) |
| `BreakSpeed` | `PlayerEvent.BreakSpeed` (`getPosition()` is Optional) |
| `PlayerUseItemEvent.Finish` | `LivingEntityUseItemEvent.Finish` |
| `LivingSpawnEvent` | `FinalizeSpawnEvent` (natural spawns) |
| `LivingDeathEvent` | same |
| `IFuelHandler` | override `Item#getBurnTime(stack, recipeType, fuelValues)` |
| OreDictionary | tags, mostly `c:` (`c:nuggets/emerald`, `c:dyes/black`, `c:raw_materials/iron`) |
| `VillagerRegistry` | `VillagerProfession` + `PoiType` registries, trades as data (see above) |
| `GameRegistry.registerTileEntity` | `new BlockEntityType<>(Factory::new, block)` in `DeferredRegister` |
| `Block#onBlockActivated` | `useItemOn(...)` (returns `InteractionResult.TRY_WITH_EMPTY_HAND` to fall through) / `useWithoutItem(...)` |
| `World#isRemote` | `level.isClientSide()` |
| Dimension id `-1` | `level.dimension() == Level.NETHER` |
| `IBossDisplayData` | tag `Tags.EntityTypes.BOSSES` (`c:bosses`) |
| `IMob` | `instanceof Enemy` |

Annotated subscribers: `@EventBusSubscriber(modid = ...)` on a class with static `@SubscribeEvent` methods (game bus by
default; mod-bus events such as `RegisterCapabilitiesEvent` and `RegisterDataMapTypesEvent` are routed automatically).
Client-only subscribers: `@EventBusSubscriber(modid = ..., value = Dist.CLIENT)`.

## Pitfalls met in practice

- Shapes must have min ≤ max: an old block whose last state had zero/negative width (FM's 12-slice cake) crashes
  registry initialisation with "The min values need to be smaller or equals to the max values".
- `Item.Properties#repairable(otherItem.get())` inside another item's registration lambda is fragile; repair with a tag.
- Leaves only decay correctly if the log is in `#minecraft:logs` (block tag). Adding a log to `#minecraft:logs_that_burn`
  makes it smelt into charcoal — conflicting with a custom smelting recipe.
- Curios inventories of players that never "joined" (GameTest mock players) are empty until `inv.reset()`.
- `net.neoforged.neoforge.common.util.FakePlayer` cannot ride (`startRiding` is overridden to fail) and never ticks, so it
  keeps spawn invulnerability; real damage to it does not fire damage events.
- A hand-built `DamageContainer` reports `getInflictedDamage() == 0` until `captureInflictedDamage()` is called.
- A Gradle run that sits for many minutes before anything starts: `jstack` the BUSY daemon; a hang in
  `listFileSystems` is file-system watching — set `org.gradle.vfs.watch=false`.
- Integrated server pauses with the pause screen: a dev client whose window has no focus opens it by itself
  (`options.pauseOnLostFocus`), and automated client checks then stall.
- `level.getEntities(...)` cannot see items spawned into chunks whose entity sections are not loaded; count drops with an
  `EntityJoinLevelEvent` listener instead (see `mc-port-verification`).
