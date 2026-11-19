# ADHD

Турнирная мини-игра для Paper с поддержкой нескольких игровых режимов.

## Создание собственного режима

1. Придумайте ключ режима (далее — **`<Mode>`**, например `Spleef`).

2. **Конфиг режима.** Создайте `src/main/resources/config_<Mode>.yml` по образцу существующих. Для одиночного режима укажите `single: true`.

3. **Метаданные режима (если нужны).** Если режиму нужны параметры из секции `meta`, создайте:
   - `game/mode/meta/concrete/<Mode>ModeMeta.kt`, наследник `ModeMeta`;
   - `game/mode/loader/concrete/<Mode>ModeMetaLoader.kt`, реализацию `ModeMetaLoader`.

   Пример — `PVPModeMeta` / `PVPModeMetaLoader`.

4. **Регистрация режима.** В `config/ADHDConfig.kt` добавьте режим в `registeredModes`, указав загрузчик или `null`:

   ```kotlin
   Pair("<Mode>", <Mode>ModeMetaLoader()),
   ```

5. **Карты.** В `src/main/resources/config.yml` опишите карты для режима (или используйте существующие) и укажите их ID в `default.maps`. Если карте нужны свои данные:
   - создайте `config/map/meta/concrete/<Name>MapMeta.kt`, наследник `MapMeta`, и `config/map/loader/concrete/<Name>MapMetaLoader.kt`, реализацию `MapMetaLoader`;
   - зарегистрируйте загрузчик в `ADHDConfig.registerMapMetaLoaders()` под ключом, который будет использоваться в `maps.<id>.meta`;
   - в игре получайте данные так: `arena.metas["<ключ>"] as? <Name>MapMeta`.

6. **Логика игры.** Создайте `game/concrete/<Mode>Game.kt`, наследник `Game`, и реализуйте методы:

   | Метод                                         | Назначение                                                                     |
   |-----------------------------------------------|--------------------------------------------------------------------------------|
   | `start(worldName, arena, members, modeMeta)`  | Телепортировать игроков на `arena.spawnPoints`, выдать предметы, перейти в `GameState.RUN` |
   | `update()`                                    | Вызывается каждые 2 тика, пока игра в состоянии `RUN`                          |
   | `getGameState()`                              | Текущее состояние: `START` / `RUN` / `FINISH`                                  |
   | `finish()`                                    | Принудительно завершить игру (таймер, выход игрока)                            |
   | `summarize()`                                 | `Map<UUID, Double>` — очки за раунд; все игроки из ключей карты считаются победителями раунда |

   Пример — `PVPGame`.

7. **Создание игры в турнире.** В `tournament/Tournament.kt`, в методе `prepare()`, добавьте режим в нужный блок `when`:
   - дуэльный режим — в `when (modeName) { ... }`;
   - одиночный режим — в `when (name) { ... }` рядом с `RPS`, `Casino`, `Memory`.

8. **Слушатели (если нужны).** Поместите их в `listener/mode/<mode>/` и зарегистрируйте в `ADHDPlugin.onEnable()`. Чтобы получить игру игрока, используйте `TournamentManager.getGame(player)` и проверку типа (`as? <Mode>Game`).

> Существующие конфиги в `plugins/ADHD/` при обновлении jar не перезаписываются. Новый `config_<Mode>.yml` появится на сервере автоматически при первом запуске, а карты для него нужно добавить в `config.yml` на сервере вручную.
