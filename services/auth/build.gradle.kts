plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Shared libraries
    implementation(project(":libs:common"))
    implementation(project(":libs:database"))
    implementation(project(":libs:redis"))
    implementation(project(":libs:jwt"))
    implementation(project(":libs:dispatcher"))

    // HTTP Server
    implementation("io.undertow:undertow-core:2.3.20.Final")

    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.20.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.20")

    // Testing
    testImplementation(libs.testng)

    // DB Migrations
    implementation("org.flywaydb:flyway-core:11.17.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.17.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.finditnow.auth.AuthApp"
}

tasks.named<Test>("test") {
    useTestNG()
}

