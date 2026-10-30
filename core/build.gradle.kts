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
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}
