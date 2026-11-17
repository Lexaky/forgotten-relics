# 26.1 API cheat sheet (verified on 26.1.2 / NeoForge 26.1.2.122)

Contents: Registration · Items · Blocks & block entities · Entities & combat · Components · Enchantments · Trades &
villagers · Worldgen & loot · Assets · GUI · Commands & misc

## Registration

```java
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
DeferredItem<Item> SHARD = ITEMS.registerSimpleItem("wrath_shard");
DeferredItem<Item> SHARD2 = ITEMS.registerSimpleItem("pride_shard", props -> props.rarity(Rarity.UNCOMMON));
DeferredItem<MyItem> TOOL = ITEMS.registerItem("tool", MyItem::new, props -> props.pickaxe(MAT, 1.0F, -2.8F));
DeferredItem<BlockItem> BI = ITEMS.registerSimpleBlockItem(MY_BLOCK);               // uses block.<ns>.<id> name
DeferredItem<BlockItem> TALL = ITEMS.registerItem("bush", p -> new DoubleHighBlockItem(BUSH.get(), p.useBlockDescriptionPrefix()));

public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
DeferredBlock<Block> B = BLOCKS.registerBlock("star_block", Block::new, p -> p.strength(5F, 1200F).sound(SoundType.METAL));

DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
// Other mods' custom registries work the same way:
DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, MODID);
```

Mod constructor: `public MyMod(IEventBus modBus, ModContainer container)`; `container.registerConfig(ModConfig.Type.COMMON, SPEC)`.
Load order matters only for class init: call every `X.register(modBus)` in the constructor; block items may live in the
block registry class as long as that class is initialised before the item register fires.

Config: `ModConfigSpec.Builder` → `define`, `defineInRange`; guard reads with `SPEC.isLoaded()` (GameTests and early
code may run before config load).

## Items

| Need | 26.1 |
|---|---|
| Right click | `InteractionResult use(Level, Player, InteractionHand)` |
| Right click on block | `InteractionResult useOn(UseOnContext)` |
| On entity | `InteractionResult interactLivingEntity(ItemStack, Player, LivingEntity, InteractionHand)` |
| Hit enemy | `hurtEnemy(stack, victim, attacker)` then `postHurtEnemy(...)` (damage the tool in post) |
| Mined block | `boolean mineBlock(ItemStack, Level, BlockState, BlockPos, LivingEntity)` |
| Inventory tick | `inventoryTick(ItemStack, ServerLevel, Entity, @Nullable EquipmentSlot)` — server only |
| Finished eating | `finishUsingItem(ItemStack, Level, LivingEntity)` |
| Food | `props.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build())` |
| Name per stack | override `Component getName(ItemStack)` |
| Glint | `isFoil(ItemStack)` |
| Tooltip | `appendHoverText(stack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` |
| Never takes damage | override `setDamage(stack, int)` → `super.setDamage(stack, 0)` (NeoForge hook, used by `setDamageValue`) |
| Burn time | `getBurnTime(ItemStack, @Nullable RecipeType<?>, FuelValues)` |
| Repair item | `props.repairable(tagOrItem)` |
| Enchant in code | `stack.enchant(holder, level)`; stored on books: `DataComponents.STORED_ENCHANTMENTS` |
| Spawn a drop | `entity.spawnAtLocation(ServerLevel, ItemStack, float yOffset)` |

## Blocks & block entities

- Cake-like blocks: `IntegerProperty.create("eaten", 0, 11)`, `getShape`, `useWithoutItem`, `attack` (left click),
  `randomTick` + `isRandomlyTicking`, `updateShape(..., ScheduledTickAccess, ...)`, `canSurvive`.
- Flowers: `new FlowerBlock(MobEffects.BLINDNESS, 8F, props)`; tall plants: subclass `TallFlowerBlock`
  (`HALF` property, `DoublePlantBlock.placeAt(level, state, pos, flags)`).
- Trees: `new SaplingBlock(new TreeGrower("ns:name", Optional.empty(), Optional.of(featureKey), Optional.empty()), props)`;
  feature = JSON `data/<ns>/worldgen/configured_feature/<name>.json` (copy vanilla `oak.json`). Leaves:
  `new TintedParticleLeavesBlock(0.01F, props)`; logs: `RotatedPillarBlock`.
- Block entity block: extend `BaseEntityBlock`, implement `codec()` (`simpleCodec(Ctor::new)`), `newBlockEntity`,
  `getRenderShape` → `RenderShape.MODEL`, ticker via `createTickerHelper(type, MY_TYPE.get(), MyBE::serverTick)`.
- Sync to clients after a change: `setChanged(); level.sendBlockUpdated(pos, state, state, 3)`.
- Redstone: `level.hasNeighborSignal(pos)`.
- Capabilities: `RegisterCapabilitiesEvent#registerBlockEntity(cap, TYPE.get(), (be, side) -> be)`,
  `registerItem(cap, (stack, ctx) -> ..., item)`; query with `level.getCapability(cap, pos, side)`.
- POI for a job site: `new PoiType(Set<BlockState>, maxTickets, validRange)`; add the id to
  `#minecraft:acquirable_job_site` (`data/minecraft/tags/point_of_interest_type/acquirable_job_site.json`).

## Entities & combat

- Create: `EntityType.X.create(level, EntitySpawnReason.SPAWNER)`; place `entity.snapTo(x, y, z, yRot, xRot)`;
  checks `level.noCollision(entity)`, `level.containsAnyLiquid(entity.getBoundingBox())`; finish
  `mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWNER, null)`;
  add `level.addFreshEntityWithPassengers(entity)`; effect `level.levelEvent(LevelEvent.PARTICLES_MOBBLOCK_SPAWN, pos, 0)`.
- Entity packages moved: `animal.pig.Pig`, `animal.equine.AbstractHorse`, `animal.golem.AbstractGolem`,
  `monster.skeleton.Skeleton/WitherSkeleton`, `monster.zombie.Zombie/ZombifiedPiglin`, `npc.villager.Villager`.
- Effects: `MobEffects.SPEED`, `HASTE`, `STRENGTH` (old MOVEMENT_SPEED/DIG_SPEED/DAMAGE_BOOST names are gone).
- Damage sources: `level.damageSources().playerAttack(p)`, `.generic()`, `.inFire()`, `.wither()`, `.fireball(null, owner)`;
  fire tag `DamageTypeTags.IS_FIRE`; custom type: `new DamageSource(registry.getOrThrow(KEY))`.
- Fire: `entity.igniteForSeconds(float)`, `clearFire()`, `isOnFire()`.
- Player heads: `head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()))`.
- Overlay message: `player.sendOverlayMessage(Component)`; chat: `sendSystemMessage`.

## Networking (S2C payloads)

- Record `implements CustomPacketPayload` with `Type<T> TYPE = new Type<>(id)` and a `StreamCodec`
  (`StreamCodec.composite(Vec3.STREAM_CODEC, ..., ByteBufCodecs.VAR_INT, ..., Ctor::new)`).
- Common side registers **without a handler**: `RegisterPayloadHandlersEvent` →
  `event.registrar("1").playToClient(TYPE, STREAM_CODEC)`. The client handler is registered separately:
  `RegisterClientPayloadHandlersEvent#register(TYPE, (payload, context) -> ...)` (client-only subscriber).
- Send: `PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, payload)`,
  `sendToPlayersTrackingEntity(entity, payload)`. For particles prefer `ServerLevel.sendParticles` with a library's own
  `ParticleOptions` — then no packet is needed at all.

## Entity renderers

- `EntityRenderer<T, S extends EntityRenderState>`: `createRenderState()`, `extractRenderState(entity, state, partial)`
  (copy what you need: `tickCount`, id, synced data), `submit(state, poseStack, SubmitNodeCollector, CameraRenderState)`.
  `state.ageInTicks`, `state.lightCoords`, `state.partialTick` are filled by `super.extractRenderState`.
- Custom quads: `collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> ...)`. Billboard:
  `poseStack.mulPose(camera.orientation)`. Entity vertex format needs every element:
  `buffer.addVertex(pose, x, y, 0).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
  .setNormal(pose, 0, 1, 0)` (template: `DragonFireballRenderer`).
- Render types (`RenderTypes`): `eyes(tex)` = additive glow (GL 770/1), `entityTranslucentEmissive(tex)` = alpha blend
  (770/771), `dragonRays()` = position-colour triangles for light rays (template: `EnderDragonRenderer.submitRays`),
  `entityCutout(tex)`, `lightning()`.
- Renderer that draws nothing: `NoopRenderer::new`. Register in `EntityRenderersEvent.RegisterRenderers`.
- `entity.tickCount` is advanced by the level's entity loop, not by `tick()`: calling `tick()` by hand in a test leaves
  it unchanged.

## Using items (hold Right-click)

- `use(...)` → `player.startUsingItem(hand)`; `LivingEntity.updateUsingItem` then calls
  `onUseTick(level, entity, stack, remaining)` with `remaining` = `getUseDuration` .. 1 and completes at 0 — the 1.7
  `onUsingTick(count)` countdown ports one to one.
- The server side of a click is `ServerPlayerGameMode.useItem(player, level, stack, hand)`; release is
  `player.releaseUsingItem()`; a left click into the air reaches the server as a swing (`Item#onEntitySwing`).
- 1.7 items that relied on Right-click auto-repeat (every 4 ticks while held) still get repeated `use` calls from the
  client; they are not "held" items.

## Sounds

- Client hook for every sound that starts: `Minecraft.getInstance().getSoundManager().addListener((sound, events, range) -> ...)`;
  server side: `PlayLevelSoundEvent`.

## Components

```java
COMPONENTS.registerComponentType("contract", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
stack.get(TYPE); stack.getOrDefault(TYPE, def); stack.set(TYPE, v); stack.remove(TYPE); stack.has(TYPE);
```
Old multi-field NBT → a record with a `RecordCodecBuilder` codec and `StreamCodec.composite(...)`; lists:
`codec.listOf(3, 3)`, `streamCodec.apply(ByteBufCodecs.list(3))`. Vanilla components used in ports: `ENCHANTMENTS`,
`STORED_ENCHANTMENTS`, `CUSTOM_NAME`, `CUSTOM_MODEL_DATA` (`new CustomModelData(floats, flags, strings, colors)`),
`PROFILE`, `FOOD`.

Another mod's component whose Java type is internal: look it up by id
(`BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id)`) and read/write through `type.codec()` with `JsonOps` — no
compile-time link to the internal class (pattern used for Thaumaturge `wand_parts`).

## Enchantments (data)

```json
{"anvil_cost":2,"description":{"translate":"enchantment.ns.wrath"},
 "effects":{"minecraft:damage":[{"effect":{"type":"minecraft:add","value":{"type":"minecraft:linear","base":1.25,"per_level_above_first":1.25}}}]},
 "exclusive_set":["minecraft:looting","ns:other"],
 "max_cost":{"base":55,"per_level_above_first":11},"max_level":5,"min_cost":{"base":5,"per_level_above_first":11},
 "slots":["mainhand"],"supported_items":"#minecraft:enchantable/sharp_weapon","weight":1}
```
Exclusivity is checked both ways, so one side is enough (`Enchantment.areCompatible(a, b)`). Items accept anvil books
only if they are in the `supported_items` tag: add custom swords/pickaxes to `#minecraft:swords`, `#minecraft:pickaxes`...
Effects not expressible in data are implemented in event handlers keyed by `ResourceKey<Enchantment>`.

## Trades & villagers

```json
// data/ns/villager_trade/heretic/greed_shards.json
{"wants":{"id":"minecraft:emerald","count":{"type":"minecraft:uniform","min":1.0,"max":2.0}},
 "additional_wants":{"id":"minecraft:stick"},
 "gives":{"id":"ns:greed_shard"},
 "given_item_modifiers":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":3.0,"max":4.0}}],
 "max_uses":12.0,"reputation_discount":0.05,"xp":2.0}
// data/ns/tags/villager_trade/heretic/level_1.json  {"values":["ns:heretic/greed_shards", ...]}
// data/ns/trade_set/heretic/level_1.json  {"amount":2.0,"random_sequence":"ns:trade_set/heretic/level_1","trades":"#ns:heretic/level_1"}
```
`gives` may carry `components` (e.g. a pre-built wand). Profession:
`new VillagerProfession(Component name, poi -> poi.is(POI_KEY), poi -> poi.is(POI_KEY), ImmutableSet.of(), ImmutableSet.of(), sound, Int2ObjectMap<ResourceKey<TradeSet>>)`.
Profession texture: `assets/<ns>/textures/entity/villager/profession/<id>.png`.

## Worldgen & loot

Loot table conditions used in ports: `minecraft:survives_explosion`, `minecraft:block_state_property` (only the lower
half of a tall plant drops), `minecraft:table_bonus` with fortune chances, `minecraft:match_tool` (shears/silk touch),
`minecraft:inverted`. Copy vanilla `oak_leaves.json`/`rose_bush.json` and edit.

## Assets

```
assets/<ns>/items/<id>.json                {"model":{"type":"minecraft:model","model":"<ns>:item/<id>"}}
assets/<ns>/models/item/<id>.json          {"parent":"minecraft:item/handheld","textures":{"layer0":"<ns>:item/<id>"}}
assets/<ns>/blockstates/<id>.json          variants; a list of {"model","weight"} picks randomly per position
assets/<ns>/models/block/<id>.json         "render_type":"minecraft:cutout" for see-through blocks
textures/item, textures/block              (singular folder names)
```
1.7 animation `.mcmeta` files work unchanged. Lang files are JSON: `item.<ns>.<id>`, `block.<ns>.<id>`,
`enchantment.<ns>.<id>`, `itemGroup.<ns>`.

## GUI

```java
@Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
  g.fill(0, 0, width, height, 0xFF202028);
  g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(2F, 2F); g.item(stack, 0, 0); g.pose().popMatrix();
  g.text(font, "label", x, y, 0xFFFFFFFF, true);
}
```
Input events: `mouseClicked(MouseButtonEvent(x, y, new MouseButtonInfo(button, mods)), doubleClick)`,
`keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0))`. Screenshot: `Screenshot.grab(gameDir, name, mc.getMainRenderTarget(), 1, callback)`.

## Commands & misc

- Run a command as a player with full rights:
  `server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), "tp @s ...")`.
- Data maps: `DataMapType.builder(id, Registries.ENTITY_TYPE, codec).synced(codec, false).build()`, register in
  `RegisterDataMapTypesEvent`, read with `type.builtInRegistryHolder().getData(MAP)`. Files at
  `data/<map-ns>/data_maps/<registry>/<path>.json`; packs merge per key, later packs override a key's whole value.
- Custom registries of other mods are reachable as `BuiltInRegistries.REGISTRY.getValue(KEY.identifier())`.
- A stack matching an `Ingredient` (keeps component data): `ingredient.display().resolveForFirstStack(SlotDisplayContext.fromLevel(level))`.
- `server.getPlayerList().getPlayers()` is a read-only view.
- Recipe by id: `server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id))`.
