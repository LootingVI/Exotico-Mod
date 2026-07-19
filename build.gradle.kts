plugins {
    // Applies fabric-loom (or its equivalent) matching the active version automatically.
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

// DO NOT set group = ... here, Stonecutter manages the subproject path.
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

repositories {
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // loomx.applyMojangMappings() does NOT auto-pick Yarn when it's available - for any
    // "obfuscated" version (isUnobfuscated == false, i.e. < 26) it always applies official
    // mappings. 1.21.11 is the only supported version that still has real Yarn mappings
    // published, so it's wired explicitly here. 26.1+ ships pre-named jars (isUnobfuscated ==
    // true) where this call is a no-op by design.
    if (sc.current.version == "1.21.11") {
        mappings("net.fabricmc:yarn:${sc.current.version}+build.6:v2")
    } else {
        loomx.applyMojangMappings()
    }

    val fabricApiVersion: String = sc.properties["deps.fabric_api"]
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("exotico") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runConfigs.all {
        runDirectory = rootProject.file("run")
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

// 26.1+ ships Java 25 class files (version 69); everything below that is built against 21.
val targetJavaVersion = if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion.majorVersion.toInt())
}

java {
    withSourcesJar()
    targetCompatibility = targetJavaVersion
    sourceCompatibility = targetJavaVersion

    toolchain {
        languageVersion = JavaLanguageVersion.of(targetJavaVersion.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft_version" to sc.current.version,
            "loader_version" to project.property("deps.fabric_loader").toString()
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }

    val archivesNameForJar = base.archivesName.get()
    jar {
        from("LICENSE") {
            rename { "${it}_$archivesNameForJar" }
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/<minecraft version>/"

        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${sc.current.version}"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = property("mod.group") as String
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        // Add repositories to publish to here.
    }
}
