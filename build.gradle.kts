plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish) apply false
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.solanamobile:web3-core")).using(project(":core"))
        substitute(module("com.solanamobile:web3-solana")).using(project(":solana"))
    }
}

//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}
