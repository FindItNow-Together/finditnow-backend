import com.google.protobuf.gradle.id

plugins {
    `java-library`
    id("java")
    id("com.google.protobuf") version "0.9.5"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}



dependencies {
    api("io.grpc:grpc-stub:1.63.0")
    api("io.grpc:grpc-protobuf:1.63.0")
    api("com.google.protobuf:protobuf-java:4.33.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // optional for testing
    api("io.grpc:grpc-netty-shaded:1.63.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.63.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
        }
    }
}

sourceSets {
    named("main") {
        java {
            srcDir("$buildDir/generated/source/proto/main/java")
            srcDir("$buildDir/generated/source/proto/main/grpc")
        }
    }
}

