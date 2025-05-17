# can-pipe
Minecraft Mod for loading [Canvas](https://github.com/vram-guild/canvas) shader pipelines using minimal amount of mixins

## 

## What doesn't work/implemented?
* Material properties (like `disableAo`, `disableDiffuse`, etc)
* Material map's `variants` properties
* `fabulousTargets` - array of render targets
* Some `skyShadows` properties: `allowEntities`, `allowParticles`, `supportForwardRender`
* `material.glsl` uniforms, and some others (`frx_entityView`, `frx_cleanViewProjectionMatrix`, `frx_vanillaClearColor`)

, and probably many other things.

Performance is obviously not up to par with `Canvas`.

## shaderpacks that are known to work
All known to me:
[Forget-Me-Not](https://modrinth.com/shader/forgetmenot),
[LumiLights](https://github.com/spiralhalo/LumiLights/releases),
[Aerie](https://modrinth.com/shader/aerie-shaders),
[ecos](https://modrinth.com/shader/ecos),
[Brunnera](https://github.com/supsm/Brunnera-Shaders)

## How to apply shader pipeline?
1. Install this mod
2. Put preferred shaderpacks into `resourcepacks` directory and activate them
3. In video settings, click on `Pipeline: ...` to switch between available pipelines

## Note on FREX
Can-pipe doesn't use [FREX](https://github.com/vram-guild/frex.git), but might in the future.
Currently, can-pipe includes some FREX shader files, with slight modifications.