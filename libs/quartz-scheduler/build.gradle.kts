plugins {
    `java-library`
}

group = "org.finditnow.scheduler"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    api(project(":libs:mail"))
    implementation("org.quartz-scheduler:quartz:2.5.1")
}

tasks.test {
    useJUnitPlatform()
}