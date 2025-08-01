# Ultimate Modding Agent Guide

This document summarizes best practices for automating Minecraft mod development across Fabric, Forge, and common toolchains.

## Environment Setup
- Install a modern JDK (17 or higher) and ensure `JAVA_HOME` is configured.
- Use Gradle for builds. The provided `gradlew` wrapper handles versioning.
- For cross-platform mods, rely on Architectury or similar projects to share code.

## Project Structure
- Keep platform neutral logic in the `Common` module.
- Put Fabric specific code inside the `Fabric` module and Forge code inside the `Forge` module.
- Resources belong under `src/main/resources` and Java sources under `src/main/java`.

## Coding Practices
- Follow Java best practices and the repository's style conventions.
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
