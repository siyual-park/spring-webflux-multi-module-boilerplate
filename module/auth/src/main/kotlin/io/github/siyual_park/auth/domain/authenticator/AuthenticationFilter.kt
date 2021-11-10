package io.github.siyual_park.auth.domain.authenticator

class AuthenticationFilter(
    private val type: String
) : AuthenticateFilter {
    override fun isSubscribe(payload: Any): Boolean {
        if (payload !is AuthorizationPayload) {
            return false
        }
        return payload.type == type
    }
}
