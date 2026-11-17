# Forgotten Relics → Thaumaturge 1.1.0 / Minecraft 26.1.2: план портирования

Документ описывает перенос аддона Forgotten Relics (Forge 1.7.10, Thaumcraft 4 + Botania) на NeoForge 26.1.2 поверх
Thaumaturge 1.1.0 и Botania Revived. Статус ведётся в [PROGRESS.md](PROGRESS.md), структура кода — в
[PROJECT_MAP.md](PROJECT_MAP.md). Правило: пункт считается готовым только после того, как прошла его проверка
(GameTest, загрузка сервера без ошибок кодеков или клиентский скриншот). Компиляция проверкой не считается.

| Что | Где | Версия / коммит |
|---|---|---|
| Оригинал | https://github.com/spannerman79/Forgotten-Relics (копия в `reference/original`) | `11dc9b2` (2019-02-11), v1.1.0 Beta, 63 java-файла, ~10.5k строк, WTFPL |
| Целевой API | Thaumaturge (Leclowndu93150), CurseForge 1628024 | 1.1.0, file 9428045 (release jar, в нём datagen-данные) |
| Botania | Botania Revived (VelronDevs, неофициальный порт), CurseForge 1725641 | 2.0.2 для 26.1.2, file 9577666; пакет `velrondevs.botania` |
| Patchouli | Modrinth `patchouli` | 26.1-94-beta (version `2CsnFLom`), жёсткая зависимость Botania Revived |
| Curios | maven.theillusivec4.top | 15.0.0+26.1.2 (жёсткая зависимость Thaumaturge; Botania Revived не принимает beta) |
| Lithostitched | Modrinth `XaDC71GB` | `84kuUpr4` (runtime-зависимость Thaumaturge) |
| NeoForge | maven.neoforged.net | 26.1.2.122 (Botania Revived требует ≥ 26.1.2.100) |
| Опыт прошлого порта | `A:\Projects\Java\forbidden-magic` | Forbidden Magic → TT 1.1.0: образец build-скриптов, `WandBridge`, `CuriosBridge`, GameTest-обвязки |

**Лицензия.** Оригинал под WTFPL, поэтому текстуры, звуки и тексты (en_US, ru_RU) можно брать. Код и ассеты
Thaumaturge и Botania Revived в аддон не копируются: используем их как зависимости (API-классы, предметы в рецептах,
частицы Botania).

## 1. Инвентарь оригинала

### 1.1 Предметы (29 зарегистрированных + 1 мёртвый класс)

Слот Baubles переносится в слот Curios: `RING → curios:ring`, `AMULET → curios:necklace`, `BELT → curios:belt`.
Колонка «Варп» — варп за крафт (`ThaumcraftApi.addWarpToItem`). Варп «носимого снаряжения» (`IWarpingGear#getWarp`)
другой: Apotheosis 5, Chaos Core 2, Chaos Tome 4, Crimson Spell 3, Eldritch Spell 4, False Justice 4, Fate Tome 7,
Ghastly Skull 3, Lunar Flares 3, Nuclear Fury 5, Obelisk Drainer 4, Oblivion Amulet 4, Paradox 8, Soul Tome 3,
Telekinesis Tome 4, Discord Tome 2 (перенесён в M2, тест `items_gear_warp`).

| Старый id | Новый id | Тип | Поведение (кратко; точные формулы переписываются из кода в milestone) | Варп за крафт |
|---|---|---|---|---|
| ItemSuperpositionRing | `superposition_ring` | кольцо | 12–74 % полученного урона делится между всеми носителями кольца; иногда меняет местами с другим носителем | – |
| ItemMiningCharm | `mining_charm` | кольцо | +100 % скорости копания (`miningCharmBoost` 1.0), +2 досягаемость (`miningCharmReach`) | – |
| ItemAdvancedMiningCharm | `advanced_mining_charm` | кольцо | +300 % скорости (`advancedMiningCharmBoost` 3.0), +4 досягаемость | – |
| ItemDarkSunRing | `dark_sun_ring` | кольцо | Ring of the Seven Suns: урон > `darkSunRingDamageCap` (100) отменяется; огонь/лава → лечение; 20 % отражения атаки; 25 % шанс усиления обычных атак по носителю; второе кольцо без эффекта | – (warp на исследовании) |
| ItemAncientAegis | `ancient_aegis` | пояс | −25 % урона, ускоренная регенерация, иммунитет к отбрасыванию; 40 % урона игроков в 32 блоках перенаправляется на носителя | – |
| ItemDeificAmulet | `deific_amulet` | амулет | иммунитет к эффектам (конфиг), тушит огонь, длинные i-frames (конфиг), воздух под водой за вис Aer+Aqua | – |
| ItemShinyStone | `shiny_stone` | амулет | ускоряющаяся регенерация при стоянии на месте (`shinyStoneCheckrate` 4) | – |
| ItemOblivionAmulet | `oblivion_amulet` | амулет | поглощает весь входящий урон, хранит его и случайно выпускает на носителя; вис Ignis+Perditio | 4 |
| ItemArcanum | `nebulous_core` | амулет | скидка на вис (`IVisDiscountGear`), генерирует случайный вис в палочки, 40 % уклонения телепортом, может уснуть → dormant | – |
| ItemDormantArcanum | `nebulous_core_dormant` | амулет | неактивен, потребляет вис для пробуждения → `nebulous_core` | – |
| ItemChaosCore | `chaos_core` | в инвентаре | входящий урон ×[0..2]; 45 %/42 % перенаправить урон случайной сущности в 16 блоках; случайные эффекты | – (2 на иссл.) |
| ItemFalseJustice | `false_justice` | в инвентаре | весь наносимый и получаемый урон становится True Damage; отключаемо `falseJusticeEnabled` | 10 |
| ItemFateTome | `fate_tome` | в инвентаре | при смертельном уроне — полное лечение + баффы за вис, кулдаун 30–90 с, шанс дебаффов; несколько томов — катастрофа | 6 |
| ItemOmegaCore | `omega_core` | в инвентаре | только креатив: +1 каждого первичного виса в случайную палочку каждый тик | – |
| ItemParadox | `paradox` | меч | материал `PARADOXICALSTUFF` (уровень 4, 3000 прочн., 16 скорость, −4 урон, 100 зачар.); случайный урон до `paradoxDamageCap`, отдача разницы владельцу | 8 |
| ItemApotheosis | `apotheosis` | том (удержание) | Babylon Weapons: 100 прямой / 75 по площади; вис | – (10 на иссл.) |
| ItemLunarFlares | `lunar_flares` | том (удержание) | Lunar Flares с неба: 72 прямой / 40 по площади; звук `lunarFlare` | – |
| ItemMissileTome | `missile_tome` | том (удержание) | Nuclear Fury: самонаводящиеся снаряды в 32 блоках, проходят сквозь блоки; урон min..max из конфига | – |
| ItemChaosTome | `chaos_tome` | том (удержание) | Orbs of Primal Energy: хаотично/в случайные цели (включая владельца), взрыв, кап `chaosTomeDamageCap` | 4 |
| ItemCrimsonSpell | `crimson_spell` | том | Crimson Orb: медленный самонаводящийся снаряд, гибнет о блоки; урон min..max | – |
| ItemEldritchSpell | `eldritch_spell` | том | Dark Matter Orb: быстрый прямой снаряд, неблокируемый урон + слабость; усилен во Внешних землях | – |
| ItemSoulTome | `soul_tome` | том (удержание) | аура 20 блоков, высасывает здоровье (делитель `soulTomeDivisor` 10), отталкивает ближе 4 блоков, замедляет владельца | – |
| ItemTelekinesisTome | `telekinesis_tome` | том (удержание) | захват сущности взглядом, удержание дистанции (Shift), ЛКМ — молния (≤16 м), Shift+ЛКМ — бросок | – |
| ItemTeleportationTome | `teleportation_tome` | том | Discord Tome: телепорт к блоку взгляда до 128, обмен местами с сущностью, Shift — 16 блоков вперёд сквозь стены | – |
| ItemDimensionalMirror | `dimensional_mirror` | предмет (удержание) | Shift+ПКМ сохраняет точку и измерение, удержание — телепорт (межмировой — конфиг) | – |
| ItemWeatherStone | `weather_stone` | предмет (удержание) | разгоняет дождь за вис Aer/Terra/Aqua | – |
| ItemObeliskDrainer | `obelisk_drainer` | предмет (удержание) | Devourer of the Void: тянет энергию из Eldritch Obelisk в 16 блоках → здоровье, голод, вис палочек | 3 |
| ItemXPTome | `xp_tome` | в инвентаре | хранит опыт: Absorption / Extraction / Deactivated, Shift+ПКМ вкл/выкл, ПКМ режим | – |
| ItemGhastlySkull | `ghastly_skull` | отладочный | ПКМ ставит здоровье 1 + вспышка; иконка категории | 3 |
| ItemTelekinesisTomeLegacy | — | не зарегистрирован | мёртвый код, не переносится | – |

### 1.2 Сущности (8) и клиентские эффекты

| Старая сущность | Новая | Используется в | Зависит от |
|---|---|---|---|
| EntityBabylonWeaponSS (`RenderBabylonWeaponSS`) | `babylon_weapon` | Apotheosis | Botania `EntityBabylonWeapon`, `RenderBabylonWeapon` |
| EntityLunarFlare | `lunar_flare` | Lunar Flares | `EntityAIProjectileBase` |
| EntityRageousMissile | `rageous_missile` | Nuclear Fury | Botania `EntityMagicMissile`-подобная логика |
| EntityChaoticOrb | `chaotic_orb` | Chaos Tome | `EntityAIProjectileBase` |
| EntityCrimsonOrb (`RenderCrimsonOrb`) | `crimson_orb` | Crimson Spell | – |
| EntityDarkMatterOrb | `dark_matter_orb` | Eldritch Spell | Botania `EntityThrowableCopy` |
| EntitySoulEnergy | `soul_energy` | Soul Tome | – |
| EntityShinyEnergy | `shiny_energy` | Soul Tome / Obelisk Drainer | – |
| FXBurst, FXWisp | частицы Botania Revived (`WispParticleData`, `SparkleParticleData`) + свои частицы при нехватке | везде | клиент |

### 1.3 Сеть (7 пакетов → S2C payload)

PortalTraceMessage, BurstMessage, ApotheosisParticleMessage, LunarBurstMessage, LunarFlaresParticleMessage,
LightningMessage, ArcLightningMessage. Все — сервер→клиент, только частицы. Там, где хватает
`ServerLevel.sendParticles` с `ParticleOptions` Botania, пакет не нужен; молнии (`LightningMessage`,
`ArcLightningMessage`) → `BoltParticleOptions` Botania или собственный payload.

### 1.4 Типы урона (`DamageRegistryHandler`)

`trueDamage` (магия, абсолютный, сквозь броню, в креативе), `trueDamageUndef`, `soulAttack`, `attackOblivion`,
`attackFate`, `attackLightning`, `attackDarkMatter` → datapack `damage_type` + теги `minecraft:bypasses_armor`,
`bypasses_invulnerability`, `bypasses_effects`, `bypasses_resistance`, `bypasses_enchantments`, `is_magic`… по флагам.

### 1.5 Исследования (категория `ForgottenRelics`, 28 записей) и рецепты (26 инфузий)

Полные данные извлечены скриптом `tools/extract_original.py` в `tools/data/original_research.json` (ключ, позиция,
сложность, флаги, родители, скрытые родители, item triggers, аспекты, страницы, рецепты, варп). Это единственный
источник для генераторов данных.

| Ключ | Поз. | Флаги | Родители (+ скрытые TC4) | Рецепт: центр → выход, нестабильность | Варп |
|---|---|---|---|---|---|
| GenericTheory | 0,0 | Round, Special | (INFUSION, THAUMIUM) | – | – |
| SuperpositionRing | 3,1 | – | GenericTheory (ELDRITCHMINOR) | blankRing → ring, 4 | – |
| WeatherStone | −3,−1 | Concealed | GenericTheory (AdvancedMiningCharm) | arcaneStone, 4 | – |
| MiningCharm | 3,−1 | Concealed, Round | GenericTheory (ELEMENTALPICK) | reachRing, 1 | – |
| AdvancedMiningCharm | 5,−3 | Hidden, trigger gaiaSpirit | MiningCharm | miningCharm, 8 | – |
| DimensionalMirror | 1,−3 | Concealed, Special | GenericTheory (MIRRORHAND, FOCUSPORTABLEHOLE, AdvancedMiningCharm) | handMirror, 6 | 1 |
| AncientAegis | 5,3 | Hidden, triggers elementium/dragonstone/dreamwood | SuperpositionRing (RUNICKINETIC, ENCHFABRIC) | tectonicGirdle, 5 | – |
| XPTome | −5,−3 | Concealed | WeatherStone (JARBRAIN, NITOR) | writableBook, 4 | 2 |
| SpellbookTheory | −1,2 | Concealed, Round | GenericTheory (CRIMSON) | – | – |
| DiscordTome | −2,5 | Hidden, triggers как Aegis | SpellbookTheory (FOCUSPORTABLEHOLE) | writableBook, 5 | 3 |
| FateTome | −4,2 | Concealed, Special | SpellbookTheory (ELDRITCHMAJOR, AdvancedMiningCharm) | writableBook, 20 | 8 |
| TelekinesisTome | −4,7 | – | DiscordTome (ELDRITCHMINOR, FOCUSSHOCK, AdvancedMiningCharm) | writableBook, 8 | – |
| ObeliskDrainer | 0,7 | Concealed | DiscordTome (ELDRITCHMAJOR, VOIDMETAL, ENCHFABRIC, NITOR, OCULUS) | writableBook, 8 | 4 |
| EldritchSpell | 2,6 | Concealed, Special | ObeliskDrainer (AdvancedMiningCharm) | writableBook, 8 | 6 |
| CrimsonSpell | 3,8 | Concealed, Hidden, trigger crimsonRites | EldritchSpell | writableBook, 8 | 5 |
| ChaosTome | 2,4 | – | EldritchSpell (ROD_primal_staff, AdvancedMiningCharm) | writableBook, 12 | 7 |
| NuclearFury | 4,5 | Concealed | ChaosTome (PRIMPEARL) | chaosTome, 10 | 4 |
| SoulTome | −1,9 | Concealed, Hidden, trigger pechFocus | ObeliskDrainer (JARBRAIN) | writableBook, 10 | 3 |
| LunarFlares | −5,9 | – | TelekinesisTome (EldritchSpell, PRIMPEARL) | writableBook, 12 | – |
| Apotheosis | −2,7 | Round, Special | DiscordTome (+11 реликвий) | writableBook, 48 | 10 |
| ChaosCore | 7,−2 | Concealed, Round | SuperpositionRing (VOIDMETAL, ALUMENTUM, AncientAegis) | primalCharm, 10 | 2 |
| TheParadox | 6,0 | Concealed, Secondary | SuperpositionRing (ChaosCore, PRIMPEARL) | voidSword, 32 | 8 |
| DarkSunRing | 7,2 | Concealed | SuperpositionRing (ELDRITCHMINOR, NITOR, RUNICCHARGED, AdvancedMiningCharm) | runicRingCharged, 8 | 3 |
| DeificAmulet | 3,−4 | Concealed | MiningCharm (RUNICEMERGENCY, AdvancedMiningCharm) | runicAmuletAdv, 4 | – |
| ShinyStone | 6,−5 | Hidden, trigger superGoldenApple | DeificAmulet | dragonStone, 8 | – |
| OblivionAmulet | −3,−4 | Concealed, Hidden, Special, trigger netherStar | XPTome (ELDRITCHMAJOR, OCULUS, PRIMPEARL) | bloodPendant, 16 | 8 |
| NebulousCore | 0,−5 | Hidden, Special, trigger superpositionRing | DeificAmulet (PRIMPEARL, VISAMULET, SuperpositionRing, THAUMIUM) | primordialPearl, 12 | – |
| FalseJustice | −7,0 | Round, Special | (Apotheosis) | writableBook, 24 | 4 |

### 1.6 События (`RelicsEventHandler`, `SuperpositionHandler`)

| Событие | Предметы | Новое событие |
|---|---|---|
| `PlayerEvent.BreakSpeed` | Mining Charm, Advanced Mining Charm | `PlayerEvent.BreakSpeed` |
| `LivingAttackEvent` | Chaos Core, Nebulous Core (уклонение), Dark Sun Ring (огонь, отражение) | `LivingIncomingDamageEvent` |
| `LivingHurtEvent` | False Justice, Chaos Core, Dark Sun Ring, Ancient Aegis, Superposition Ring, Oblivion Amulet | `LivingIncomingDamageEvent` / `LivingDamageEvent.Pre` |
| `LivingDeathEvent` | False Justice, Omega Core, Fate Tome | `LivingDeathEvent` |

Точные константы переписываются из кода при реализации каждого milestone и становятся ожидаемыми значениями тестов.

### 1.7 Конфиг (`RelicsConfigHandler`, 45 опций)

Generic Config (досягаемость/скорость амулетов, шансы, кап урона, межмировое зеркало, `falseJusticeEnabled`…),
Damage Values (урон всех томов), множители виса для каждого предмета. Переносится 1:1 в `ModConfigSpec`
(`forgottenrelics-common.toml`) с теми же именами и значениями по умолчанию.

### 1.8 Интеграции

| Мод | Для чего | Что на 26.1.2 |
|---|---|---|
| Thaumcraft 4 | вис палочек, исследования, инфузия, варп, аспекты, Eldritch Obelisk, Outer Lands | Thaumaturge 1.1.0 |
| Botania | ингредиенты рецептов, частицы, Babylon Weapon, extra reach, проверка Gaia/фейк-игрока, `Vector3`, `ItemNBTHelper`, аспекты на блоки Botania | Botania Revived 2.0.2 |
| Baubles | слоты | Curios |

## 2. Архитектура целевой платформы

| Подсистема | Как устроено | Расширяется аддоном? |
|---|---|---|
| Аспекты предметов | data map `thaumaturge:base_aspects` | данные |
| Аспекты предметов Botania | уже есть во встроенном паке Botania Revived `resourcepacks/thaumaturge_aspects` | не нужно, `RelicsAspectRegistry` для Botania отбрасывается |
| Исследования | `data/<ns>/thaumaturge/research_category|research_entry/*.json`, стадии, заметки, знания | данные |
| Инфузия | рецепт `thaumaturge:infusion` | данные |
| Вис палочек | компонент `thaumaturge:wand_vis`, `WandAccess` (API), запись через компонент (как `WandBridge` в FM) | API + свой хелпер |
| Скидка виса | `IVisDiscountGear` | API |
| Варп | `IWarpingGear`, `WarpHelper`, `"warp"` на стадии исследования | API / данные |
| Eldritch Obelisk | `content.eldritch.block.BlockEntityEldritchObelisk` (internal) | только через `instanceof`/id блока — изолировать в `compat` |
| Внешние земли | измерение `thaumaturge:...` (папка `dimensions/thaumaturge` в сейвах) | проверка `level.dimension()` |
| Слоты | Curios (`ICurioItem`, теги `curios:ring|necklace|belt`) | API |
| Мана / частицы Botania | `velrondevs.botania.api.*`, `client.fx.*ParticleData` | API (Botania Revived не гарантирует стабильность API — изолировать в `compat/BotaniaBridge`) |
| Боссы | тег `c:bosses` + `GaiaGuardianEntity` | данные/API |

## 3. Соответствия старого и нового

### 3.1 Системы

| Старое | Новое |
|---|---|
| `GameRegistry.registerItem` | `DeferredRegister.Items` |
| `EntityRegistry.registerModEntity` | `DeferredRegister<EntityType<?>>` + рендеры в `EntityRenderersEvent` |
| NBT предмета (`ItemNBTHelper`) | data components (`FRDataComponents`): XP Tome (опыт, режим), Mirror (точка, измерение), Oblivion (урон), Fate Tome (кулдаун), Telekinesis (цель) |
| `WandManager.consumeVisFromInventory(player, AspectList)` | `VisHelper.consumeFromInventory(player, aspects)` поверх `WandAccess` (сантивис, как TC4) |
| `ItemWandCasting.addVis` | `VisHelper.addVis` |
| `SimpleNetworkWrapper` | `RegisterPayloadHandlersEvent`, S2C payload-ы или `sendParticles` |
| `Botania.proxy.setExtraReach` | атрибут `minecraft:block_interaction_range` через модификатор Curios |
| `Botania.proxy.wispFX/sparkleFX` | `WispParticleData` / `SparkleParticleData` Botania Revived |
| `EntityDoppleganger.isTruePlayer` | `!(player instanceof FakePlayer)` |
| `EntityThaumcraftBoss \|\| EntityDoppleganger` | тег `c:bosses` + `instanceof GaiaGuardianEntity` |
| `ExperienceHelper` | собственный `XpHelper` (формулы ванильного опыта) |
| `IRepairable` (TC4 ремонт) | атрибут TT — уточнить; иначе тег ремонта |
| `.lang` | `assets/forgottenrelics/lang/en_us.json`, `ru_ru.json` (генератор) |
| `sounds.json` (`sound.discharge`…) | `SoundEvent` + `sounds.json` с ключами `discharge`, `lunar_flare`, `starfall` |
| `EnumRarity.epic` | `Rarity.EPIC` |

### 3.2 Аспекты TC4 → TT (по таблице скилла `thaumcraft-to-thaumaturge`)

TT 1.1.0 содержит 37 аспектов TC6 (проверено по release jar): aer alienis alkimia aqua auram aversio bestia cognitio
desiderium exanimis fabrico gelum herba humanus ignis instrumentum lux machina metallum mortuus motus ordo perditio
permutatio potentia praecantatio praemunio sensus spiritus tenebrae terra vacuos victus vinculum vitium vitreus volatus.

Прямые соответствия: DARKNESS=tenebrae, ELDRITCH=alienis, EXCHANGE=permutatio, AIR=aer, WATER=aqua,
TOOL=instrumentum, MOTION=motus, MAN=humanus, AURA=auram, MAGIC=praecantatio, MIND=cognitio, VOID=vacuos,
LIFE=victus, FLIGHT=volatus, TRAP=vinculum, FIRE=ignis, ENTROPY=perditio, ORDER=ordo, EARTH=terra, ENERGY=potentia,
LIGHT=lux, SOUL=spiritus, DEATH=mortuus, SENSES=sensus, CRYSTAL=vitreus, METAL=metallum, TAINT=vitium, CRAFT=fabrico.

TC4-аспекты, которых в TT нет (как в TC6), и выбранная замена:

| TC4 | TT | Почему |
|---|---|---|
| TRAVEL (iter) | motus | в TC6 iter слит с motus |
| WEATHER (tempestas) | aer | в TC6 погода убрана; ближе всего воздух |
| MINE (perfodio) | instrumentum | в TC6 perfodio слит с instrumentum |
| ARMOR (tutamen) | praemunio | TC6-аналог защиты |
| HEAL (sano) | victus | в TC6 лечение — victus |
| WEAPON (telum) | aversio | TC6-аналог, так же сделано в Forbidden Magic |
| GREED (lucrum) | desiderium | TC6-аналог |

При слиянии двух TC4-аспектов в один TT-аспект количества складываются.

Масштаб: аспекты в рецептах инфузии и item aspects ×2 (как в Forbidden Magic), применяется в генераторе.

### 3.3 Ингредиенты (зафиксировано в M3; итоговая таблица — `INGREDIENTS` в `tools/gen_data.py`, все id проверены по jar-ам TT/Botania/MC)

Решения по «❓» ниже: wisp essence → `essentia_crystal` с аспектом `auram`; portable hole → `thaumaturge:focus_2`;
shock focus → `focus_1`; primal focus → `focus_3`; pech focus → `thaumaturge:pech_wand` (тоже добыча с печей);
golden laurel → `thaumaturge:charm_undying` (тот же смысл — спасение от смерти, как у Fate Tome).

| TC4 / Botania 1.7 | 26.1.2 |
|---|---|
| itemShard 0..5 (aer…perditio) | `thaumaturge:essentia_crystal` + `crystal_aspect` |
| itemResource 1 nitor / 0 alumentum / 14 salis / 15 primal charm / 16 void ingot / 17 void seed / 6 amber / 7 fabric / 2 thaumium | `nitor_yellow` / `alumentum` / `salis_mundus` / `primal_charm` / `ingot_void` / `void_seed` / `amber` / `fabric` / `ingot_thaumium` |
| itemResource 9 knowledge fragment | `thaumaturge:curio_knowledge` |
| itemBaubleBlanks 1 (ring) | `thaumaturge:ring_mundane` |
| itemAmuletVis 1 | `thaumaturge:amulet_vis_crafted` |
| itemRingRunic 2 / itemAmuletRunic 1 / itemGirdleRunic 1 | `ring_fancy` / `amulet_fancy` / `girdle_fancy` (в TT нет рунических баублов — щит через тег `thaumaturge:runic_shieldable`; отклонение) |
| itemEldritchObject 0 / 1 / 3 / 13 | `eldritch_eye` / `curio_rites` / `primordial_pearl` / `runed_tablet` |
| itemHandMirror | `thaumaturge:hand_mirror` |
| blockJar 1 (brain) | `thaumaturge:jar_brain` |
| itemBottleTaint | `thaumaturge:bottle_taint` |
| blockCosmeticSolid 6 (arcane stone) | `thaumaturge:stone_arcane` |
| blockCustomPlant 3 cinderpearl | `thaumaturge:cinderpearl` |
| itemPickElemental / itemPickVoid / itemSwordVoid | `elemental_pickaxe` / `void_pickaxe` / `void_sword` |
| itemWispEssence | ❓ нет в TT → `essentia_crystal` с аспектом `auram` (или `phial` с эссенцией) |
| itemFocusPortableHole / FocusShock / FocusPrimal / FocusPech | ❓ фокусы TT собираются из spell parts; замена: `focus_2`/`focus_3` или `primal_charm`/`pech_wand` |
| Botania manaResource 5 gaia spirit / 4 terrasteel / 7 elementium / 8 pixie / 9 dragonstone / 15 ender air / 19 elementium nugget | `botania:life_essence` / `terrasteel_ingot` / `elementium_ingot` / `pixie_dust` / `dragonstone` / `ender_air_bottle` / `elementium_nugget` |
| Botania rune 3, reachRing, knockbackBelt, lavaPendant, superLavaPendant, bloodPendant, gravityRod, missileRod, starSword, kingKey, dreamwood, storage 2/4 | `rune_air`, `reach_ring`, `knockback_belt`, `lava_pendant`, `super_lava_pendant`, `blood_pendant`, `gravity_rod`, `missile_rod`, `star_sword`, `king_key`, `dreamwood`, `elementium_block`, `dragonstone_block` |
| Botania goldLaurel | ❓ нет в Botania Revived → замена (например `botania:gaia_head` или `minecraft:totem_of_undying`), решить в M3 |
| зелья 8226 (Speed II) / 8229 (Healing II) | `minecraft:potion` с `potion_contents` `strong_swiftness` / `strong_healing` |
| golden_apple:1 | `minecraft:enchanted_golden_apple` |

### 3.4 Ключи исследований TC4 → TT (зафиксировано в M3, `TC4_RESEARCH` в `tools/gen_data.py`; ENCHFABRIC →
`unlock_infusion`, NITOR → `unlock_alchemy` — записи TT, открывающие рецепт ткани и нитора)

INFUSION→`infusion`, THAUMIUM→`metallurgy`, ELDRITCHMINOR→`base_eldritch`, ELDRITCHMAJOR→`outer_revelations`,
ELEMENTALPICK→`elemental_tools`, MIRRORHAND→`mirror_hand`, FOCUSPORTABLEHOLE→`focus_rift`, FOCUSSHOCK→`focus_bolt`,
RUNICKINETIC/RUNICCHARGED/RUNICEMERGENCY→`runic_shielding`, ENCHFABRIC→`base_infusion`, JARBRAIN→`jar_brain`,
NITOR→`base_alchemy`, OCULUS→`oculus`, CRIMSON→`crimson_rites`, ROD_primal_staff→`staff_primal`,
PRIMPEARL→`primordial_pearl`, VOIDMETAL→`base_eldritch`, ALUMENTUM→`alumentum`, VISAMULET→`vis_amulet`.
Скрытые родители TC4 → родители `~ns:id` (без линии) в TT.

## 4. Переносится / адаптируется / отбрасывается

**Переносится как есть (логика):** все 28 реликвий (кроме мёртвого Legacy-тома), 8 сущностей, 28 исследований,
26 инфузий, 45 опций конфига, текстуры, звуки, en/ru тексты.

**Адаптируется:**
- Вис палочек: TC4 тратил вис из палочки в инвентаре — переносится через `WandAccess` + компонент `wand_vis`.
- Наведение Botania-частиц и Babylon Weapon — через API Botania Revived; рендер Babylon Weapon — свой рендерер,
  использующий модели оружия (не копируем класс Botania).
- Extra reach — атрибут `block_interaction_range`.
- Предметы-триггеры исследований (`setItemTriggers`) → стадии с `required_items`/скан (см. data-formats скилла).
- Рунические баублы TC4 → «fancy» баублы TT (рунического щита нет как отдельных предметов).
- TC4 сложность исследований и TC4-страницы → стадии TT (`stage_N` текстовые ключи) с `note_aspects` из аспектов
  исследования оригинала.
- Аспекты предметов Botania не добавляем: их уже даёт Botania Revived.

**Отбрасывается:**
- `ItemTelekinesisTomeLegacy` — не зарегистрирован в оригинале.
- `RelicsAspectRegistry` для блоков/предметов Botania — дублирует данные Botania Revived.
- Ачивки Botania (`ModAchievements`) — в 26.1 нет; при необходимости своё advancement.

## 5. План работ

| # | Milestone | Зависит от | Критерий проверки |
|---|---|---|---|
| M0 | Планирование: инвентарь, карта, план, прогресс, скиллы в `.claude/skills` | – | документы в корне проекта |
| M1 | Каркас: gradle (MDG 2.0.148, JDK 25), mods.toml, зависимости TT/Botania Revived/Patchouli/Curios/Lithostitched, главный класс, пустые реестры, конфиг | M0 | `runServer` доходит до `Done (`, в логе нет ERROR от `forgottenrelics`, список модов содержит thaumaturge, botania, patchouli, curios |
| M2 | Реестр 29 предметов, вкладка, модели/текстуры, lang en/ru, звуки; damage types | M1 | GameTest: все id зарегистрированы; `check_assets.py`/`check_lang.py` без ошибок; клиентский скриншот вкладки |
| M3 | Данные TT: аспекты предметов, категория и 28 исследований, 26 инфузий, варп; таблица ингредиентов зафиксирована | M2 | сервер грузится без ошибок кодеков; `aspect_index.json` содержит наши предметы; GameTest: рецепты/исследования есть в реестрах |
| M4 | Общие системы: `VisHelper`, `CuriosBridge`, `BotaniaBridge`, `RelicsDamageSources`, payload-ы частиц | M2 | GameTest: списание/начисление виса палочке в инвентаре; поиск надетого кольца |
| M5 | Баублы: Superposition Ring, Mining Charm ×2, Dark Sun Ring, Ancient Aegis, Deific Amulet, Shiny Stone, Oblivion Amulet, Nebulous Core (+dormant) | M4 | GameTest на каждую формулу (уклонение 40 %, −25 %, кап 100, BreakSpeed ×2/×4, перенос урона) |
| M6 | Пассивные реликвии в инвентаре: Chaos Core, False Justice, Fate Tome, Omega Core, XP Tome | M4 | GameTest: true damage, спасение от смерти + кулдаун, сохранение опыта в компоненте |
| M7 | Утилитарные: Dimensional Mirror, Weather Stone, Teleportation Tome, Obelisk Drainer, Ghastly Skull, Paradox | M4 | GameTest: телепорт/сохранение точки, отключение дождя, урон Paradox в диапазоне |
| M8 | Сущности и боевые тома: Crimson/Eldritch Spell, Chaos Tome, Nuclear Fury, Lunar Flares, Apotheosis, Soul Tome, Telekinesis Tome | M4, M7 | GameTest: снаряд спавнится, наносит урон из конфига, вис списан; клиентский скриншот рендера |
| M9 | Клиент: рендеры сущностей, частицы, тултипы (Shift/Ctrl как в оригинале) | M8 | автоматический клиентский прогон со скриншотами |
| M10 | Полировка: ru/en перевод исследований, баланс аспектов, финальная проверка, README | всё | полный прогон GameTest, проверка ассетов/языков, статус в PROGRESS.md |

## 6. Отклонения (заполняется по мере реализации)

- Botania: используется неофициальный Botania Revived (пакет `velrondevs.botania`), по просьбе пользователя.
- Аспекты Botania: не дублируются, берутся из Botania Revived.
- Рунические баублы TC4 в рецептах → fancy-баублы TT.
- Curios 15.0.0 (релиз) вместо 15.0.0-beta.2 из Forbidden Magic: Botania Revived требует `curios ≥ 15.0.0`.
- Paradox: материал уровня 4 → тег `incorrect_for_netherite_tool`; базовый урон меча задан 4.0 (вместо ванильных 3.0),
  чтобы итог совпал с 1.7.10: `4 + (−4)` бонуса материала + 1 базовый урон игрока. Тег ремонта
  `forgottenrelics:paradox_repair_materials` пустой: в TC4 Paradox чинился только инфузионным чаром Repair.
- Флаги урона 1.7 → теги: magic → `neoforge:is_magic` + `minecraft:witch_resistant_to`; absolute →
  `bypasses_resistance` + `bypasses_enchantments`; bypassArmor → `bypasses_armor` + `bypasses_shield`;
  allowedInCreative → `bypasses_invulnerability`. Id типов урона переименованы (`trueDamage` → `true_damage`, …),
  сообщения о смерти перенесены под `death.attack.forgottenrelics.<id>`; для `soul_drain` текста в оригинале не было —
  добавлен свой.
- Ресурсы переименованы в snake_case (требование 26.1): текстуры `textures/item/<id>.png`, звук `lunarFlare` →
  `lunar_flare`. Ключи подсказок оригинала сохранены как `lore.forgottenrelics.<старый ключ>`.
- Ghastly Skull не имел имени в lang оригинала — добавлены «Ghastly Skull» / «Призрачный череп»; вкладка в ru_RU
  была на английском — переведена как «Забытые реликвии» (так называлась категория исследований).
- **Исследования (M3).** Категория `forgottenrelics:forgottenrelics` открывается после `thaumaturge:unlock_infusion`
  (в оригинале GenericTheory требовал INFUSION и THAUMIUM), index 11, фон TT `gui_research_back_1_smooth`, иконка —
  Ghastly Skull, формула — 8 самых частых аспектов исследований. Каждая запись: стадия 0 (текст — подзаголовок
  `fr.tag`, требует 1 знание theory при сложности ≥ 2, иначе observation) и стадия 1 (все страницы оригинала через
  `<PAGE>`, рецепт, варп исследования). Скрытые родители TC4 → родители `~ns:id` (без линии). Флаги: Round → `round`,
  Special → `spiky`, Hidden → `hidden`; Concealed и Secondary в TT аналогов не имеют и отброшены.
- Item triggers TC4 (8 скрытых записей) → `data/forgottenrelics/thaumaturge/scan/<entry>.json` с ключом
  `forgottenrelics:scanned/<entry>` как родителем записи — та же схема, что у TT (`infusion_eldritch`).
- Опечатка оригинала: Apotheosis имел скрытого родителя `NebulosCore` (несуществующий ключ) — заменён на
  `nebulous_core`, как задумывалось.
- Аспекты заметок (`note_aspects`): у TT не больше 5 аспектов и сумма ≤ 36 (проверено по 202 записям TT), у оригинала
  до 8 и 60 — берутся 5 крупнейших, сумма пропорционально ужимается до 36.
- Инфузия: аспекты рецептов ×1 (у TT масштаб как у TC4: jar_brain 25/25/25). Нестабильность TC4 1..48 → TT 1..10 по
  `round(2 + tc4/6)`, порядок сохраняется (Apotheosis 48 → 10, Paradox 32 → 7, Mining Charm 1 → 2).
- Аспекты предметов-реликвий ×2 (правило Forbidden Magic). Superposition Ring и Paradox в оригинале аспектов не имели —
  TT выводит их из рецептов инфузии.
- **Не перенесено в M3:** варп за крафт (`addWarpToItem`). В TT data map `thaumaturge:warp` прибавляется к варпу
  снаряжения, а не начисляется за крафт, поэтому будет обработчик `InfusionCraftedEvent` → `WarpHelper.addWarp` (M4).
  Отключение исследования False Justice конфигом `falseJusticeEnabled` — через NeoForge condition (M6).
- **Общие системы (M4).** Вис: `compat/VisHelper` — TC4 `consumeVisFromInventory` обходит основной инвентарь с
  последнего слота и платит первой палочкой, у которой хватает всего (через `WandVisHelper.payCosts` TT со скидкой
  крафта, как TC4); единицы — сантивис (100 = 1 вис). Палочки — тег `thaumaturge:wands`. TC4 сначала тратил вис из
  надетого Vis Amulet — в TT амулеты берут вис из ауры и хранилища не имеют, поэтому шаг пропущен.
  `WandVisHelper` — внутренний класс TT (`content`), используется только в `VisHelper`.
- Баублы: `item/RelicCurioItem` (`ICurioItem`, Curios сам вешает capability). Атрибуты баубла (`fillModifiers`)
  → `getAttributeModifiers` Curios; надевание ПКМ запрещено фейк-игрокам (как `isTruePlayer`); звук надевания —
  `botania:equip_bauble` 0.1/1.3. Косметические вставки и фантомные чернила Botania не перенесены.
- `findPlayerWithBauble` сохраняет выбор оригинала `(int)((n − 1) · random)` — последний из нескольких кандидатов
  никогда не выбирается.
- `validTeleport`: координаты округляются вниз (`floor`), а не `(int)`, иначе на отрицательных координатах цель
  сдвигалась на блок. «Твёрдый блок» = не воздух и непустая форма коллизии.
- Боссы: тег `forgottenrelics:relic_bosses` (`#c:bosses`, `botania:doppleganger`, 6 боссов TT) вместо
  `instanceof EntityThaumcraftBoss / EntityDoppleganger`. «Абсолютный» урон: тег `forgottenrelics:absolute`.
- Вспышка TC4 `burst` (текстура узла) временно — кольцо виспов Botania тех же цветов; своя частица в M9.
  Частицы Botania отправляет сервер (`sendParticles`), отдельные пакеты для виспов/искр не нужны.
- Варп за крафт: `event/FRCraftingWarp` на `InfusionCraftedEvent`, половина (вниз) — PERMANENT, остаток — NORMAL
  (по аналогии с разбиением варпа исследований TC4; точный код TC4 TileInfusionMatrix не сверялся).
- Проверка «надет ли баубл» обходит слоты Curios напрямую: `isEquipped`/`findFirstCurio` отвечают из кэша, который
  обновляется на тике инвентаря, и запрос до надевания давал устаревший ответ (найдено тестом `systems_curios_bauble`).
- **Баублы (M5).** События: 1.7 `LivingAttackEvent` и `LivingHurtEvent` → один `LivingIncomingDamageEvent`;
  «attack»-обработчики на HIGHEST, «hurt» — на NORMAL и пропускают удары, которые отсекут кадры неуязвимости
  (в 1.7 LivingHurtEvent для них не срабатывал). `hurtResistantTime` → `invulnerableTime`.
- Ring of the Seven Suns: в оригинале кап урона в обработчике зашит как `100.0F`, а опция `darkSunRingDamageCap`
  влияла только на подсказку; теперь кап берётся из опции (по умолчанию те же 100), как обещает её описание.
- Shiny Stone: оригинал делил `checkrate / 4` нацело и при `shinyStoneCheckrate` 1..3 падал на `% 0`; делитель
  ограничен снизу единицей. EntityShinyEnergy (только визуал) пока заменена вспышкой искр Botania — сущность в M8.
- Nebulous Core: выбор первичного аспекта `(int)(random · 5)` сохранён — перфидио оригинал никогда не генерировал.
  Ядро засыпает/просыпается в том же слоте Curios, где надето (в оригинале — всегда слот амулета 0).
- Состояние в NBT → компоненты данных: `stored_damage` (Oblivion), `dormant_lifetime`, `relic_cooldown` (Deific),
  `stand_state` (Shiny Stone).
- Подсказки: `lore()` берёт перевод строкой (`Language`), как `StatCollector.translateToLocal`, чтобы цветовые коды
  и числа склеивались как в оригинале. Shift/Ctrl — через `TooltipKeys`, клиент подставляет `Minecraft#hasShiftDown`.
- **Пассивные реликвии (M6).** Проверка «в инвентаре» — основной инвентарь (36 слотов), как `inventory.hasItem`
  в 1.7. Порядок обработчиков: «attack» — Chaos Core атакующего (45 %), Chaos Core жертвы (42 %), Nebulous Core,
  Seven Suns; «hurt» — False Justice, множитель Chaos Core, затем баублы M5. Выбор случайной сущности Chaos Core
  `(int)(random · (n − 1))` сохранён (последняя из нескольких не выбирается).
- False Justice: отмена смерти без лечения (как в оригинале) — с ним игрок не умирает ни от чего, кроме истинного
  урона; смерть от абсолютного урона (пустота, голод, судьба) остаётся «отменённой» при нулевом здоровье, как в 1.7.
  Опция `falseJusticeEnabled` убирает запись исследования и рецепт через условие
  `forgottenrelics:false_justice_enabled` (`neoforge:conditions`) — проверено прогоном с выключенной опцией.
- Tome of Broken Fates: стоимость 100 вис каждого первичного (× `fateTomeVisMult`) считается в момент спасения, а не
  один раз при загрузке класса, как в оригинале (там смена опции требовала перезапуска).
- Tome of Ageless Wisdom: `ExperienceHelper` Botania 1.7 считал уровни по кривой 1.7.10; порт считает по текущей
  кривой Minecraft (`XpHelper`), иначе уровни игрока и тома расходились бы. Переключение — только на сервере.
- Звуки TC4 `hhon/hhoff/runicShieldCharge` → одноимённые звуки Thaumaturge (с запасным ванильным).
- **Утилитарные реликвии (M7).** Удержание ПКМ: `onUsingTick(count)` → `onUseTick(remaining)` (тот же отсчёт
  вниз, последний тик — 1). Действия выполняет только сервер; в 1.7 тома тратили вис и на клиенте.
- Tome of Discord: оригинал соединял условия через `&` (не `&&`), поэтому `consumeVisFromInventory` вызывался в
  каждой проверенной ветке и обычный телепорт к блоку списывал вис до трёх раз. Порт списывает один раз — так, как
  написано в подсказке («Vis per cast»). Поиск сущности TC4 `EntityUtils.getPointedEntity` → `ProjectileUtil`
  (луч 128 блоков, только живые сущности).
- Dimensional Mirror: измерение хранится как `GlobalPos` (в подсказке — id измерения вместо числа). Межмировой
  переход доступен только настоящему `ServerPlayer` (как и в оригинале — `EntityPlayerMP`).
- Runic Stone: погода — `MinecraftServer.setWeatherParameters(0, 24000..1000000, false, false)` (в 26.1 погода у
  сервера, а не у мира); как и в оригинале, гроза отдельно не снимается — без дождя её нет.
- Devourer of the Void: обелиск TT ищется по типу блок-сущности `thaumaturge:eldritch_obelisk` (без внутренних классов
  TT); берётся ближайший в 16 блоках, а не первый в списке загруженных. Молнии и «портальные» следы пока — линии
  ванильных частиц (`FRFx`), настоящие молнии — в M9.
- Ghastly Skull: строки подсказки `ItemGhastlySkull1/2` в языковых файлах оригинала отсутствовали — не выводятся.
- **Боевые тома (M8).** Снаряды — свой базовый класс `entity/FRProjectile` (на `Projectile`): без сопротивления
  воздуха, гравитация и столкновение с блоками — по образцу каждой 1.7-сущности (Rageous Missile, Lunar Flare и обе
  энергии пролетают сквозь блоки — их `onImpact` игнорировал блоки). Взрывы/урон по площади считаются от точки
  контакта. Частицы рисует клиент в `clientTick` (как `onUpdate` 1.7), рендер сущностей пока пустой (модели Babylon
  Weapon и Crimson Orb — M9). Сущности не сохраняются с чанком (`noSave`).
- Кулдауны Crimson/Eldritch Spell: в оригинале использовался `player.xpCooldown` (заодно блокировал подбор опыта),
  теперь — кулдаун предмета (`ItemCooldowns`, виден на иконке), те же 30/20 тиков.
- Crimson Spell брал множитель виса `chaosTomeVisMult` (опечатка оригинала) — теперь `crimsonSpellVisMult`.
- Lunar Flare бил жёстко заданными 100/75 вместо опций `damageLunarFlareDirect/Impact` (72/40), которые показывала
  подсказка, — теперь используются опции.
- Dark Matter Orb: оригинал отбрасывал результат `Math.abs`, и сферы, летевшие в сторону −x/−y/−z, лопались через
  1 с; порт проверяет модуль скорости (лопается только почти остановившаяся сфера). Внешние земли —
  `thaumaturge:outer_lands`.
- Orb of Primal Energy: в особом исходе оригинал в половине случаев создавал случайный узел ауры TC4 — не перенесено
  (узлы TT не создаются из API); вторая половина (заражение) — через `TaintApi.spreadFibres`.
- Apotheosis: направление дуги берётся из точного горизонтального вектора взгляда (оригинал приближал его кусочно по
  yaw); оружие ищет свободное место в 2 блоках от других, как в оригинале.
- Nuclear Fury: формула высоты старта `posY + 3.8 + (random − 0.5 · 3.1)` (опечатка оригинала, 2.25..3.25 над ногами)
  сохранена.
- Tome of Predestiny: левый клик — `onEntitySwing` (пакет взмаха приходит на сервер и при клике в воздух), вместо
  проверки `swingProgress` 1.7. Притягивание — повтор ПКМ (каждые 4 тика), как в 1.7.
- Shiny Stone теперь выпускает настоящую Shiny Energy (вместо вспышки искр M5).
- Неиспользуемые текстуры оригинала (`1Chaos_Core`, `2Omega_Core`, `Eye_of_Devastation`, `Mysterious_Prism`,
  `Timeless_Eye`, `Undefined_Spellbook` и др.) не перенесены: ни один предмет их не регистрировал.
- **Клиент (M9, сверено с TC4 4.2.2.0).** Пакеты `BurstMessage` и `ArcLightningMessage` не нужны: эффекты TT
  (`content.effect.Effects`, только из `compat/TTEffects`) — `burst` (TC4 рисовал полуразмер `particleScale · size`,
  `particleScale` 1..2; у TT `0.1 · size`, поэтому ×15) и `zapArc` (цвет и `h` как дрейф). `LightningMessage`
  (`FXLightningBolt`, фрактальная молния) — свой S2C-пакет `forgottenrelics:lightning` и `BoltRenderer` Botania Revived:
  `boltStrike` TT — короткий луч на 3 тика без ветвления, не аналог. Цвет type 0 — свечение (0.6, 0.3, 0.6) из TC4,
  ширина TC4 = размер Botania, длительность = время жизни; изгиб и скорость роста не переносятся. Молния Soul Tome теперь
  видна и на выделенном сервере (в оригинале `Main.proxy.lightning` там был пустым).
- Рендеры сфер по классам TC4: Crimson Orb — кадры golem-orb листа частиц (у TT отдельная лента `golem_orb_blue`, те
  же 6 кадров), Rageous Missile и Chaotic Orb — `RenderPrimalOrb` (12 лучей, зерно — id сущности, повороты
  накапливаются, кончики — цвета осколков TC4, ядро — 13 кадров вихря, у TT `flux_swirl`), Dark Matter Orb —
  `RenderEldritchOrb` (лучи цвета ordo, ядро `eldritch_orb`). Лист частиц TC4 в TT не поставляется; ресурсы TC4 в мод не
  копируются. Флаг `red` Crimson Orb оригинал никогда не ставил.
- Babylon Weapon: рендер повторяет `BabylonWeaponRenderer` Botania Revived (наследник `BotaniaEntityRenderer`, вложен в
  `BotaniaBridge.Client`, чтобы API Botania оставался в одном файле); задержка и live-тики синхронизируются, как data
  watcher 1.7.
- **Полировка (M10).** В ru_RU оригинала заголовок исследования Ethereal Mining Charm не переведён — генератор подставляет
  «Эфирный шахтёрский талисман» (`RU_FIXES`). `<LINE/>` TC4 → `<LINE>` TT; стих Oblivion Amulet был свёрстан пробелами под
  ширину страницы TC4 — серии из 3+ пробелов заменены на `<BR>`. Аспекты реликвий (×2 оригинала) сверены с вычисленным
  индексом TT и не менялись.

