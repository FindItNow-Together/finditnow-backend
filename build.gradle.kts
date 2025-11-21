tasks.register("createService") {
    notCompatibleWithConfigurationCache("This task generates project files dynamically")

    doLast {
        val moduleName = project.property("serviceName")?.toString()
            ?: throw GradleException("Pass -PserviceName=<moduleName>")

        val dir = file("services/$moduleName")

        if (dir.exists()) {
            throw GradleException("Service already exists: $moduleName")
        }

        file("$dir/src/main/java").mkdirs()
        file("$dir/src/main/resources").mkdirs()
        file("$dir/src/test/java").mkdirs()

        file("$dir/build.gradle.kts").writeText(
            """
            plugins {
                java
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            dependencies {
            }
            """.trimIndent()
        )

        file("settings.gradle.kts").appendText(
            "\ninclude(\"services:$moduleName\")"
        )
    }
}
