plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cdimascio:java-dotenv:5.2.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

