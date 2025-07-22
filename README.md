# can-pipe
Minecraft Mod for loading [Canvas](https://github.com/vram-guild/canvas) shader pipelines using minimal amount of mixins

## 

## What doesn't work / not implemented?
* some Material properties
* Material map's `variants` properties
* `fabulousTargets` - array of render targets
* `material.glsl` uniforms, and some others: `frx_entityView`, `frx_cleanViewProjectionMatrix`, `frx_vanillaClearColor`

, and probably many other things.

Performance is not on par with `Canvas`.

## Shader pipelines that are known to work
[Forget-Me-Not](https://modrinth.com/shader/forgetmenot) and
[Aerie](https://modrinth.com/shader/aerie-shaders) by [ambrosia13](https://github.com/ambrosia13),
[LumiLights](https://github.com/spiralhalo/LumiLights/releases) and
[ecos](https://modrinth.com/shader/ecos) by [spiralhalo](https://github.com/spiralhalo),
[Brunnera](https://github.com/supsm/Brunnera-Shaders) by [supsm](https://github.com/supsm)

## How to apply shader pipeline?
1. Install this mod
2. Put preferred shaderpacks into `resourcepacks` directory and activate them
3. In video settings, click on `Pipeline: ...` to switch between available pipelines