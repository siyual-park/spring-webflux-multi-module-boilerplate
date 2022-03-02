package io.github.siyual_park.auth.domain.token

import io.github.siyual_park.auth.domain.Principal
import io.github.siyual_park.auth.domain.authorization.Authorizator
import io.github.siyual_park.auth.domain.scope_token.ScopeTokenFinder
import io.github.siyual_park.auth.entity.ScopeToken
import io.github.siyual_park.auth.exception.RequiredPermissionException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TokenIssuer(
    private val authorizator: Authorizator,
    private val scopeTokenFinder: ScopeTokenFinder,
    private val tokenEncoder: TokenEncoder,
    private val claimEmbedder: ClaimEmbedder,
    @Value("#{T(java.time.Duration).ofSeconds(\${application.auth.access-token.age})}")
    private val accessTokenAge: Duration = Duration.ofHours(1),
    @Value("#{T(java.time.Duration).ofSeconds(\${application.auth.refresh-token.age})}")
    private val refreshTokenAge: Duration = Duration.ofDays(30)
) {
    suspend fun issue(principal: Principal, scope: Set<ScopeToken>? = null): TokenContainer {
        val accessTokenCreateScope = scopeTokenFinder.findByNameOrFail("access-token:create")
        val refreshTokenCreateScope = scopeTokenFinder.findByNameOrFail("refresh-token:create")

        if (!authorizator.authorize(principal, accessTokenCreateScope)) {
            throw RequiredPermissionException()
        }

        val claim = claimEmbedder.embedding(principal)

        val accessTokenScope =
            principal.scope.filter { it.id != accessTokenCreateScope.id && it.id != refreshTokenCreateScope.id && scope?.contains(it) ?: true }
        val accessToken = tokenEncoder.encode(
            claim,
            accessTokenAge,
            scope = accessTokenScope
        )

        val refreshToken = if (authorizator.authorize(principal, refreshTokenCreateScope)) {
            val refreshTokenScope = principal.scope.filter { it.id != refreshTokenCreateScope.id && scope?.contains(it) ?: true }
            tokenEncoder.encode(
                claim,
                refreshTokenAge,
                scope = refreshTokenScope
            )
        } else null

        return TokenContainer(
            accessToken = Token(
                value = accessToken,
                type = "bearer",
                expiresIn = accessTokenAge,
            ),
            refreshToken = refreshToken?.let {
                Token(
                    value = it,
                    type = "bearer",
                    expiresIn = refreshTokenAge,
                )
            }
        )
    }
}
