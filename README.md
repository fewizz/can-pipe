# can-pipe
Minecraft Mod for loading [Canvas](https://github.com/vram-guild/canvas) shader pipelines using minimal amount of mixins

## 

## What doesn't work / not implemented?
* some Material properties
* Material map's `variants` properties
* `fabulousTargets` - array of render targets
* `skyShadows` properties: `allowEntities`, `allowParticles`, `supportForwardRender`
* `material.glsl` uniforms, and some others: `frx_entityView`, `frx_cleanViewProjectionMatrix`, `frx_vanillaClearColor`

, and probably many other things.

Performance is obviously not on par with `Canvas`.

## Cursed bits (only small fraction of them)
* Since MC 1.21.5, available texture formats are hardcoded in `TextureFormat` enum,
so we extend it and [provide](https://github.com/fewizz/can-pipe/blob/1e5dabe53713c440c02294b7aa4b66a1d273b1cb/common/src/main/java/fewizz/canpipe/mixin/m01_texture_formats/Plugin.java#L125) additional, most used formats
* Doesn't provide `FRAPI` renderer, parasitizes on `Indigo` instead

## Shader pipelines that are known to work
All known to me:
[Forget-Me-Not](https://modrinth.com/shader/forgetmenot) and
[Aerie](https://modrinth.com/shader/aerie-shaders) by [ambrosia13](https://github.com/ambrosia13),
[LumiLights](https://github.com/spiralhalo/LumiLights/releases) and
[ecos](https://modrinth.com/shader/ecos) by [spiralhalo](https://github.com/spiralhalo),
[Brunnera](https://github.com/supsm/Brunnera-Shaders) by [supsm](https://github.com/supsm)

## How to apply shader pipeline?
1. Install this mod
2. Put preferred shaderpacks into `resourcepacks` directory and activate them
3. In video settings, click on `Pipeline: ...` to switch between available pipelines

## Note on FREX
Can-pipe doesn't use [FREX](https://github.com/vram-guild/frex.git), but might in the future.
Currently, can-pipe includes some FREX shader files, with slight modifications.