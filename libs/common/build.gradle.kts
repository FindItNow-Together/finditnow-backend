plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mindrot:jbcrypt:0.4")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

