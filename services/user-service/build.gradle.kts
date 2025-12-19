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
    implementation(project(":libs:proto"))//grpc

    implementation("org.postgresql:postgresql:42.7.8")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // https://mvnrepository.com/artifact/org.projectlombok/lombok
    implementation("org.projectlombok:lombok:1.18.42")

    // DB Migrations
    implementation("org.flywaydb:flyway-core:11.17.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.17.0")

    //grpc
    implementation("io.grpc:grpc-netty-shaded:1.76.0")
    implementation("io.grpc:grpc-stub:1.63.0")
    implementation("io.grpc:grpc-protobuf:1.63.0")
}