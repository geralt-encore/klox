import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.3.61"
}

dependencies {
    implementation(kotlin("stdlib"))
}