plugins {
    `java-library`
}

repositories {
    mavenCentral()
}


dependencies{
    api(project(":libs:config"))
    implementation("com.sun.mail:jakarta.mail:2.0.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
