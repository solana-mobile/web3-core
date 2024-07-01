plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.publish)
}

val artifactIdPrefix: String by project
val moduleArtifactId = "$artifactIdPrefix-solana"
val buildDir = layout.buildDirectory.asFile.get()
val generatedDir = "${buildDir}/generated/src/commonTest/kotlin"

kotlin {
    jvmToolchain(11)
    jvm {
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
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.borsh)
                implementation(libs.multimult)
                implementation(libs.salkt)
            }
        }
        val commonTest by getting {
            kotlin.srcDir(File(generatedDir))
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.crypto)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.rpc.core)
                implementation(libs.rpc.ktordriver)
                implementation(libs.rpc.solana)
            }
        }
    }
}

mavenPublishing {
    coordinates(group as String, moduleArtifactId, version as String)
}

afterEvaluate {
    val defaultRpcUrl = properties["testing.rpc.defaultUrl"]
    var rpcUrl = properties["rpcUrl"] ?: defaultRpcUrl

    val useLocalValidator = project.properties["localValidator"] == "true"
    val localRpcUrl = project.properties["testing.rpc.localUrl"]
    if (useLocalValidator && localRpcUrl != null) rpcUrl = localRpcUrl

    val dir = "${generatedDir}/com/solana/config"
    mkdir(dir)
    File(dir, "TestConfig.kt").writeText(
        """
            package com.solana.config
            
            internal object TestConfig {
                const val RPC_URL = "$rpcUrl" 
            }
        """.trimIndent()
    )
}
