import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = properties["group"]!!
version = properties["version"]!!

lateinit var shade: Configuration

configurations {
    register("implementationAppending") {
        shade = this
        configurations.implementation.get().extendsFrom(this)
    }
}

repositories {
    mavenCentral()
    maven(url = "https://maven.fabricmc.net")
}

dependencies {
    val loaderVersion: String by project
    val mixinVersion: String by project
    val mixinExtrasVersion: String by project

    implementation(group = "net.fabricmc", name = "fabric-loader", version = loaderVersion)

    implementation(group = "net.fabricmc", name = "access-widener", version = "2.1.0")
    implementation(group = "net.fabricmc", name = "sponge-mixin", version = mixinVersion) {
        exclude(module = "launchwrapper")
        exclude(module = "guava")
        exclude(module = "gson")
    }
    implementation(group = "com.google.guava", name = "guava", version = "33.1.0-jre")
    implementation(group = "com.google.code.gson", name = "gson", version = "2.10.1")

    listOf("asm", "asm-analysis", "asm-commons", "asm-tree", "asm-util").forEach {
        implementation(group = "org.ow2.asm", name = it, version = "9.6")
    }

    implementation(group = "io.github.llamalad7", name = "mixinextras-fabric", version = mixinExtrasVersion)
}

java {
    withSourcesJar()
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.getName() },
                    "Specification-Version" to 8.0,
                    "Multi-Release" to true,
                    "Main-Class" to "${project.group}.provider.Main"
                )
            )
        }
        val libraryDir = File("${layout.buildDirectory.get().asFile.path}/libs/lib")
        if (!libraryDir.exists()) {
            libraryDir.mkdirs()
        }
        configurations.runtimeClasspath.get()
            .map { Pair(it, File(libraryDir, it.name)) }
            .forEach { (file, dest) -> file.copyTo(dest, true) }

        println(configurations.runtimeClasspath.get().joinToString(";") { "lib/" + it.name })
        dependsOn(shadowJar)
    }
    named<ShadowJar>("shadowJar") {
        configurations = listOf(shade)
    }
}