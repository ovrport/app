import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.21"
    application
    id("com.gradleup.shadow") version "9.2.2"
}

group = "moe.crx"
version = "1.2.1"

dependencies {
    implementation(libs.android.tools.build)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(fileTree("libs"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "moe.crx.overport.cli.CliMainKt"
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
}