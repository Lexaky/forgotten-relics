# Прогресс порта Forgotten Relics

Легенда: ✅ сделано и проверено · 🟡 сделано, проверено частично (что не проверено — указано) · ⬜ не начато.
Проверка: загрузка сервера (`runServer`, grep лога), GameTests (`runGameTestServer`), клиентский прогон со
скриншотами (`runClientCheck`), `tools/check_assets.py`, `tools/check_lang.py`. Компиляция проверкой не считается.

| Milestone | Статус | Что проверено (имена тестов) / **Не проверено** |
|---|---|---|
| M0 Планирование | ✅ | PORTING_PLAN.md, PROJECT_MAP.md, PROGRESS.md; скиллы в `.claude/skills`; инвентарь исследований/рецептов извлечён скриптом (28 / 26, совпадает с числом вызовов в оригинале); аспекты сверены с release jar TT; наличие Botania Revived 26.1.2 и Patchouli 26.1 подтверждено |
| M1 Каркас и зависимости | ✅ | `compileJava`, `build` → `forgotten-relics-1.1.0-26.1.2.jar`; `runServer` → `Done`; загружены thaumaturge 1.1.0, botania 2.0.2, patchouli 26.1-94, curios 15.0.0+26.1.2, lithostitched 1.7.13. Конфиг: 45/45 опций (`tools/gen_config.py`). Клиент: `runClientCheck` запускается, проходит 16 сценариев и снимает 18 скриншотов |
| M2 Предметы, ассеты, языки | ✅ | GameTests: `items_registered`, `items_gear_warp`, `items_paradox_sword`, `items_creative_tab`, `items_curio_slots`, `data_damage_types`, `data_sounds`. `check_assets.py` 29/29, `check_lang.py` без пропусков, en/ru 357/357 ключей. Клиент: `frcheck_items.png`, `frcheck_creative_tab.png`. Звуки: сценарий клиентского прогона `m2_sounds_played_on_the_client` — `starfall` и `lunar_flare` сыграны сервером и запущены звуковым движком клиента (`discharge` оригинал объявлял, но не использовал). Анимация Runic Stone: `tools/check_animation.py` — кадры через 10 тиков различаются ровно в одной ячейке иконки |
| M3 Данные Thaumaturge | ✅ | `tools/gen_data.py`: 28 записей, 26 инфузий, 8 scan-триггеров, аспекты 27 реликвий; сервер без ошибок кодеков. GameTests: `data_research_loaded`, `data_recipes_loaded`, `data_item_aspects`; **26 × `altar_infusion_<рецепт>`** — настоящий алтарь TT (матрица, пьедесталы, колонны, стабилизаторы), активация жезлом, эссенция тянется из банок, компоненты съедаются, результат на центральном пьедестале; **`research_progression_and_scans`** — с пустого состояния: записи открываются только после родителей (TT и свои), observation оплачивается пулом аспектов, theory — решёнными строками записки стола исследований, 8 скрытых записей открываются только после сканирования своих предметов, в итоге все 28 завершены. Клиент: `frcheck_thaumonomicon.png`, `frcheck_entry_*.png`, `frcheck_recipe_*.png`. Варп за крафт и условие False Justice сделаны в M4/M6 |
| M4 Общие системы | ✅ | GameTests: `systems_vis_add_and_consume`, `systems_curios_bauble`, `systems_damage_and_bosses`, `systems_crafting_warp`, `systems_valid_teleport`. Эффекты: burst и дуга — эффекты TT, молния — свой пакет `forgottenrelics:lightning` + рендер молний Botania (`frcheck_effects.png`); в `m7_obelisk_drainer_hold_and_interrupt` до клиента дошло 8 пакетов молний |
| M5 Баублы | ✅ | GameTests (формулы): `bauble_mining_speed`, `bauble_attribute_modifiers`, `bauble_ancient_aegis`, `bauble_ancient_aegis_redirect`, `bauble_dark_sun_ring`, `bauble_oblivion_amulet`, `bauble_superposition_split`, `bauble_deific_amulet`, `bauble_shiny_stone`, `bauble_nebulous_core_*`. Сценарии с реальным игроком: `m5_equip_by_right_click_and_slot_validity` (надевание ПКМ, все баублы приняты своими слотами Curios), `m5_aegis_owner_found_in_world` (владелец в мире найден: жертве 6 из 10; без него — 10), `m5_superposition_swap_across_dimensions` (кольцо само сработало на 600-м тике: игрок из overworld в Незер на место второго носителя), `m5_nebulous_core_dodge_teleport` (10..15 уклонений из 40), `m5_nebulous_core_discount_paid_by_a_wand` (атрибут TT `vis_discount` 0.35; 10 aer палочкой: 1100 → 750 сантивис). Подсказки: `frcheck_tooltips.png` |
| M6 Пассивные реликвии | ✅ | GameTests: `passive_false_justice_*`, `passive_chaos_core_*`, `passive_omega_core`, `passive_fate_tome`, `passive_xp_tome`, **`passive_fate_tome_catastrophe`** (все тома исчезают, зомби погибает от 40000, взрывы силы 16 у каждого живого и 100 у держателя — записаны и отменены). Сценарии: `m6_real_death_cancelled_by_relics` (смертельный удар по реальному игроку: False Justice, Omega Core, Fate Tome с палочкой), `m6_xp_tome_right_click_and_shift` (Shift+ПКМ включает и копит опыт, ПКМ — извлечение). Выключение `falseJusticeEnabled` проверено в M6 |
| M7 Утилитарные реликвии | ✅ | GameTests: `utility_*`. Сценарии: `m7_dimensional_mirror_return_from_nether` (Shift+ПКМ запоминает точку, удержание 80 тиков в Незере возвращает в overworld), `m7_tome_of_discord_block_and_blink` (ПКМ — к блоку под взглядом, Shift+ПКМ — ровно 16 блоков), `m7_obelisk_drainer_hold_and_interrupt` (рядом с обелиском TT: здоровье 8 → 16, еда растёт; в 17 блоках удержание прервано), `m7_paradox_left_click` (удар реального игрока). Частицы и молнии: `frcheck_effects.png` |
| M8 Сущности и боевые тома | ✅ | GameTests: `tome_*` (полёт снарядов на реальных тиках). Сценарии: `m8_primal_chaos_orbs_in_flight` (19 сфер Tome of Primal Chaos, 9..12 взрывов на попадании — записаны и отменены), `m8_apotheosis_weapons_in_flight` (14 оружий, попадания по цели в 14 блоках), `m8_tome_of_predestiny_grab_and_shift_throw` (захват повторами ПКМ, молния 16..40, Shift+ЛКМ бросает цель ~2.95 блока/тик), `m8_edict_holds_the_caster_still` (клиентская половина Эдикта гасит толчок 0.4 блока/тик полностью; после отпускания — движение). Снимки в полёте: `frcheck_scenario_chaos_orbs.png`, `frcheck_scenario_babylon_*.png` |
| M9 Клиент | ✅ | Рендеры и эффекты сверены с классами TC4 4.2.2.0 (`RenderCrimsonOrb`, `RenderPrimalOrb`, `RenderEldritchOrb`, `FXBurst`, `FXLightningBolt`): лучи (12, зерно — id сущности, накопленные повороты, цвета осколков), ядра из лент TT с теми же кадрами (`golem_orb_blue`, `flux_swirl`, `eldritch_orb`), молния type 0 (0.6/0.3/0.6), размер burst. Снимки: `frcheck_entities.png`, `frcheck_effects.png`, `frcheck_tooltips.png`, `frcheck_scenario_*.png`; GameTest `client_babylon_weapon_synced_state` |
| M10 Полировка | ✅ | Перевод: 85/85 ключей исследований на русском (исправлен непереведённый в оригинале заголовок Эфирного шахтёрского талисмана), ключи en/ru совпадают (357/357). Разметка TC4 → TT: `<LINE/>` → `<LINE>`, стих Oblivion Amulet — через `<BR>` вместо пробелов; снимки `frcheck_entry_oblivion*.png`. Баланс аспектов: реликвии 94..458 — в диапазоне топовых крафтов TT/Botania (терасталь 285..360, void robe 348, стабилизатор узла 392); переплавка всегда в минус — аспекты реликвии составляют 0.18..0.68 от эссенции и аспектов ингредиентов инфузии, правило ×2 оставлено. `check_assets.py`: 29/29; `check_lang.py`: все ключи есть. GameTests 80/80, `build` → `forgotten-relics-1.1.0-26.1.2.jar`. README.md. mods.toml: Curios `[15.0.0,)`. |

## Реликвии

| Предмет | Код | Данные (рецепт/исследование загружаются, M3) | Тест механики |
|---|---|---|---|
| superposition_ring | ✅ | ✅ | ✅ |
| mining_charm | ✅ | ✅ | ✅ |
| advanced_mining_charm | ✅ | ✅ | ✅ |
| dark_sun_ring | ✅ | ✅ | ✅ |
| ancient_aegis | ✅ | ✅ | ✅ |
| deific_amulet | ✅ | ✅ | ✅ |
| shiny_stone | ✅ | ✅ | ✅ |
| oblivion_amulet | ✅ | ✅ | ✅ |
| nebulous_core / nebulous_core_dormant | ✅ | ✅ | ✅ |
| chaos_core | ✅ | ✅ | ✅ |
| false_justice | ✅ | ✅ | ✅ |
| fate_tome | ✅ | ✅ | ✅ |
| omega_core | ✅ | – (креатив) | ✅ |
| xp_tome | ✅ | ✅ | ✅ |
| dimensional_mirror | ✅ | ✅ | ✅ |
| weather_stone | ✅ | ✅ | ✅ |
| teleportation_tome | ✅ | ✅ | ✅ |
| obelisk_drainer | ✅ | ✅ | ✅ |
| ghastly_skull | ✅ | – (креатив) | ✅ |
| paradox | ✅ | ✅ | ✅ |
| crimson_spell | ✅ | ✅ | ✅ |
| eldritch_spell | ✅ | ✅ | ✅ |
| chaos_tome | ✅ | ✅ | ✅ |
| missile_tome | ✅ | ✅ | ✅ |
| lunar_flares | ✅ | ✅ | ✅ |
| apotheosis | ✅ | ✅ | ✅ |
| soul_tome | ✅ | ✅ | ✅ |
| telekinesis_tome | ✅ | ✅ | ✅ |

## Открытые вопросы

- API Botania Revived неофициальный и молодой (2.0.x за неделю): все вызовы — только через `compat/BotaniaBridge`.

## Журнал

- 2026-11-16 — M0: скиллы перенесены из `forbidden-magic/.claude/skills` (локальные папки были пустыми);
  клонирован оригинал; скачаны Botania Revived 2.0.2 (26.1.2) и TT 1.1.0 для анализа; написаны план, карта, прогресс.
- 2026-11-16 — M1: gradle (MDG 2.0.148, NeoForge 26.1.2.122, JDK 25), mods.toml, главный класс, конфиг; сервер
  загружается. Curios поднят до 15.0.0 (Botania Revived отклонял beta.2).
- 2026-11-16 — M2: 29 предметов (`RelicItem`, варп снаряжения, редкость), Paradox как меч (`FRMaterials.PARADOX`),
  вкладка, 3 звука, 7 типов урона + теги, теги Curios. Генератор `tools/gen_assets.py` (текстуры, модели, lang
  en/ru, sounds.json, damage_type). Чекеры скопированы в `tools/` и научены находить хелпер `relic("id")`.
  Клиентский прогон `runClientCheck` (мир `run-check/saves/frcheck` скопирован из проверочного мира FM).
  Поведение предметов пока пустое — появится в M5–M8. В клиентском логе есть шум `Unknown loader:
  sophisticatedbackpacks/sophisticatedstorage` — это опциональные модели Botania Revived, не наши.
- 2026-11-16 — M3: `tools/gen_data.py` (исследования, инфузии, scan, аспекты предметов, тексты исследований en/ru →
  `tools/data/research_lang_*.json`, которые подмешивает `gen_assets.py`). Порядок запуска генераторов:
  `extract_original.py` → `gen_data.py` → `gen_assets.py`. Ингредиенты без аналога решены (см. план §3.3).
  `FRDataTests` (3 теста), Таумономикон в клиентском прогоне. Предупреждения Patchouli `Invalid icon item stack:
  botania:...` в клиентском логе — книга Botania Revived ссылается на свои отсутствующие предметы, не наше.
- 2026-11-16 — M4: `VisHelper`, `CuriosBridge`, `BotaniaBridge`, `RelicCurioItem`, `FRDamageSources`,
  `SuperpositionHandler`, `FRCraftingWarp`, теги `absolute` и `relic_bosses`. Тесты нашли: неверный id ванильного
  урона (`out_of_world`), кэш Curios в `isEquipped`. Мок `makeMockServerPlayerInLevel` непригоден — падает на
  синхронизации Curios; для ServerPlayer используется `FakePlayerFactory`.
- 2026-11-16 — M5: 10 баублов (`item/bauble/*`), `FRCombatEvents`, `FRMiningEvents`, `FRDataComponents`,
  `TooltipKeys`/`FRClient`. Логика раздела урона вынесена в `redirectToAegis` / `splitSuperposition` для тестов.
- 2026-11-16 — M6: `item/passive/*`, `XpHelper`, `TTSounds`, `FRConditions`, обработчики смерти в
  `FRCombatEvents`; `gen_data.py` добавляет `neoforge:conditions` к записи и рецепту False Justice.
- 2026-11-16 — M7: `item/utility/*`, `ObeliskBridge`, `FRFx`, звуки `jar/zap/altar_craft`, компоненты
  `mirror_target`, `obelisk_pos`. В тестах: FakePlayer игнорирует телепорт; `isRaining()` отстаёт от флага погоды.
- 2026-11-16 — M8: `entity/*`, `FREntities`, `item/tome/*` (`HeldTomeItem` для удержания), компонент
  `telekinesis`, звуки TT/Botania (`shock`, `egattack`, `ice`, `missile`, `babylon_*`), `RelicItem.tomeTooltip`.
  Тесты нашли: удар по площади считался от позиции до перемещения (теперь от точки контакта); сущности вне
  тикающих чанков тестового мира замирают.
- 2026-11-16 — M9: `client/renderer/OrbRenderer`, `BotaniaBridge.Client` (рендер Babylon на базе `BotaniaEntityRenderer`),
  `compat/TTEffects` (фасад `content.effect.Effects` TT: `burst`, `boltStrike`, `zapArc`) — свои S2C-пакеты не понадобились,
  TT рассылает эффекты сам. `BotaniaBridge.burst` удалён. Сцены `ClientCheck` 15–27. Найдено прогоном: TT рисует burst в 5 раз
  мельче TC4 (масштаб ×5); в тесте `tickCount` двигает цикл мира, а не `tick()`.
- 2026-11-16 — M10: правки генератора (`RU_FIXES`, `page_text`), README.md, сцена `ClientCheck` 28–33 (страницы
  Oblivion Amulet). Долгие (10+ мин) прогоны: Gradle висел в `listFileSystems` (file-system watching) до старта
  игры — выключено `org.gradle.vfs.watch=false`.
- 2026-11-17 — финал: все M доведены до проверки. Сверено с классами TC4 4.2.2.0 (CurseForge 2222517, только для
  чтения): молния type 0, FXBurst (масштаб ×15), лучи и ядра сфер — рендеры переписаны. Молнии — свой пакет
  `forgottenrelics:lightning` + `BoltRenderer` Botania (у TT нет аналога `FXLightningBolt`). GameTests 80: 26 инфузий на
  алтаре TT (`compat/TTTestBridge`, структура `empty_17x8x17` из `tools/gen_test_structures.py`), исследования и сканы,
  катастрофа Fate Tome. `client/ScenarioRunner`: 16 сценариев с реальным игроком. Найдено: TT подключает скидку
  `IVisDiscountGear` через атрибут, который Curios обновляет на тике игрока (проверка на FakePlayer невозможна).

## Что осталось

- **Не перенесено:** создание случайного узла ауры Orb of Primal Energy (у TT нет API создания узлов); косметика
  баублов и фантомные чернила Botania; шаг Vis Amulet при оплате виса (амулеты TT не хранят вис).
- **Как устроены проверки (ограничения стенда):**
  - Второй игрок в сценариях — FakePlayer, добавленный в список игроков и в мир (мок-игрок на embedded-канале ломает
    синхронизацию Curios). FakePlayer неуязвим и не телепортируется: долю урона владельца Аегиды проверяет GameTest
    `bauble_ancient_aegis_redirect`, а при обмене Суперпозиции наблюдается сторона реального игрока (вторая сторона —
    тот же вызов `teleportTo`).
  - Экран Curios мышью не кликается: проверяется допуск слота (`isItemValid`) — ровно то, что экран проверяет при
    клике, — и надевание ПКМ.
  - Мини-игра записок TT не решается: тест выставляет её результат (`ResearchNoteData.learnKey`), всё остальное
    (родители, пул аспектов, стадии, сканы) — настоящими вызовами TT.
  - Ввод в сценариях подаётся теми же серверными вызовами, что и обработчики пакетов (`ServerPlayerGameMode.useItem`,
    `releaseUsingItem`, `setShiftKeyDown`, `attack`, `swing`): окно прогона обычно без фокуса. Клиентская половина
    Эдикта проверяется на клиентском игроке.
  - Взрывы (катастрофа Fate Tome, сферы Primal Chaos) записываются через `ExplosionEvent.Start` и отменяются, чтобы
    не разрушать проверочный мир.
