val group: String by project
val version: String by project
val repo: String by project

project.group = group
project.version = version

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.shadow)

    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://maven.joutak.ru/releases")
}

dependencies {
    compileOnly(libs.kotlin)
    compileOnly(libs.paper)

    implementation("ru.joutak:minigamesapi:3.9.2")

    paperweight.paperDevBundle("26.1.2.build.+")
}

kotlin {
    jvmToolchain(
        libs.versions.jdk
            .get()
            .toInt(),
    )

    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}

tasks.processResources {
    val paperVersion = libs.versions.paper.get()

    val minecraftVersion =
        if (".build." in paperVersion) {
            paperVersion.substringBefore(".build")
        } else {
            paperVersion.substringBefore("-")
        }

    val commitHash = project.findProperty("commitHash") as String?

    val website =
        if (repo.isBlank()) {
            "https://joutak.ru"
        } else {
            if (commitHash.isNullOrBlank()) repo else "$repo/tree/$commitHash"
        }

    val props =
        mapOf(
            "NAME" to project.name,
            "VERSION" to project.version,
            "MINECRAFT_VERSION" to minecraftVersion,
            "KOTLIN_VERSION" to libs.versions.kotlin.get(),
            "WEBSITE" to website,
        )

    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveFileName.set("${project.name}-${project.version}.jar")

    if (System.getenv("TEST_PLUGIN_BUILD") != null) {
        val serverPath = System.getenv("SERVER_PATH")
        if (serverPath != null) {
            destinationDirectory.set(file("$serverPath\\plugins"))
        } else {
            logger.warn("SERVER_PATH property is not set!")
        }
    }
}
