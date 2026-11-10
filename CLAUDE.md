# MX Wet Dreams – notes for Claude

Paper plugin for Minecraft 26.3 (Java 25). Package `de.ermackov.wetdreams`.
Lets players sleep in a bed while it rains during the day.

## Build and test

- Build: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew build` → `build/libs/MXWetDreams-26.3-dev.jar`
  CI passes `-PreleaseVersion=26.3-patch-N`.
- Integration tests: `python3 test/run_tests.py` — requires the test server in
  `../playtime/run/` to be running (Paper 26.3, MinecraftMCP plugin enabled).
  The test scripts are gitignored (hardcoded server paths and token).
  To run: start the server, then run the script. All 15 assertions should pass.

## How it works

Vanilla only allows sleeping when `level.isDarkOutside()` is true (sky darkness ≥ 4).
Rain darkens the sky slightly but not enough; a thunderstorm does.
Three Paper events are intercepted:

1. **`PlayerBedEnterEvent`** — fired after `ServerPlayer.startSleepInBed` calls
   `CraftEventFactory.callPlayerBedEnterEvent`. When the result is `NOT_POSSIBLE_NOW`
   with `canSleep == BedRuleResult.TOO_MUCH_LIGHT` and it is raining (not thundering),
   `setUseBed(ALLOW)` overrides the refusal. Monster check is done here because vanilla
   only checks monsters after the light check.

2. **`PlayerBedLeaveEvent`** — `Player.tick` calls `stopSleepInBed` every tick when
   `canSleep` returns false. This event is cancelled only when it is still raining and
   the call comes from `Player.tick` (detected via `StackWalker`). All other wake-up
   paths (button, damage, `wakeUpAllPlayers`) go through unchanged.

3. **`TimeSkipEvent` (extends `ClockTimeSkipEvent`)** — when enough players slept,
   the server fires this with `NIGHT_SKIP`. During rain at non-night hours the skip
   amount is set to 0 (rain stops, time stays). Thunderstorms and vanilla nights pass
   through unchanged. Skipped entirely when `skip-to-morning: true` in config.

## Important details

- The `StackWalker` check in `onBedLeave` looks for `net.minecraft.world.entity.player.Player#tick`
  with Mojang mappings (Paper ships with them). If a Paper update renames or inlines
  that method, the walker finds nothing and the cancel stops working — players get
  kicked out of bed during rain by the tick. Not a crash, just a regression.
- `world.isDayTime()` delegates to `level.isBrightOutside()` which is `skyDarken < 4`.
  `isNight()` in the plugin uses `world.getTime()` (ticks 12542–23460) to avoid
  misidentifying a thunderstorm-at-noon as "night" via the sky darkness path.
- `hasStorm()` returns true for both rain and thunderstorms. `isThundering()` is
  checked wherever the plugin should stay out of the way for thunderstorms.

## Stack of Paper APIs used

| API | Why |
|---|---|
| `PlayerBedEnterEvent.enterAction().canSleep()` | distinguish light refusal from other refusals |
| `BedRuleResult.TOO_MUCH_LIGHT` | Paper constant for the specific refusal reason |
| `PlayerBedFailEnterEvent` | replace "can only sleep at night" with "monsters nearby" message |
| `TimeSkipEvent` | cancel the morning skip during rain-only sleep |
| `World.isDayTime()` | delegates to `isBrightOutside()` |
| `World.hasStorm()` / `isThundering()` | rain vs thunderstorm distinction |

## Rules

- Commits use Conventional Commits (`feat:`, `fix:` …).
  The release changelog is generated from them, see `.github/scripts/release-info.sh`.
- No NMS / paperweight-userdev: the plugin uses only the public Paper API.
  To inspect internals for debugging, decompile from the dev bundle of another project
  (`mx-crafting-chest/server-paper/.gradle/caches/paperweight/taskCache/mappedServerJar.jar`)
  with Vineflower. Do this in the scratchpad, not in the repo.
