# Карта проекта Forgotten Relics (NeoForge 26.1.2)

Мод: id `forgottenrelics`, пакет `com.integral.forgottenrelics`, версия `1.1.0-26.1.2`.
План — [PORTING_PLAN.md](PORTING_PLAN.md), прогресс — [PROGRESS.md](PROGRESS.md).

## Каталоги

```
forgotten-relics/
├── .claude/skills/                 скиллы порта (legacy-mod-porting, neoforge-26-1-api,
│                                   thaumcraft-to-thaumaturge, mc-port-verification)
├── reference/original/             оригинал 1.7.10 (git clone, только чтение)
├── tools/                          генераторы и проверки (Python, запуск `python -I tools/<script>.py`)
│   ├── extract_original.py         исследования и инфузии оригинала → tools/data/original_research.json
│   ├── data/                       таблицы-источники: original_research.json, ingredients.json, aspects.json
│   ├── gen_data.py                 (M3) research_category/entry, infusion, base_aspects, damage_type, теги
│   ├── gen_assets.py               (M2) items/*.json, models/item, lang en_us/ru_ru, sounds.json
│   ├── gen_test_structures.py      пустая структура 17x8x17 для GameTest алтаря
│   ├── check_assets.py, check_lang.py   копии из mc-port-verification
│   ├── check_animation.py          анимация Runic Stone по двум снимкам runClientCheck
├── src/main/java/com/integral/forgottenrelics/
│   ├── ForgottenRelics.java        @Mod, регистрация DeferredRegister-ов, конфига, payload-ов
│   ├── FRConfig.java               ModConfigSpec, 45 опций с именами оригинала
│   ├── registry/                   FRItems, FREntities, FRDataComponents, FRSounds, FRCreativeTabs,
│   │                               FRDamageTypes, FRTags, FRMaterials
│   ├── item/                       базовые классы: RelicItem, RelicCurioItem (ICurioItem), TomeItem (удержание)
│   ├── item/bauble/                SuperpositionRing, MiningCharm, AdvancedMiningCharm, DarkSunRing, AncientAegis,
│   │                               DeificAmulet, ShinyStone, OblivionAmulet, NebulousCore, NebulousCoreDormant
│   ├── item/passive/               ChaosCore, FalseJustice, FateTome, OmegaCore, XpTome
│   ├── item/utility/               DimensionalMirror, WeatherStone, TeleportationTome, ObeliskDrainer, GhastlySkull,
│   │                               Paradox
│   ├── item/tome/                  CrimsonSpell, EldritchSpell, ChaosTome, MissileTome, LunarFlares, Apotheosis,
│   │                               SoulTome, TelekinesisTome
│   ├── entity/                     AIProjectileBase, BabylonWeapon, LunarFlare, RageousMissile, ChaoticOrb,
│   │                               CrimsonOrb, DarkMatterOrb, SoulEnergy, ShinyEnergy
│   ├── event/                      FRCombatEvents (incoming damage/death), FRMiningEvents (BreakSpeed)
│   ├── compat/                     VisHelper (WandAccess), CuriosBridge, BotaniaBridge (частицы, Gaia),
│   │                               ObeliskBridge (internal-класс TT изолирован здесь),
│   │                               TTEffects (burst/дуги через Effects TT), BotaniaBridge.Client (рендер Babylon, молнии),
│   │                               TTTestBridge (алтарь и исследования для тестов)
│   ├── network/                    FRNetwork, LightningPayload (единственный S2C-пакет)
│   ├── util/                       SuperpositionHandler (поиск баублов, цели, телепорт), XpHelper, Vec3Helper
│   ├── client/                     FRClient (рендеры, Shift/Ctrl), renderer/OrbRenderer, ClientCheck (снимки), ScenarioRunner (16 сценариев с реальным игроком)
│   └── gametest/                   FRGameTests + тесты по milestone (регистрируются при -Dforgottenrelics.gametest)
├── src/main/resources/
│   ├── assets/forgottenrelics/     textures/item (из оригинала, snake_case), items/, models/item/, lang/, sounds/
│   └── data/forgottenrelics/       thaumaturge/research_category, thaumaturge/research_entry, recipe/,
│                                   data_maps/item, damage_type/, tags/
├── src/main/templates/META-INF/neoforge.mods.toml
├── build.gradle, settings.gradle, gradle.properties, gradlew*
└── run/, run-server/, run-gametest/, run-check/   (игровые каталоги, не в git)
```

## Старый класс → новый

| Оригинал (`com.integral.forgottenrelics`) | Новый | Примечание |
|---|---|---|
| `Main` | `ForgottenRelics`, `registry/*` | |
| `handlers/RelicsConfigHandler` | `FRConfig` | |
| `handlers/RelicsEventHandler` | `event/FRCombatEvents`, `event/FRMiningEvents` | |
| `handlers/SuperpositionHandler` | `util/SuperpositionHandler`, `compat/VisHelper`, `compat/CuriosBridge` | wandSearch/consumeVis → VisHelper |
| `handlers/DamageRegistryHandler` | `registry/FRDamageTypes` + `data/.../damage_type/*.json` | |
| `handlers/RelicsMaterialHandler` | `registry/FRMaterials` (`ToolMaterial` record) | |
| `handlers/*Message` (7) | `network/LightningPayload` (1); burst/дуги — эффекты TT, искры/виспы — частицы Botania | |
| `items/ItemBaubleBase`, `ItemBaubleBaseModifier` | `item/RelicCurioItem` | |
| `items/Item*` (29) | `item/{bauble,passive,utility,tome}/*` | |
| `items/ItemTelekinesisTomeLegacy` | — | не переносится |
| `entities/*` (8 + база) | `entity/*` | |
| `entities/FXBurst`, `FXWisp` | `client/particle/*` или частицы Botania | |
| `proxy/ClientProxy`, `Render*` | `client/FRClient`, `client/renderer/*` | |
| `proxy/CommonProxy` | — | не нужен |
| `research/RelicsResearchRegistry` | `data/forgottenrelics/thaumaturge/research_*`, `data/.../recipe/*` (генератор) | |
| `research/RelicsAspectRegistry` | `data/forgottenrelics/data_maps/item/base_aspects.json` (только свои предметы) | |
| `research/ForgottenRelicsResearchItem` | — | заменён JSON |

## Зависимости во время выполнения

```
forgottenrelics ─┬─ thaumaturge 1.1.0 ─┬─ curios 15.0.0
                 │                     └─ lithostitched
                 └─ botania (Revived 2.0.2) ── patchouli 26.1-94
```
