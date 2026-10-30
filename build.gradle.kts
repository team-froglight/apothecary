// Shared configuration for every module. The core module is plain Java (no Minecraft), so it can be
// reused by other loaders or game versions; the neoforge module is the 1.21.1 backend + showcase.
tasks.wrapper {
    distributionType = Wrapper.DistributionType.BIN
}

subprojects {
    apply(plugin = "java")

    group = providers.gradleProperty("mod_group_id").get()
    version = providers.gradleProperty("mod_version").get()

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(21)
    }

    repositories { mavenCentral() }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 21
    }
}
