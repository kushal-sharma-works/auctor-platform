plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
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
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-java:3.25.3")

    // Required for generated gRPC code
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // Security
    implementation("io.ktor:ktor-server-auth-jvm:3.0.0")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.0.0")
    implementation("com.auth0:java-jwt:4.4.0")

    // GraphQL
    implementation("com.graphql-java:graphql-java:21.5")
    implementation("com.apurebase:kgraphql:0.19.0")
    implementation("com.apurebase:kgraphql-ktor:0.19.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")
    
    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    // OpenTelemetry
    implementation("io.opentelemetry:opentelemetry-sdk:1.40.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.40.0")
    implementation("io.opentelemetry:opentelemetry-api:1.40.0")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.25.0-alpha")
    
    // Micrometer
    implementation("io.micrometer:micrometer-core:1.13.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.0")
    testImplementation("io.grpc:grpc-inprocess:1.63.0")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.auctor.execution.ApplicationKt"
    }
}

// ADD THIS BLOCK - This is the only addition
tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.63.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc")
                create("grpckt")
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
        kotlin {
            srcDir("build/generated/source/proto/main/kotlin")
            srcDir("build/generated/source/proto/main/grpckt")
        }
    }
}