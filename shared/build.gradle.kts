val kmongo_version: String by project
val kotlinx_version: String by project

plugins {
    `java-library`
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_version")
    api("org.litote.kmongo:kmongo-coroutine-serialization:$kmongo_version")
}