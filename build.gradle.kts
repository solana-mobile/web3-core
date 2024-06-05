//buildscript {
//    dependencies {
//        classpath("com.android.tools.build:gradle:8.1.0")
//    }
//}
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}
