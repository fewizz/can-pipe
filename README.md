# can-pipe
Minecraft Mod for loading [Canvas](https://github.com/vram-guild/canvas) shader pipelines

## What doesn't work / not implemented?
* Some Material properties
* Material map's `variants` properties
* `fabulousTargets` - array of render targets
* `material.glsl` uniforms, and some others: `frx_cleanViewProjectionMatrix`

, and probably many other things.

Performance is not on par with `Canvas`.

## Shaderpacks that are known to work
* [Forget-Me-Not](https://modrinth.com/shader/forgetmenot) and
[Aerie](https://modrinth.com/shader/aerie-shaders) by [ambrosia13](https://github.com/ambrosia13)
* [LumiLights](https://github.com/spiralhalo/LumiLights/releases),
[ecos](https://modrinth.com/shader/ecos) and
[LumiPBRExt](https://github.com/spiralhalo/LumiPBRExt) (limited compat) by [spiralhalo](https://github.com/spiralhalo)
* [Brunnera](https://github.com/supsm/Brunnera-Shaders) by [supsm](https://github.com/supsm)

## How to apply shader pipeline?
1. Install this mod
2. Put preferred shaderpacks into `resourcepacks` directory and activate them
3. In video settings, click on `Pipeline: ...` to switch between available shader pipelines

## Gradle run tasks
* `fabric:runClient`
* `fabric:runClientCinnabar`
* `fabric:runClientCinnabarValidated`
* `neoforge:runClient` (if subproject is included)

## Discord
https://discord.gg/jW2UvpvFKh