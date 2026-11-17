# Thaumaturge code integration (1.1.0)

API facades are bound by TT in its mod constructor, so call them after mod construction (events, ticks, not static init).

## Wand rods and caps

```java
public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, MODID);
DeferredHolder<WandRod, WandRod> ROD_INFERNAL = RODS.register("infernal", () -> new WandRod(
        150,                                   // capacity in vis
        12,                                    // craft cost factor
        id("textures/models/wand_rod_infernal.png"),   // TC4 96x96 rod textures render correctly
        new InfernalRodUpdate(),               // IWandRodOnUpdate or null
        false, false, false,                   // glow, staff, runes
        id("rod_infernal")));                  // research gating the generated assembly recipe
// item — internal class, required for auto assembly:
ITEMS.registerItem("wand_rod_infernal", p -> new com.leclowndu93150.thaumaturge.content.wands.ItemWandRod(p, ROD_INFERNAL));
```
Caps: `new WandCap(baseCostModifier, List<ResourceKey<IAspect>> special, specialModifier, craftCost, texture)` and
`ItemWandCap`. Rod registry lookup: `BuiltInRegistries.REGISTRY.getValue(WandRod.REGISTRY_KEY.identifier())`.

## Reading and writing wand vis (no internal classes)

- Read: `WandAccess.getVis(wand, aspectKey)` / `getAllVis(wand)` — centivis (100 = 1 vis).
- `WandAccess.withVis` returns a **copy**; rod hooks must mutate the stack they get, so write the component directly:
  `wand.set((DataComponentType<WandVis>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id("thaumaturge","wand_vis")), vis.with(key, n))`.
- Rod/cap/sceptre: component `thaumaturge:wand_parts` has an internal type; encode it with `type.codec()` +
  `JsonOps` → `{"cap":..., "rod":..., "sceptre":bool}`, edit, decode, `set` (used to swap a profane rod for the
  profaned one). Capacity = rod.capacity × 100 (× 150 for sceptres).
- Wands are item `thaumaturge:wand` (tag `#thaumaturge:wands`); a socketed focus is component `thaumaturge:socketed_focus`.
- In tests build a wand by parsing `{"cap":"thaumaturge:iron","rod":"<ns>:infernal"}` with the parts codec.

## Essentia consumer (machine that burns essentia)

Implement `IEssentiaTransport` on the block entity and register it:
`event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, TYPE.get(), (be, side) -> be)` (+ `ASPECT_QUERY` with an
`IAspectQuery#queryAspects()` for goggles). Tubes route by **suction**: return the wanted aspect from `getSuctionType`
and e.g. 128 from `getSuctionAmount` while you have room. Then pull actively each tick:
```java
IEssentiaTransport src = EssentiaAccess.transport(level, pos.relative(dir), dir.getOpposite());
if (src != null && src.canOutputTo(face) && src.getEssentiaAmount(face) > 0) { Holder<IAspect> held = src.getEssentiaType(face);
    if (held != null && held.is(wanted) && src.takeEssentia(held, 1, face) == 1) stored++; }
```
One tube carries one aspect at a time; a machine wanting several aspects should pick one suction type (FM used a mode
switch). Jars expose transport only through their top face. `Aspects.resolve(level, key)` turns a key into a holder.

## Caster (wand) triggers

```java
CasterTriggerRegistry.registerCasterBlockTrigger(manager, EVENT_ID, Blocks.OBSIDIAN.defaultBlockState(), MODID);
// ICasterTriggerManager#performTrigger(Level, ItemStack caster, Player, BlockPos, Direction side, int event)
```
Register in `FMLCommonSetupEvent#enqueueWork`. TT calls it when a wand is used on a matching block.

## Knowledge, research, aspect pools, warp

- `KnowledgeAccess.of(player)` → `IPlayerKnowledge`: `isResearchComplete`, `isResearchKnown`, `addResearch` (starts it,
  e.g. unlocking a hidden entry when an item is first held), `sync(serverPlayer)`. The API cannot *complete* research;
  completion happens through play (`content.research.ResearchManager` is internal — fine in tests only).
- `AspectPoolAccess.grant(serverPlayer, holder, amount)` — adds research points (FM Crystal Scribing Tools).
- `WarpHelper.addWarp(serverPlayer, n, WarpType.NORMAL|PERMANENT|TEMPORARY)`; `WarpHelper.getActualWarp(player)`.
- `IWarpingGear#warp(stack, wearer)` on items: counted in both hands, armour slots and curios.
- Events: `ResearchEvent.Unlocked/StageAdvanced/Completed/KnowledgeGained` (cancellable), `InfusionCraftedEvent`
  (can replace the result).

## Taint

`TaintApi.isTainted(level, pos)` (biome tag or chunk saturation over a threshold), `addEcologicalPressure`,
`cleanEcologicalPressure`, `addTaintSeed`. Tests can taint a spot with `addEcologicalPressure(level, pos, 1000F)`.

## Item aspects at runtime

`AspectIndexAccess.of(itemOrStack)` → `AspectList` (`amountOf(key, registryAccess)`); custom derivation strategies via
`RegisterAspectContributorsEvent`.

## Spells (foci)

A spell is a graph: `new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(SpellNode.of(part)))`.
Register a behaviour:
```java
DeferredRegister<SpellBehaviorType<?>> B = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, MODID);
B.register("hellfire", () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, Hellfire.CODEC)); // MapCodec.unit(Hellfire::new)
class Hellfire extends AbstractEffectBehavior { protected void apply(CastContext ctx, SpellTarget t, float power, int index) {...}
    protected boolean widens() { return false; } }   // true = also hit entities in the RADIUS stat around point targets
```
Effects receive targets from the previous node and pass them on, so order matters (TT's Blink moves the caster first).
Validate like the focal manipulator: `Spells.analyze(spell, null, registryAccess, player).problems()`; cast in tests:
`Spells.cast(caster, ItemStack.EMPTY, spell, List.of(SpellTarget.entity(target, Vec3.ZERO)), 1F)`.

## Curios

TT registers player slots (head, necklace, ring, belt, charm, back). Add items to `data/curios/tags/item/<slot>.json`
and register `CuriosCapability.ITEM` (`(stack, ctx) -> new ICurio(){ getStack(); curioTick(SlotContext) }`).
Find worn items: `CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.findFirstCurio(item))`; equip:
`inv.getStacksHandler("necklace").get().getStacks().setStackInSlot(i, stack)`. Curios API is `compileOnly`
(`curios-neoforge:<ver>:api`), the full jar `localRuntime`.

## API vs internal quick list

API (safe): `api.aspect.*`, `api.wands.{WandRod,WandCap,WandVis,WandAccess,IWandRodOnUpdate}`, `api.essentia.*`,
`api.casters.*`, `api.capability.*`, `api.research.*` (records, events, pool), `api.warp.*`, `api.taint.TaintApi`,
`api.spell.*`, `api.items.{IWarpingGear,IVisDiscountGear}`, `api.damagesource.TTDamageTypes`.
Internal (avoid outside tests): `content.wands.{WandParts,WandVisHelper,ItemWandRod*}`, `content.research.*`,
`registry.TT*`, `client.screen.*` (* ItemWandRod is the accepted exception).
