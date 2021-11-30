package io.github.siyual_park.auth.domain.authorization

import io.github.siyual_park.auth.domain.Principal
import io.github.siyual_park.auth.domain.scope_token.ScopeTokenFinder
import io.github.siyual_park.auth.entity.ScopeToken
import org.springframework.stereotype.Component

@Suppress("UNCHECKED_CAST")
@Component
class Authorizator(
    private val scopeTokenFinder: ScopeTokenFinder,
) {
    private val authorizators = mutableListOf<Pair<AuthorizeFilter, AuthorizeProcessor<*>>>()

    fun register(filter: AuthorizeFilter, processor: AuthorizeProcessor<*>): Authorizator {
        authorizators.add(filter to processor)
        return this
    }

    suspend fun <PRINCIPAL : Principal> authorize(
        principal: PRINCIPAL,
        scope: List<ScopeToken>,
        targetDomainObjects: List<Any?>? = null
    ): Boolean {
        if (targetDomainObjects != null && scope.size != targetDomainObjects.size) {
            return false
        }

        for (i in scope.indices) {
            if (!authorize(principal, scope[i], targetDomainObjects?.get(i))) {
                return false
            }
        }

        return true
    }

    suspend fun <PRINCIPAL : Principal> authorize(
        principal: PRINCIPAL,
        scopeToken: String,
        targetDomainObject: Any? = null
    ): Boolean {
        return scopeTokenFinder.findByName(scopeToken)?.let {
            authorize(principal, it, targetDomainObject)
        } ?: return false
    }

    suspend fun <PRINCIPAL : Principal> authorize(
        principal: PRINCIPAL,
        scopeToken: ScopeToken,
        targetDomainObject: Any? = null
    ): Boolean {
        return authorizators.filter { (filter, _) -> filter.isSubscribe(principal, scopeToken) }
            .map { (_, evaluator) -> evaluator as? AuthorizeProcessor<PRINCIPAL> }
            .filterNotNull()
            .all { it.authorize(principal, scopeToken, targetDomainObject) }
    }
}
