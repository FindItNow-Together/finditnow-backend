plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":libs:config"))

    implementation("org.postgresql:postgresql:42.7.8")
    implementation("com.zaxxer:HikariCP:7.0.2")
    
    implementation("org.slf4j:slf4j-api:2.0.17")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

