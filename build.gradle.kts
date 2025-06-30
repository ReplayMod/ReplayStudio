import gg.essential.gradle.util.*
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute

plugins {
    `java-library`
    `maven-publish`
    id("gg.essential.defaults.java")
    id("xyz.wagyourtail.jvmdowngrader") version "0.7.2"
}

group = "com.github.ReplayMod"
description = "ReplayStudio"
version = "master-SNAPSHOT"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.viaversion.com")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://jitpack.io")
}

val relocated = registerRelocationAttribute("viaversion-relocated") {
    relocate("io.netty", "com.github.steveice10.netty")
    relocate("us.myles.ViaVersion.api", "com.replaymod.replaystudio.lib.viaversion.legacyapi")
    relocate("com.viaversion.viaversion", "com.replaymod.replaystudio.lib.viaversion")
    relocate("com.google.common", "com.replaymod.replaystudio.lib.guava")
}

val viaVersion by configurations.creating {
    attributes { attribute(relocated, true) }
    jvmdg.dg(this)
}

dependencies {
    viaVersion("com.github.replaymod.viaversion:viaversion-common:e90baa6b4")

    viaVersion("com.google.guava:guava:17.0")
    implementation("com.google.code.gson:gson:2.3.1")

    implementation("it.unimi.dsi:fastutil:8.3.1") // this is the version MC ships with 1.14.4, upgrade with care

    implementation("com.github.viaversion:opennbt:0a02214") // 2.0-SNAPSHOT (ViaVersion Edition)
    viaVersion("com.github.steveice10:packetlib:614d56cdc0" /* 1.3 */) {
        exclude("org.slf4j")
    }

    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("org.apache.commons:commons-collections4:4.0")
    implementation("commons-cli:commons-cli:1.2")

    implementation(prebundle(viaVersion))

    compileOnly(annotationProcessor("org.projectlombok:lombok:1.18.32")!!)
    testImplementation("junit:junit:4.11")
    testImplementation("com.google.guava:guava-testlib:18.0")
    testImplementation("pl.pragmatists:JUnitParams:1.0.4")
}

tasks.jar {
    from(viaVersion.files.map { zipTree(it) })
}

jvmdg.shadePath.set { "com/replaymod/replaystudio/lib" }

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

defaultTasks.add("publishToMavenLocal")
