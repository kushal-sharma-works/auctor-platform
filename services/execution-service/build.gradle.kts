plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("com.google.protobuf") version "0.9.4"
}

group = "com.auctor"
version = "0.0.1-SNAPSHOT"

application {
    mainClass.set("com.auctor.execution.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // gRPC client
    implementation("io.grpc:grpc-netty-shaded:1.63.0")
    implementation("io.grpc:grpc-protobuf:1.63.0")
    implementation("io.grpc:grpc-stub:1.63.0")
    implementation("com.google.protobuf:protobuf-java:3.25.3")

    // Required for generated gRPC code
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation(kotlin("test"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.auctor.execution.ApplicationKt"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.63.0"
        }
    }

    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
            srcDir("build/generated/source/proto/main/grpc")
        }
    }
}
