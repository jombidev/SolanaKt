import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java")
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
}

val ktorVersion: String by project

group = "dev.jombi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("ch.qos.logback:logback-classic:1.4.8")

    // *** kotlinx ***
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")

    // *** KTOR ***
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("com.google.protobuf:protobuf-javalite:3.23.2")
    implementation("org.bitcoinj:bitcoinj-core:0.16.2")
    implementation("net.i2p.crypto:eddsa:0.3.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        jvmToolchain(8)
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
    }
}