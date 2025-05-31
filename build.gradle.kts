// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val kotlinVersion="2.1.10"
    val androidVersion = "8.9.2"

    id("com.android.application") version androidVersion apply false
    id("com.android.library") version androidVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion apply false
    id("com.google.devtools.ksp") version "2.1.10-1.0.29" apply false
    id("androidx.room") version "2.7.1" apply false

//    id("io.ktor.plugin") version "3.0.3" apply false

}
