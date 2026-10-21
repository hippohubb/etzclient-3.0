# MetzClient — Fabric 1.21.1

A compact, dark, blue-accented Minecraft client-side utility mod inspired by the supplied reference UI. The MetzClient logo is rendered in the top-right while in a world.

## Build

- Install JDK 21.
- Open this folder in IntelliJ IDEA (or another Gradle IDE) and allow Gradle to download dependencies.
- Run `gradle build` (or use the IDE's Gradle task `build`).
- The mod jar will be in `build/libs/`.
- Install Fabric Loader for Minecraft 1.21.1 and Fabric API, then put the built jar in `.minecraft/mods`.

This source archive does not include a prebuilt `.jar`; Gradle must resolve Fabric/Minecraft dependencies online to produce it.

## Controls

- **Right Shift** opens/closes the client menu.
- **C** is the zoom key while Zoom is enabled.

## Included functional toggles

Coordinates, FPS, direction, biome, in-game time, armor status, potion effects, item durability, keystrokes, CPS, custom crosshair, zoom, fullbright, auto sprint, auto tool selection, weather suppression, player radar, storage scan/ESP list, base-clue scan, nearby diamond/ancient-debris alert, anti-AFK jump, reach-distance HUD, ping, chunk coordinates, safe-walk hint, and held-item name.

### Notes

- Storage ESP and base-clue scanning inspect blocks in the **already loaded nearby area**; they do not magically reveal unloaded chunks or server-hidden data. The client menu is intended for single-player or servers where these utilities are allowed.
- Some utility features depend on Minecraft/Fabric APIs and server rules. Test on a clean Fabric 1.21.1 instance first.
