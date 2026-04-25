plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.20.1")

    api(project(":libs:config"))
    api(project(":libs:redis"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

