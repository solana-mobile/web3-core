pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

gradle.beforeProject {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.solanamobile:web3-solana"))
                .using(project(":solana"))
        }
    }
}

rootProject.name = "Web3 Core"
include(":core")
include(":solana")
include(":mwa-signer")
