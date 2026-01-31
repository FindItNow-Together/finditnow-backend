plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
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
    implementation(project(":libs:database"))
    implementation(project(":libs:jwt"))
    implementation(project(":libs:redis"))
    implementation(project(":libs:proto"))
    implementation(project(":libs:interservice-caller"))

    implementation("org.postgresql:postgresql:42.7.8")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ✅ REQUIRED for @Valid / Bean Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ✅ Swagger (Boot 3.5 compatible)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")

    // ✅ REQUIRED by springdoc at runtime
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Lombok
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // MapStruct (stable)
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Flyway
    implementation("org.flywaydb:flyway-core:11.17.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.17.0")

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.76.0")
    implementation("io.grpc:grpc-stub:1.63.0")
    implementation("io.grpc:grpc-protobuf:1.63.0")
}



