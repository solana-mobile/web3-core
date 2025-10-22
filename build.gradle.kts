import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.withType<KotlinNpmInstallTask>().configureEach {
    doFirst {
        val pkgJson = file("${rootProject.buildDir}/js/package.json")
        if (pkgJson.exists()) {
            val text = pkgJson.readText()
            val safe = rootProject.name.lowercase().takeWhile { !it.isWhitespace() }
            pkgJson.writeText(
                text.replaceFirst(
                    Regex("\"name\"\\s*:\\s*\"[^\"]+\""),
                    "\"name\": \"$safe\""
                )
            )
        }
    }
}

//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}
