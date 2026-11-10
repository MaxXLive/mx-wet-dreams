<p align="center">
  <img src="docs/icon.png" alt="MX Wet Dreams icon" width="128">
</p>

<h1 align="center">MX Wet Dreams</h1>

<p align="center">
  <i>This weather makes me sleepy.</i><br>
  A Paper plugin for Minecraft 26.3 that lets players sleep in a bed while it rains.
</p>

---

## Features

- Beds can be used while it rains during the day. In vanilla only nights and thunderstorms allow sleeping.
- When enough players sleep (gamerule `players_sleeping_percentage`), the rain stops and everyone wakes up, like after a night.
- By default only the rain is skipped and the time of day stays. With `skip-to-morning: true` the time jumps to the next morning as after a normal night.
- Monsters nearby still keep players awake, as in vanilla.
- Nights, thunderstorms and dimensions where beds never work (Nether, End) behave as in vanilla.

## Configuration

`plugins/MXWetDreams/config.yml`, read on server start:

```yaml
# false: only the rain is skipped, the time of day stays as it is.
# true:  like a normal night, the time jumps to the next morning.
skip-to-morning: false
```

The rain only stops after sleeping if the gamerule `advance_weather` is on, as in vanilla.

## How it works

Rain darkens the sky, but not enough for vanilla to allow sleeping. The plugin

1. lets the player into the bed when the only reason for refusing is the light and it is raining,
2. keeps the player in bed: vanilla wakes sleeping players up every tick while it is too bright. Only this wake-up is cancelled; leaving the bed with the button, waking up after the night skip or after damage still works,
3. sets the time skip to 0 when the sleep finishes during the day (unless `skip-to-morning` is on).

## Requirements

| Software | Version |
|---|---|
| [Paper](https://papermc.io/downloads/paper) | 26.3 |

## Building

Building needs JDK 25.

```sh
./gradlew build
```

The jar is written to `build/libs/`. Every push to `master` builds a new GitHub release (`v<mc>-patch-<n>`).

## License

This plugin is licensed under the [MIT License](LICENSE).
