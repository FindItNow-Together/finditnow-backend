plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":libs:config"))
    
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:7.0.2")
    
    implementation("org.slf4j:slf4j-api:2.0.17")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

