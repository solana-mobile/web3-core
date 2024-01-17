plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.21"
    id("com.vanniktech.maven.publish")
}

val artifactIdPrefix: String by project
val moduleArtifactId = "$artifactIdPrefix-solana"

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
                api(project(mapOf("path" to ":core")))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("io.github.funkatronics:multimult:0.2.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("com.diglol.crypto:pkc:0.1.5")
                implementation("com.solanamobile:rpc-core:0.2.4")
            }
        }
    }
}

mavenPublishing {
    coordinates(group as String, moduleArtifactId, version as String)
}
