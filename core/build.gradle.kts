plugins {
    `java-library`
}

base {
    archivesName = "apothecary-core"
}

java {
    withSourcesJar()
}

dependencies {
    // JSVG renders the pixel icons' SVG drawings; the mod jar carries it jar-in-jar.
    api(libs.jsvg)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}
