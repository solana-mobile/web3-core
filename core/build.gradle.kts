plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.21"
    id("com.vanniktech.maven.publish")
}

val artifactIdPrefix: String by project
val moduleArtifactId = "$artifactIdPrefix-core"

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64()
    ).forEach {
        it.binaries.framework {
            baseName = moduleArtifactId
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

mavenPublishing {
    coordinates(group as String, moduleArtifactId, version as String)
}
