package io.github.siyual_park.application.server.controller

import io.github.siyual_park.application.server.dto.GrantType
import io.github.siyual_park.application.server.dto.request.CreateTokenRequest
import io.github.siyual_park.application.server.dto.response.PrincipalInfo
import io.github.siyual_park.application.server.dto.response.TokenInfo
import io.github.siyual_park.application.server.property.TokensProperty
import io.github.siyual_park.auth.domain.Principal
import io.github.siyual_park.auth.domain.authentication.Authenticator
import io.github.siyual_park.auth.domain.authentication.AuthorizationPayload
import io.github.siyual_park.auth.domain.authorization.Authorizator
import io.github.siyual_park.auth.domain.principal_refresher.PrincipalRefresher
import io.github.siyual_park.auth.domain.scope_token.ScopeTokenStorage
import io.github.siyual_park.auth.domain.scope_token.loadOrFail
import io.github.siyual_park.auth.domain.token.Token
import io.github.siyual_park.auth.domain.token.TokenFactoryProvider
import io.github.siyual_park.auth.domain.token.TokenStorage
import io.github.siyual_park.auth.domain.token.TokenTemplate
import io.github.siyual_park.auth.exception.RequiredPermissionException
import io.github.siyual_park.client.domain.auth.ClientCredentialsGrantPayload
import io.github.siyual_park.json.bind.RequestForm
import io.github.siyual_park.mapper.MapperContext
import io.github.siyual_park.mapper.map
import io.github.siyual_park.persistence.AsyncLazy
import io.github.siyual_park.user.domain.auth.PasswordGrantPayload
import io.swagger.annotations.Api
import kotlinx.coroutines.flow.toSet
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Api(tags = ["auth"])
@RestController
@RequestMapping("")
class AuthController(
    private val authenticator: Authenticator,
    private val authorizator: Authorizator,
    tokenFactoryProvider: TokenFactoryProvider,
    private val principalRefresher: PrincipalRefresher,
    private val scopeTokenStorage: ScopeTokenStorage,
    private val tokensProperty: TokensProperty,
    private val tokenStorage: TokenStorage,
    private val mapperContext: MapperContext,
) {
    private val tokenScope = AsyncLazy {
        scopeTokenStorage.loadOrFail("token:create")
    }
    private val accessTokenScope = AsyncLazy {
        scopeTokenStorage.loadOrFail("access-token:create")
    }
    private val refreshTokenScope = AsyncLazy {
        scopeTokenStorage.loadOrFail("refresh-token:create")
    }

    private val accessTokenFactory = AsyncLazy {
        tokenFactoryProvider.get(
            TokenTemplate(
                type = "acs",
                limit = listOf(
                    "pid" to 1
                ),
                pop = setOf(accessTokenScope.get(), refreshTokenScope.get())
            )
        )
    }
    private val refreshTokenFactory = AsyncLazy {
        tokenFactoryProvider.get(
            TokenTemplate(
                type = "rfr",
                pop = setOf(refreshTokenScope.get()),
            )
        )
    }

    @PostMapping("/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createToken(@Valid @RequestForm request: CreateTokenRequest): TokenInfo {
        val principal = authenticate(request)
        val (accessToken, refreshToken) = createTokens(principal, request)

        return TokenInfo(
            accessToken = accessToken.signature,
            tokenType = "bearer",
            expiresIn = tokensProperty.accessToken.age,
            refreshToken = refreshToken?.signature
        )
    }

    @GetMapping("/principal")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasPermission(null, 'principal[self]:read')")
    suspend fun readSelf(@AuthenticationPrincipal principal: Principal): PrincipalInfo {
        return mapperContext.map(principal)
    }

    suspend fun authenticate(request: CreateTokenRequest): Principal {
        authenticator.authenticate(ClientCredentialsGrantPayload(request.clientId, request.clientSecret))
            .also {
                if (!authorizator.authorize(it, tokenScope.get())) {
                    throw RequiredPermissionException()
                }
            }

        val principal = authenticator.authenticate(
            when (request.grantType) {
                GrantType.PASSWORD -> PasswordGrantPayload(request.username!!, request.password!!, request.clientId)
                GrantType.CLIENT_CREDENTIALS -> ClientCredentialsGrantPayload(request.clientId, request.clientSecret)
                GrantType.REFRESH_TOKEN -> AuthorizationPayload("bearer", request.refreshToken!!)
            }
        )

        if (!authorizator.authorize(principal, accessTokenScope.get())) {
            throw RequiredPermissionException()
        }

        if (request.grantType == GrantType.REFRESH_TOKEN) {
            return principalRefresher.refresh(principal)
        }

        return principal
    }

    suspend fun createTokens(principal: Principal, request: CreateTokenRequest): Pair<Token, Token?> {
        val scope = request.scope?.split(" ")
            ?.let { scopeTokenStorage.load(it) }
            ?.toSet()

        val refreshToken = if (request.grantType == GrantType.REFRESH_TOKEN) {
            tokenStorage.load(request.refreshToken!!)
        } else if (authorizator.authorize(principal, refreshTokenScope.get())) {
            refreshTokenFactory.get().create(
                principal,
                tokensProperty.refreshToken.age,
                filter = scope,
            )
        } else {
            null
        }
        val accessToken = accessTokenFactory.get().create(
            principal,
            tokensProperty.accessToken.age,
            claims = refreshToken?.id?.toString()?.let { mapOf("pid" to it) },
            filter = scope,
        )

        return if (request.grantType != GrantType.REFRESH_TOKEN) {
            accessToken to refreshToken
        } else {
            accessToken to null
        }
    }
}
