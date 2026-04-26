plugins {
   application
    java
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

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.20")
}

//tasks.named<Jar>("jar") {
//    dependsOn(
//        ":libs:redis:jar",
//        ":libs:config:jar"
//    )
//}
//
//tasks.jar {
//    dependsOn(
//        ":libs:redis:jar",
//        ":libs:config:jar"
//    )
//
//    manifest {
//        attributes["Main-Class"] = application.mainClass
//    }
//
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//
//    from({
//        configurations.runtimeClasspath.get().map {
//            if (it.isDirectory) it else zipTree(it)
//        }
//    })
//}

