package io.github.siyual_park.application.server.gateway

import io.github.siyual_park.application.server.dto.request.GrantScopeRequest
import io.github.siyual_park.application.server.dto.response.ScopeTokenInfo
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.FluxExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.util.Optional

@Component
class ScopeControllerGateway(
    private val client: WebTestClient,
    private val gatewayAuthorization: GatewayAuthorization,
) {
    suspend fun readAll(
        id: String? = null,
        name: String? = null,
        createdAt: String? = null,
        updatedAt: String? = null,
        sort: String? = null,
        page: Int = 0,
        perPage: Int = 15,
    ): FluxExchangeResult<ScopeTokenInfo> {
        return client.get()
            .uri {
                it.path("/scope")
                    .queryParamIfPresent("id", Optional.ofNullable(id))
                    .queryParamIfPresent("name", Optional.ofNullable(name))
                    .queryParamIfPresent("created-at", Optional.ofNullable(createdAt))
                    .queryParamIfPresent("updated-at", Optional.ofNullable(updatedAt))
                    .queryParamIfPresent("sort", Optional.ofNullable(sort))
                    .queryParamIfPresent("page", Optional.ofNullable(page))
                    .queryParamIfPresent("per-page", Optional.ofNullable(perPage))
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, gatewayAuthorization.getAuthorization())
            .exchange()
            .returnResult(ScopeTokenInfo::class.java)
    }

    suspend fun read(scopeId: Long): FluxExchangeResult<ScopeTokenInfo> {
        return client.get()
            .uri("/scope/$scopeId")
            .header(HttpHeaders.AUTHORIZATION, gatewayAuthorization.getAuthorization())
            .exchange()
            .returnResult(ScopeTokenInfo::class.java)
    }

    suspend fun readChildren(scopeId: Long): FluxExchangeResult<ScopeTokenInfo> {
        return client.get()
            .uri("/scope/$scopeId/children")
            .header(HttpHeaders.AUTHORIZATION, gatewayAuthorization.getAuthorization())
            .exchange()
            .returnResult(ScopeTokenInfo::class.java)
    }

    suspend fun grantScope(scopeId: Long, request: GrantScopeRequest): FluxExchangeResult<ScopeTokenInfo> {
        return client.post()
            .uri("/scope/$scopeId/children")
            .header(HttpHeaders.AUTHORIZATION, gatewayAuthorization.getAuthorization())
            .bodyValue(request)
            .exchange()
            .returnResult(ScopeTokenInfo::class.java)
    }

    suspend fun revokeScope(scopeId: Long, childId: Long): FluxExchangeResult<Unit> {
        return client.delete()
            .uri("/scope/$scopeId/children/$childId")
            .header(HttpHeaders.AUTHORIZATION, gatewayAuthorization.getAuthorization())
            .exchange()
            .returnResult()
    }
}