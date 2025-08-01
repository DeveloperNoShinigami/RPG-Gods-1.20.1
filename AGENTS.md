# Ultimate Modding Agent Guide

This document summarizes best practices for automating Minecraft mod development across Fabric, Forge, and common toolchains.

## Environment Setup
- Install a modern JDK (17 or higher) and ensure `JAVA_HOME` is configured.
- Use Gradle for builds. The provided `gradlew` wrapper handles versioning.
- For cross-platform mods, rely on Architectury or similar projects to share code.
- Single-platform projects, such as Forge-only mods, commonly keep everything in one module.

## Project Structure
- When targeting multiple loaders, keep platform-neutral logic in a `Common` module with separate `Fabric` and `Forge` modules.
- For Forge-only mods, a single module with both code and resources is perfectly acceptable.
- Resources belong under `src/main/resources` and Java sources under `src/main/java`.

## Coding Practices
- Follow Java best practices and the repository's style conventions.
- Standard package names typically use a reversed-domain prefix, but shorter names (as in this project's `rpggods` package and `RG*` classes) are common in Forge mods.
- Prefer composition over inheritance for complex systems.
- Add meaningful comments for non-trivial logic.
- Keep feature flags or version checks platform specific.

## Build and Testing
- Use `./gradlew build` to compile all modules.
- Execute `./gradlew test` to run unit tests.
- Generated artifacts are found under `build/libs` for each platform.

## Mod Development Methods
- Leverage Mixin to modify vanilla code when necessary.
- Register event listeners through Fabric API or Forge's event bus.
- Use datapacks or KubeJS scripts to configure behavior without rebuilding.
- Document commands, config options, and datapack formats in README files.

## Publishing
- Deploy releases to a Maven repository or hosting platform like Modrinth or CurseForge.
- Provide a changelog and update compatibility notes for each Minecraft version.

This overview provides a starting point for building or scripting an automated agent to assist with Minecraft modding tasks.
