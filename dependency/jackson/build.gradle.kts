val jackson_version: String by project

plugins {
    application
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core:$jackson_version")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
}