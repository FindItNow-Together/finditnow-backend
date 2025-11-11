plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":libs:config"))
    
    implementation("redis.clients:jedis:7.0.0")
    
    implementation("org.slf4j:slf4j-api:2.0.17")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

