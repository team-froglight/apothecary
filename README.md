# Apothecary UI

A portable, stylable UI toolkit for Minecraft mods: flexbox and grid layout, typed code-only styling with
tokens and themes, transitions, keyframe animations, springs and SDF shaders. Run `/apothecary` in game to
open the showcase.

```java
import static me.methodial.apothecary.ui.UI.*;

col(s(x -> x.gap(6).pad(10)).card(),
    h2("Inventory"),
    muted("Drag items to reorder."),
    row(gap(4),
        button("check", "Save", this::save).primary(),
        button("Cancel", this::close).ghost()));
```

Show a tree with `UIScreen`: subclass it and override `build(UIRoot)`, or
`UIScreen.of(Component.literal("Hi"), root -> root.setContent(myNode))`.

## Modules

| Module | What it is |
| --- | --- |
| `core` | Plain Java, no Minecraft: nodes, layout, style sheets, themes, animation, widgets. Reusable on any loader or game version. |
| `neoforge` | The NeoForge 1.21.1 backend (rendering, text, input, `UIScreen`) and the showcase. |

## Using it

One jar carries both modules: `me.methodial.apothecary:apothecary-neoforge`, published to GitHub Packages
(`team-froglight/apothecary`). Ship it inside your mod with jar-in-jar:

```gradle
repositories {
    maven {
        url = 'https://maven.pkg.github.com/team-froglight/apothecary'
        credentials {   // GitHub Packages needs a token with read:packages, even for public packages
            username = providers.gradleProperty('gpr.user').orElse(providers.environmentVariable('GITHUB_ACTOR')).getOrElse('')
            password = providers.gradleProperty('gpr.key').orElse(providers.environmentVariable('GITHUB_TOKEN')).getOrElse('')
        }
    }
}
dependencies {
    implementation "me.methodial.apothecary:apothecary-neoforge:${apothecary_version}"
    jarJar("me.methodial.apothecary:apothecary-neoforge:${apothecary_version}") { transitive = false }
}
```

## Building

Java 21.

```
./gradlew build                 # → neoforge/build/libs/apothecary-neoforge-1.21.1-0.1.0.jar
./gradlew :neoforge:runClient   # the showcase: /apothecary
./gradlew publishToMavenLocal   # for a sibling mod checkout, without GitHub credentials
```

GitHub Actions builds every push to `main` and every pull request (`.github/workflows/ci.yml`) and attaches the jar
to the run. To release, bump `mod_version` in `gradle.properties` and run the **Publish** workflow; GitHub Packages
refuses to overwrite a version that already exists.
