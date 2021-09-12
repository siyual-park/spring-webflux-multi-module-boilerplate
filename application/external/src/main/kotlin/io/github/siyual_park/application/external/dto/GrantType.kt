package io.github.siyual_park.application.external.dto

enum class GrantType(val value: String) {
    PASSWORD("password"),
    REFRESH_TOKEN("refresh_token");
}