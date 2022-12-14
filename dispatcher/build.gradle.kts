plugins {
    application
    kotlin("jvm") version "1.7.10"
}

group = "com.example"
version = "0.0.1"

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
}

application {
    mainClass.set("com.example.MainKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.insert-koin:koin-core:3.2.0")
    implementation("com.sksamuel.hoplite:hoplite-core:2.5.2")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.5.2")
    implementation("ch.qos.logback:logback-classic:1.2.9")
    implementation("org.apache.kafka:kafka-clients:3.2.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    implementation("io.confluent:kafka-json-serializer:7.2.1")
    implementation(projects.shared)

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.mockk:mockk:1.12.7")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}
