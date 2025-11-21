plugins {
    `java-library`
}

group = "com.finditnow.dispatcher"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    api(project(":libs:mail"))
    api(project(":libs:quartz-scheduler"))
    implementation("org.quartz-scheduler:quartz:2.5.1")
}

tasks.test {
    useJUnitPlatform()
}