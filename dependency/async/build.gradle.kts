val coroutines_version: String by project
val projectreactor_version: String by project

plugins {
    application
}

dependencies {
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:$projectreactor_version")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutines_version")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutines_version")
}