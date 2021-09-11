plugins {
    application

    id("org.springframework.boot")
    id("io.spring.dependency-management")

    kotlin("plugin.spring")
}

dependencies {
    api(project(":dependency:async"))
    api(project(":dependency:r2dbc"))

    implementation(project(":dependency:spring"))
    testImplementation(project(":dependency:spring-test"))

    testImplementation(project(":dependency:async-test"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src/main")
kotlin.sourceSets["test"].kotlin.srcDirs("src/test")

sourceSets["main"].resources.srcDirs("src/main/resources")
sourceSets["test"].resources.srcDirs("src/test/resources")

tasks {
    bootJar {
        enabled = false
    }

    jar {
        enabled = true
    }
}