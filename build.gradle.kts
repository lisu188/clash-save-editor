plugins {
    kotlin("jvm") version "2.4.20" apply false
    kotlin("plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.compose") version "1.12.1" apply false
}
allprojects {
    group = "com.lis"
    version = "2.0.0"
}
subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxHeapSize = "1g"
        testLogging { events("failed", "skipped"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    }
}
tasks.register("test") { dependsOn(":core:test", ":mcp:test", ":desktop:test") }
tasks.register("check") { dependsOn("test") }
tasks.register("build") { dependsOn(":core:build", ":mcp:build", ":desktop:build") }
tasks.register("run") { dependsOn(":desktop:run") }
tasks.register("runMcpServer") { dependsOn(":mcp:run") }
tasks.register("fatJar") { dependsOn(":mcp:fatJar") }