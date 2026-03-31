EzRTP Forge module scaffold
==========================

This module is a minimal scaffold to begin adding a Forge-compatible integration for EzRTP.

Next steps for completion:

- Update `pom.xml` to include the correct Forge/MDK build plugin and loader coordinates for your target Minecraft/Forge version.
- Implement an actual `@Mod` entrypoint class annotated with `net.minecraftforge.fml.common.Mod` (requires adding Forge dependency in the POM).
- Replace scaffolded no-op implementations with real implementations that use Forge/Minecraft APIs.
- Optionally shade `ezrtp-common` into the final JAR or configure distribution as desired.

Build & package (experimental Maven + shade)
-----------------------------------------

This repository includes an experimental Maven-based packaging approach that uses the `maven-shade-plugin` to produce a single shaded JAR. The shaded artifact name is controlled by the module `finalName` (defaults to `EzRTP-forge-${project.version}`).

To produce the shaded JAR (uses the `forge.loader.version` property to filter `META-INF/mods.toml`):

```bash
# Build required reactor modules and package the forge module
mvn -q -DskipTests -pl ezrtp-forge -am package -Dforge.loader.version=YOUR_LOADER_VERSION

# The artifact will be at:
# ezrtp-forge/target/EzRTP-forge-<version>.jar
```

Notes and caveats
- The current module compiles against the platform SPI in `ezrtp-common` and includes a provided `paper-api` dependency so SPI types resolve during compilation; those Bukkit types are part of the shared SPI and are not required at runtime for the Forge client, but you may need to adjust/replace them if you remove the shared Bukkit-oriented SPI surface.
- The Maven + shade approach is a convenience for quick iteration and testing; for a full Forge development experience and proper Forge loader integration, using the official ForgeGradle MDK (Gradle) is recommended.
- Runtime testing requires a Forge development environment (IDE run config or a local Forge client). The packaged JAR produced here may need additional verification and dependency adjustments to run correctly under Forge.

Running tests
-------------

Compile tests and run unit tests for the module with:

```bash
mvn -q -pl ezrtp-forge test
```

If you want me to convert this module to a ForgeGradle MDK layout instead (recommended for production mods), tell me the target Minecraft/Forge versions and I will scaffold a Gradle build and run the MDK setup.
