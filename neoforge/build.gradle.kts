plugins {
    idea
    `maven-publish`
    alias(libs.plugins.moddev)
}

fun prop(name: String): String = providers.gradleProperty(name).get()

base {
    archivesName = "${prop("mod_id")}-neoforge-${prop("minecraft_version")}"
}

evaluationDependsOn(":core")
val core = project(":core")
val coreMain = core.extensions.getByType<SourceSetContainer>().named("main")

neoForge {
    version = prop("neo_version")

    parchment {
        minecraftVersion = prop("parchment_minecraft_version")
        mappingsVersion = prop("parchment_mappings_version")
    }

    runs {
        register("client") {
            client()
            programArguments.addAll("--width", "1280", "--height", "720")
            // ./gradlew :neoforge:runClient -PquickPlayWorld=<save name> jumps straight into a world.
            providers.gradleProperty("quickPlayWorld").orNull?.let {
                programArguments.addAll("--quickPlaySingleplayer", it)
            }
        }
        configureEach {
            logLevel = org.slf4j.event.Level.INFO
            gameDirectory = rootProject.file("run")
            // JEC's jec_*_hotswap and jec_*_trace attach to the running game.
            jvmArguments.addAll("-Djdk.attach.allowAttachSelf=true", "-XX:+EnableDynamicAgentLoading")
        }
    }

    mods {
        register(prop("mod_id")) {
            // core + backend form one mod module at runtime
            sourceSet(sourceSets.main.get())
            sourceSet(coreMain.get())
        }
    }
}

repositories {
    // JEC (development only), see the runtimeOnly dependency below.
    maven("https://gitlab.com/api/v4/projects/87129086/packages/maven") { name = "JEC" }
}

dependencies {
    implementation(core)
    // Core's one library, shipped inside the mod jar so the mod stays a single drop-in file.
    implementation(libs.jsvg)
    jarJar(libs.jsvg)
    "additionalRuntimeClasspath"(libs.jsvg)

    // JEC: an MCP for the running game (inspection, rendering, hot-swap) in runClient. runtimeOnly, never bundled.
    runtimeOnly(libs.jec.neoforge)
}

// Ship core's classes inside the mod jar so the mod is a single drop-in file.
tasks.jar {
    from(coreMain.map { it.output })
}

// Expands src/main/templates (neoforge.mods.toml) with the values from gradle.properties.
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = listOf(
        "minecraft_version", "minecraft_version_range", "neo_version", "neo_version_range",
        "loader_version_range", "mod_id", "mod_name", "mod_license", "mod_authors", "mod_description",
    ).associateWith { prop(it) } + ("mod_version" to project.version.toString())
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main {
    resources.srcDir(generateModMetadata)
}
neoForge.ideSyncTask(generateModMetadata)

// Published for mods that build on Apothecary: one jar (core + backend, JSVG inside it jar-in-jar), no further dependencies.
// Depend on me.methodial.apothecary:apothecary-neoforge and jarJar it. `./gradlew publishToMavenLocal`
// for a local checkout; the Publish workflow runs `./gradlew publish` to GitHub Packages.
publishing {
    publications {
        register<MavenPublication>("mod") {
            artifactId = "apothecary-neoforge"
            artifact(tasks.jar)
        }
    }
    repositories {
        maven("https://maven.pkg.github.com/team-froglight/apothecary") {
            name = "GitHubPackages"
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).getOrElse("")
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).getOrElse("")
            }
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
