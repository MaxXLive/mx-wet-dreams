plugins {
    java
}

val minecraftVersion: String by project
val paperBuild: String by project

group = "de.ermackov"
// CI passes -PreleaseVersion=<mc>-patch-<n>; local builds are marked as dev builds
version = providers.gradleProperty("releaseVersion").getOrElse("$minecraftVersion-dev")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion.build.$paperBuild-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }
    processResources {
        val props = mapOf("version" to project.version, "minecraftVersion" to minecraftVersion)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
    jar {
        from("LICENSE") {
            rename { "${it}_MXWetDreams" }
        }
    }
}
