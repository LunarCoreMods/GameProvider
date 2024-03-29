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

    shade(group = "net.fabricmc", name = "fabric-loader", version = loaderVersion)

    shade(group = "net.fabricmc", name = "access-widener", version = "2.1.0")
    shade(group = "net.fabricmc", name = "sponge-mixin", version = mixinVersion) {
        exclude(module = "launchwrapper")
        exclude(module = "guava")
        exclude(module = "gson")
    }
    shade(group = "com.google.guava", name = "guava", version = "33.1.0-jre")
    shade(group = "com.google.code.gson", name = "gson", version = "2.10.1")
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
                    "Multi-Release" to true
                )
            )
        }
        dependsOn(shadowJar)
    }
    named<ShadowJar>("shadowJar") {
        configurations = listOf(shade)
    }
}