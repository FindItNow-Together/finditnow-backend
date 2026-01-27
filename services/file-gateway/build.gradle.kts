plugins {
   application
}

application {
    mainClass = "com.finditnow.filegateway.FileGatewayApp"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":libs:redis"))
    implementation(project(":libs:config"))
    // HTTP Server
    implementation("io.undertow:undertow-core:2.3.20.Final")
}

