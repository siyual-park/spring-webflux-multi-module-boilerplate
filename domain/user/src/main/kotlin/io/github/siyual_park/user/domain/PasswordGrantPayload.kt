package io.github.siyual_park.user.domain

import io.github.siyual_park.auth.domain.authenticator.AuthenticationPayload

data class PasswordGrantPayload(
    val username: String,
    val password: String,
) : AuthenticationPayload
