plugins {
    kotlin("jvm") version "2.2.20"
    application
}

application {
    mainClass.set("com.auctor.execution.ApplicationKt")
}

group = "com.auctor"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.0")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.auctor.execution.ApplicationKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.auctor.execution.ApplicationKt"
    }
}
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.auctor.execution.ApplicationKt"
    }
}

