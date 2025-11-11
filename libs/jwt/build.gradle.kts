plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":libs:config"))
    api(project(":libs:redis"))
    
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

